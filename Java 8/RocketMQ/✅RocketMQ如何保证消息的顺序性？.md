# 典型回答

和Kafka只支持同一个Partition内消息的顺序性一样，RocketMQ中也提供了基于队列(分区)的顺序消费。即同一个队列内的消息可以做到有序，但是不同队列内的消息是无序的！

当我们作为MQ的生产者需要发送顺序消息时，**需要在send方法中，传入一个MessageQueueSelector。**

MessageQueueSelector中需要实现一个select方法，这个方法就是用来定义要把消息发送到哪个MessageQueue的，通常可以使用取模法进行路由：

```latex
   SendResult sendResult = producer.send(msg, new MessageQueueSelector() {
    @Override
  	//mqs：该Topic下所有可选的MessageQueue
    //msg：待发送的消息
    //arg：发送消息时传递的参数
    public MessageQueue select(List<MessageQueue> mqs, Message msg, Object arg) {
     Integer id = (Integer) arg;
     //根据参数，计算出一个要接收消息的MessageQueue的下标
     int index = id % mqs.size();
     //返回这个MessageQueue
     return mqs.get(index);
    }
   }, orderId);
```

**通过以上形式就可以将需要有序的消息发送到同一个队列中**。需要注意的时候，这里需要使用同步发送的方式！

消息按照顺序发送的消息队列中之后，那么，消费者如何按照发送顺序进行消费呢？

RocketMQ的MessageListener回调函数提供了两种消费模式，有序消费模式MessageListenerOrderly和并发消费模式MessageListenerConcurrently。所以，**想要实现顺序消费，需要使用MessageListenerOrderly模式接收消息**：

```latex
consumer.registerMessageListener(new MessageListenerOrderly() {
        Override
        public ConsumeOrderlyStatus consumeMessage(List<MessageExt> msgs ,ConsumeOrderlyContext context) {
             System.out.printf("Receive order msg:" + new String(msgs.get(0).getBody()));
             return ConsumeOrderlyStatus.SUCCESS ; 
        }
});
```

当我们用以上方式注册一个消费之后，为了保证同一个队列中的有序消息可以被顺序消费，就要保证RocketMQ的Broker只会把消息发送到同一个消费者上，这时候就需要**加锁**了。

在实现中，ConsumeMessageOrderlyService 初始化的时候，会启动一个定时任务，**会尝试向 Broker 为当前消费者客户端申请分布式锁**。如果获取成功，那么后续消息将会只发给这个Consumer。

接下来在消息拉取的过程中，消费者会一次性拉取多条消息的，并且会将拉取到的消息放入 ProcessQueue，同时将消息提交到消费线程池进行执行。

那么拉取之后的消费过程，怎么保证顺序消费呢？这里就需要更多的锁了。

RocketMQ在消费的过程中，需要**申请 MessageQueue 锁**，确保在同一时间，一个队列中只有一个线程能处理队列中的消息。

获取到 MessageQueue 的锁后，就可以从ProcessQueue中依次拉取一批消息处理了，但是这个过程中，为了保证消息不会出现重复消费，还需要**对ProcessQueue进行加锁**。（这个在扩展知识中展开）

然后就可以开始处理业务逻辑了。

**总结下来就是三次加锁，先锁定Broker上的MessageQueue，确保消息只会投递到唯一的消费者，对本地的MessageQueue加锁，确保只有一个线程能处理这个消息队列。对存储消息的ProcessQueue加锁，确保在重平衡的过程中不会出现消息的重复消费。**

（完整的处理流程大家可以看一下这张图，是极客时间上某个专栏中的内容，虽然专栏中这段文字描述不太容易懂，但是这个图画的还是挺清晰的。）

![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1679051138277-fa95bc6f-aacb-4897-9ec3-e08995702699.png#averageHue=%23f9f9f8&clientId=uacdd369f-2f0b-4&from=paste&height=664&id=u6ab16e67&originHeight=1328&originWidth=1920&originalType=binary&ratio=2&rotation=0&showTitle=false&size=472357&status=done&style=none&taskId=uf0b6f03b-1de4-4a65-a48b-96fcbee6470&title=&width=960)


# 扩展知识

## 第三把锁有什么用？

前面介绍客户端加锁过程中，一共加了三把锁，那么，有没有想过这样一个问题，第三把锁如果不加的话，是不是也没问题？

因为我们已经对MessageQueue加锁了，为啥还需要对ProcessQueue再次加锁呢？

这里其实主要考虑的是**重平衡的问题**。

当我们的消费者集群，新增了一些消费者，发生重平衡的时候，某个队列可能会原来属于客户端A消费的，但是现在要重新分配给客户端B了。

这时候客户端A就需要把自己加在Broker上的锁解掉，而在这个解锁的过程中，就需要确保消息不能在消费过程中就被移除了，因为如果客户端A可能正在处理一部分消息，但是位点信息还没有提交，如果客户端B立马去消费队列中的消息，那存在一部分数据会被重复消费。

那么如何判断消息是否正在消费中呢，**就需要通过这个ProcessQueue上面的锁来判断了，也就是说在解锁的线程也需要尝试对ProcessQueue进行加锁，加锁成功才能进行解锁操作。以避免过程中有消息消费。**

## 顺序消费存在的问题

通过上面的介绍，我们知道了RocketMQ的顺序消费是通过在消费者上多次加锁实现的，这种方式带来的问题就是会降低吞吐量，并且如果前面的消息阻塞，会导致更多消息阻塞。所以，顺序消息需要慎用。


## <br /><br />
