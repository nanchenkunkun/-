# 典型回答

MQ的消费模式可以大致分为两种，一种是推Push，一种是拉Pull

Push是服务端主动推送消息给客户端，Pull是客户端需要主动到服务端轮询获取数据。

他们各自有各自的优缺点，推优点是及时性较好，但如果客户端没有做好流控，一旦服务端推送大量消息到客户端时，就会导致客户端消息堆积甚至崩溃。

拉优点是客户端可以依据自己的消费能力进行消费，但是频繁拉取会给服务端造成压力，并且可能会导致消息消费不及时。

**RocketMQ既提供了Push模式也提供了Pull模式**，开发者可以自行选择，主要有两个Consumer可以供开发者选择：

```sql
public class DefaultMQPullConsumer extends ClientConfig implements MQPullConsumer {

// https://github.com/apache/rocketmq/blob/develop/client/src/main/java/org/apache/rocketmq/client/consumer/DefaultMQPullConsumer.java
}

public class DefaultMQPushConsumer extends ClientConfig implements MQPushConsumer {

//https://github.com/apache/rocketmq/blob/develop/client/src/main/java/org/apache/rocketmq/client/consumer/DefaultMQPushConsumer.java
}
```

其中DefaultMQPullConsumer已经不建议使用了，建议使用DefaultLitePullConsumer。Lite Pull Consumer是RocketMQ 4.6.0推出的Pull Consumer，相比于原始的Pull Consumer更加简单易用，它提供了Subscribe和Assign两种模式。

> /**
>  * @deprecated Default pulling consumer. This class will be removed in 2022, and a better implementation {@link
>  * DefaultLitePullConsumer} is recommend to use in the scenario of actively pulling messages.
>  */


但是，我们需要注意的是，**RocketMQ的push模式其实底层的实现还是基于pull实现的，**只不过他把pull给封装的比较好，让你以为是在push。

在下面这篇文章中我们介绍过长轮询，其实RocketMQ的push就是通过长轮询来实现的。

[✅消息队列使用拉模式好还是推模式好？为什么？](https://www.yuque.com/hollis666/fo22bm/mq3pwg8ge56hfvhx?view=doc_embed)

以下是关于RocketMQ中实现长轮询的代码（基于5.1.4），关键入口PullMessageProcessor的processRequest方法的部分代码：

```sql
if (this.brokerController.getMessageStore() instanceof DefaultMessageStore) {
    DefaultMessageStore defaultMessageStore = (DefaultMessageStore)this.brokerController.getMessageStore();
    boolean cgNeedColdDataFlowCtr = brokerController.getColdDataCgCtrService().isCgNeedColdDataFlowCtr(requestHeader.getConsumerGroup());
    if (cgNeedColdDataFlowCtr) {
        boolean isMsgLogicCold = defaultMessageStore.getCommitLog()
            .getColdDataCheckService().isMsgInColdArea(requestHeader.getConsumerGroup(),
                requestHeader.getTopic(), requestHeader.getQueueId(), requestHeader.getQueueOffset());
        if (isMsgLogicCold) {
            ConsumeType consumeType = this.brokerController.getConsumerManager().getConsumerGroupInfo(requestHeader.getConsumerGroup()).getConsumeType();
            if (consumeType == ConsumeType.CONSUME_PASSIVELY) {
                response.setCode(ResponseCode.SYSTEM_BUSY);
                response.setRemark("This consumer group is reading cold data. It has been flow control");
                return response;
            } else if (consumeType == ConsumeType.CONSUME_ACTIVELY) {
                if (brokerAllowFlowCtrSuspend) {  // second arrived, which will not be held
                    PullRequest pullRequest = new PullRequest(request, channel, 1000,
                        this.brokerController.getMessageStore().now(), requestHeader.getQueueOffset(), subscriptionData, messageFilter);
                    this.brokerController.getColdDataPullRequestHoldService().suspendColdDataReadRequest(pullRequest);
                    return null;
                }
                requestHeader.setMaxMsgNums(1);
            }
        }
    }
}
```

其中这部分代码，就是通过创建一个轮询任务。

```sql
PullRequest pullRequest = new PullRequest(request, channel, 1000,
                        this.brokerController.getMessageStore().now(), requestHeader.getQueueOffset(), subscriptionData, messageFilter);
this.brokerController.getColdDataPullRequestHoldService().suspendColdDataReadRequest(pullRequest);
```

[ColdDataPullRequestHoldService](https://github.com/apache/rocketmq/blob/develop/broker/src/main/java/org/apache/rocketmq/broker/coldctr/ColdDataPullRequestHoldService.java) （[PullRequestHoldService](https://github.com/apache/rocketmq/blob/develop/broker/src/main/java/org/apache/rocketmq/broker/longpolling/PullRequestHoldService.java)）是一个子线程，他的run方法如下：

```sql
@Override
public void run() {
    // 记录服务启动信息
    log.info("{} service started", this.getServiceName());

    // 在服务未停止的情况下循环执行以下逻辑
    while (!this.isStopped()) {
        try {
            // 根据配置决定等待的时长，控制数据流量
            if (!this.brokerController.getMessageStoreConfig().isColdDataFlowControlEnable()) {
                this.waitForRunning(20 * 1000); // 不启用冷数据流量控制时等待 20 秒
            } else {
                this.waitForRunning(5 * 1000);  // 启用冷数据流量控制时等待 5 秒
            }

            // 记录当前时间戳以计算处理时间
            long beginClockTimestamp = this.systemClock.now();

            // 执行检查数据并拉取的逻辑
            this.checkColdDataPullRequest();

            // 计算处理所花费的时间
            long costTime = this.systemClock.now() - beginClockTimestamp;

            // 记录处理耗时，并根据情况标记为 "NOTIFYME" 或 "OK"
            log.info("[{}] checkColdDataPullRequest-cost {} ms.", costTime > 5 * 1000 ? "NOTIFYME" : "OK", costTime);

        } catch (Throwable e) {
            // 记录异常信息，但不中断循环
            log.warn(this.getServiceName() + " service has exception", e);
        }
    }

    // 记录服务结束信息
    log.info("{} service end", this.getServiceName());
}

```

就是说，每隔一段时间（5秒或者20秒），执行一次数据拉取`checkColdDataPullRequest`，看下这个方法的具体实现：
```sql
/**
 * 检查数据并拉取
 */
private void checkColdDataPullRequest() {
    int succTotal = 0, errorTotal = 0;
    int queueSize = pullRequestColdHoldQueue.size();

    // 使用迭代器遍历冷数据拉取请求队列
    Iterator<PullRequest> iterator = pullRequestColdHoldQueue.iterator();
    while (iterator.hasNext()) {
        PullRequest pullRequest = iterator.next();

        // 判断是否超过了冷数据拉取的超时时间
        if (System.currentTimeMillis() >= pullRequest.getSuspendTimestamp() + coldHoldTimeoutMillis) {
            try {
                // 向请求中添加标记表明不需要挂起
                pullRequest.getRequestCommand().addExtField(NO_SUSPEND_KEY, "1");

                // 使用消息处理器执行请求，唤醒客户端进行消息拉取
                this.brokerController.getPullMessageProcessor().executeRequestWhenWakeup(
                    pullRequest.getClientChannel(), pullRequest.getRequestCommand());
                succTotal++;
            } catch (Exception e) {
                // 记录异常信息
                log.error("PullRequestColdHoldService checkColdDataPullRequest error", e);
                errorTotal++;
            }

            // 从迭代器中移除已处理的请求
            iterator.remove();
        }
    }

    // 记录处理结果的日志信息
    log.info("checkColdPullRequest-info-finish, queueSize: {} successTotal: {} errorTotal: {}",
        queueSize, succTotal, errorTotal);
}

```

# 扩展知识

## 用法

以下实例来自RocketMQ官网：[https://rocketmq.apache.org/](https://rocketmq.apache.org/)
### Push模式

```sql
public class Consumer {
  public static void main(String[] args) throws InterruptedException, MQClientException {
    // 初始化consumer，并设置consumer group name
    DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("please_rename_unique_group_name");
   
    // 设置NameServer地址 
    consumer.setNamesrvAddr("localhost:9876");
    //订阅一个或多个topic，并指定tag过滤条件，这里指定*表示接收所有tag的消息
    consumer.subscribe("TopicTest", "*");
    //注册回调接口来处理从Broker中收到的消息
    consumer.registerMessageListener(new MessageListenerConcurrently() {
      @Override
      public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
        System.out.printf("%s Receive New Messages: %s %n", Thread.currentThread().getName(), msgs);
        // 返回消息消费状态，ConsumeConcurrentlyStatus.CONSUME_SUCCESS为消费成功
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
      }
    });
    // 启动Consumer
    consumer.start();
    System.out.printf("Consumer Started.%n");
  }
}
```

### Pull模式

```sql
public class PullConsumerTest {
  public static void main(String[] args) throws MQClientException {
    DefaultMQPullConsumer consumer = new DefaultMQPullConsumer("please_rename_unique_group_name_5");
    consumer.setNamesrvAddr("127.0.0.1:9876");
    consumer.start();
    try {
      MessageQueue mq = new MessageQueue();
      mq.setQueueId(0);
      mq.setTopic("TopicTest");
      mq.setBrokerName("jinrongtong-MacBook-Pro.local");
      long offset = 26;
      PullResult pullResult = consumer.pull(mq, "*", offset, 32);
      if (pullResult.getPullStatus().equals(PullStatus.FOUND)) {
        System.out.printf("%s%n", pullResult.getMsgFoundList());
        consumer.updateConsumeOffset(mq, pullResult.getNextBeginOffset());
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    consumer.shutdown();
  }
}
```


```sql
public class LitePullConsumerSubscribe {
    public static volatile boolean running = true;
    public static void main(String[] args) throws Exception {
        DefaultLitePullConsumer litePullConsumer = new DefaultLitePullConsumer("lite_pull_consumer_test");
        litePullConsumer.subscribe("TopicTest", "*");
        litePullConsumer.setPullBatchSize(20);
        litePullConsumer.start();
        try {
            while (running) {
                List<MessageExt> messageExts = litePullConsumer.poll();
                System.out.printf("%s%n", messageExts);
            }
        } finally {
            litePullConsumer.shutdown();
        }
    }
}
```
