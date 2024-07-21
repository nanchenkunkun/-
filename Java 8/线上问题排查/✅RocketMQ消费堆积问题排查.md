## 问题现象
负责的业务中有一个应用因为特殊原因，需要修改消息配置（将Spring Cloud Stream 改为 RocketMQ native），修改前和修改后的配置项如下：
```properties
spring.cloud.stream.bindings.consumerA.group=CID_CONSUMER_A
spring.cloud.stream.bindings.consumerA.contentType=text/plain
spring.cloud.stream.bindings.consumerA.destination=CONSUMER_A_TOPIC
spring.cloud.stream.rocketmq.bindings.consumerA.consumer.tags=CONSUMER_A_TOPIC_TAG

spring.cloud.stream.bindings.consumerB.group=CID_CONSUMER_A
spring.cloud.stream.bindings.consumerB.contentType=text/plain
spring.cloud.stream.bindings.consumerB.destination=CONSUMER_B_TOPIC
spring.cloud.stream.rocketmq.bindings.consumerB.consumer.tags=CONSUMER_B_TOPIC_TAG

spring.cloud.stream.bindings.consumerC.group=CID_CONSUMER_A
spring.cloud.stream.bindings.consumerC.contentType=text/plain
spring.cloud.stream.bindings.consumerC.destination=CONSUMER_C_TOPIC
spring.cloud.stream.rocketmq.bindings.consumerC.consumer.tags=CONSUMER_C_TOPIC_TAG
```
```properties
spring.rocketmq.consumers[0].consumer-group=CID_CONSUMER_A
spring.rocketmq.consumers[0].topic=CONSUMER_A_TOPIC
spring.rocketmq.consumers[0].sub-expression=CONSUMER_A_TOPIC_TAG
spring.rocketmq.consumers[0].message-listener-ref=consumerAListener

spring.cloud.stream.bindings.consumerB.group=CID_CONSUMER_A
spring.cloud.stream.bindings.consumerB.contentType=text/plain
spring.cloud.stream.bindings.consumerB.destination=CONSUMER_B_TOPIC
spring.cloud.stream.rocketmq.bindings.consumerB.consumer.tags=CONSUMER_B_TOPIC_TAG

spring.cloud.stream.bindings.consumerC.group=CID_CONSUMER_A
spring.cloud.stream.bindings.consumerC.contentType=text/plain
spring.cloud.stream.bindings.consumerC.destination=CONSUMER_C_TOPIC
spring.cloud.stream.rocketmq.bindings.consumerC.consumer.tags=CONSUMER_C_TOPIC_TAG
```
但是当机器发布一半后开始灰度观察的时候，出现了消息堆积问题：<br />![image.png](https://cdn.nlark.com/yuque/0/2023/png/719664/1682427421166-51c052e1-7992-4ed3-b71e-21d9bdd0709a.png#averageHue=%23f0f1ef&clientId=u77d3dd1b-a691-4&from=paste&height=247&id=ua9c360cd&originHeight=494&originWidth=836&originalType=binary&ratio=2&rotation=0&showTitle=false&size=128848&status=done&style=none&taskId=u5374b2fd-40da-4248-aa2f-fcab3f2dede&title=&width=418)
## 问题原因
### 消息订阅关系不一致
经过历史经验和踩坑，感觉有可能是订阅组机器订阅关系不一致导致的消息堆积问题（因为订阅组的机器有的订阅关系是A，有的是B，MQ不能确定是否要消费，就能只能先堆积到broker中），查看MQ控制台后发现，确实是消息订阅关系不一致，导致消息堆积<br />![image.png](https://cdn.nlark.com/yuque/0/2023/png/719664/1682427825233-7e043a14-1dac-46b2-9c97-b34d090890e7.png#averageHue=%23f7f7f7&clientId=u77d3dd1b-a691-4&from=paste&height=322&id=ubae4d40c&originHeight=644&originWidth=1112&originalType=binary&ratio=2&rotation=0&showTitle=false&size=311250&status=done&style=none&taskId=u71fb135a-eba9-4cc3-b611-327dc969628&title=&width=556)<br />已经发布的那台订阅如下：<br />![image.png](https://cdn.nlark.com/yuque/0/2023/png/719664/1682428293109-62907d55-e0c5-469c-abbc-08f3e6cf3b38.png#averageHue=%23f8f8f8&clientId=u77d3dd1b-a691-4&from=paste&height=249&id=u1fcfd19e&originHeight=498&originWidth=2634&originalType=binary&ratio=2&rotation=0&showTitle=false&size=411298&status=done&style=none&taskId=u8aa51e81-d8ae-4260-983b-b1739059924&title=&width=1317)<br />未发布的订阅关系如下（明显多于已经发布的机器的订阅关系）<br />![image.png](https://cdn.nlark.com/yuque/0/2023/png/719664/1682428655336-a8380f7b-a0ee-4319-8de1-32d9c41144b2.png#averageHue=%23fafafa&clientId=u77d3dd1b-a691-4&from=paste&height=608&id=uebff3698&originHeight=1216&originWidth=2344&originalType=binary&ratio=2&rotation=0&showTitle=false&size=799554&status=done&style=none&taskId=ub497a6a3-f521-420b-adbb-2e6030497bb&title=&width=1172)
### Spring Cloud Stream 和 RocketMQ Native
所以就引申出了一个问题，为什么将Spring Cloud Stream修改为原生的MetaQ之后，同一个ConsumerId对应的订阅关系就会改变呢？<br />更简单来说，就是为什么当RocketMQ和Spring Cloud Stream 使用相同的ConsumerId之后，RocketMQ的订阅关系会把Spring Cloud Stream的订阅关系给冲掉呢？
> 注意，一个consumerId是可以订阅多个topic的

这个时候就只能翻Spring Cloud Stream 和 RocketMQ 的启动源码来解答疑惑。
#### RocketMQ
RocketMQ client的类图如下：<br />![image.png](https://cdn.nlark.com/yuque/0/2023/png/719664/1682425441600-dea2f79c-1e75-497a-b208-f6bf09973596.png#averageHue=%23f0f0f0&clientId=u77d3dd1b-a691-4&from=drop&id=pQzt6&originHeight=543&originWidth=1259&originalType=binary&ratio=2&rotation=0&showTitle=false&size=209287&status=done&style=none&taskId=u068121ea-f37b-4590-abd5-0b9b391a226&title=)

- MQConsumerInner：记录当前consumerGroup和服务端的交互方式，以及topic和tag的映射关系。默认的实现是DefaultMQPushConsumerImpl，和consumerGroup的对应关系是1 : 1
- MQClientInstance：统一管理网络链接等可以复用的对象，通过Map维护了ConsumerGroupId和MQConsumerInner的映射关系。简单来说，就是一个ConsumerGroup，只能对应一个MQConsumerInner，如下代码所示：

![image.png](https://cdn.nlark.com/yuque/0/2023/png/719664/1682425442602-e5ee0fff-4b44-4ae0-aeae-8f563baf3942.png#averageHue=%232d2c2b&clientId=u77d3dd1b-a691-4&from=drop&id=qnLCg&originHeight=596&originWidth=1414&originalType=binary&ratio=2&rotation=0&showTitle=false&size=91537&status=done&style=none&taskId=u9e72ce5d-31b3-4826-842f-83f5de9e936&title=)
#### Spring Cloud Stream
![image.png](https://cdn.nlark.com/yuque/0/2023/png/719664/1682425443533-40b769ce-8a26-481d-a42e-26a2949fd9fe.png#averageHue=%23ebebeb&clientId=u77d3dd1b-a691-4&from=paste&height=430&id=cSi5v&originHeight=310&originWidth=373&originalType=binary&ratio=2&rotation=0&showTitle=false&size=19310&status=done&style=none&taskId=u59873022-926b-4368-9e87-066b005bcfe&title=&width=517.5)<br />Spring Cloud Stream是连接Spring和中间件的一个胶水层，在Spring Cloud Stream启动的时候，也会注册一个ConsumerGourp，如下代码所示：<br />![image.png](https://cdn.nlark.com/yuque/0/2023/png/719664/1682425444585-ca1490b9-08c7-4654-98a3-726304f6e095.png#averageHue=%232c2d2b&clientId=u77d3dd1b-a691-4&from=paste&height=486&id=RqZmn&originHeight=972&originWidth=2482&originalType=binary&ratio=2&rotation=0&showTitle=false&size=582238&status=done&style=none&taskId=u8d8b9c2f-40e4-4f52-93d8-4b57e44d133&title=&width=1241)
### 问题根因
分析到这里，原因就已经很明显了。Spring Cloud Stream会在启动的时候自己new一个MetaPushConsumer（事实上就是一个新的MQConsumerInner），所以对于一个ConsumerGroup来说，就存在了两个MQConsumerInner，这显然是不符合RocketMQ要求的1:1的映射关系的，所以RocketMQ默认会用新的映射代替老的映射关系。显然，Spring Cloud Stream的被RocketMQ原生的给替代掉了。<br />这也就是为什么已经发布的机器中，对于ConsumerA来说，只剩下RocketMQ原生的那组订阅关系了
## 解决思路
修改consumerId
```properties
spring.rocketmq.consumers[0].consumer-group=CID_CONSUMER_A
spring.rocketmq.consumers[0].topic=CONSUMER_A_TOPIC
spring.rocketmq.consumers[0].sub-expression=CONSUMER_A_TOPIC_TAG
spring.rocketmq.consumers[0].message-listener-ref=consumerAListener

spring.cloud.stream.bindings.consumerB.group=CID_CONSUMER_B
spring.cloud.stream.bindings.consumerB.contentType=text/plain
spring.cloud.stream.bindings.consumerB.destination=CONSUMER_B_TOPIC
spring.cloud.stream.rocketmq.bindings.consumerB.consumer.tags=CONSUMER_B_TOPIC_TAG

spring.cloud.stream.bindings.consumerC.group=CID_CONSUMER_B
spring.cloud.stream.bindings.consumerC.contentType=text/plain
spring.cloud.stream.bindings.consumerC.destination=CONSUMER_C_TOPIC
spring.cloud.stream.rocketmq.bindings.consumerC.consumer.tags=CONSUMER_C_TOPIC_TAG
```
## 思考和总结

1. 问题原因并不复杂，但是很多人可能分析到第一层（订阅关系不一致导致消费堆积）就不会再往下分析了，但是我们还需要有更深入的探索精神的
2. 生产环境中尽量不要搞两套配置项，会额外增加理解成本。。。。
## 小技巧
### 中间件代码如何确定版本
arthas中的sc 命令 <br />![image.png](https://cdn.nlark.com/yuque/0/2023/png/719664/1682425445317-52934b47-4f0c-4ba9-9787-adf6713e075b.png#averageHue=%230c0808&clientId=u77d3dd1b-a691-4&from=paste&height=420&id=ud2fe17b8&originHeight=840&originWidth=2576&originalType=binary&ratio=2&rotation=0&showTitle=false&size=522962&status=done&style=none&taskId=u6b68f502-b534-4107-9b3e-66ef69890e4&title=&width=1288)
### Idea如何debug具体版本的中间件
![image.png](https://cdn.nlark.com/yuque/0/2023/png/719664/1682425445693-f30c71da-02ab-4d5b-bf80-6ecb10d1a66d.png#averageHue=%233e4246&clientId=u77d3dd1b-a691-4&from=paste&height=703&id=uc86e600a&originHeight=1406&originWidth=1798&originalType=binary&ratio=2&rotation=0&showTitle=false&size=749461&status=done&style=none&taskId=u2af3e1a9-3bef-44ba-9b90-8a55fe15703&title=&width=899)

![image.png](https://cdn.nlark.com/yuque/0/2023/png/719664/1682425446186-55f31009-2326-4304-903e-36954e97eaec.png#averageHue=%23373b3b&clientId=u77d3dd1b-a691-4&from=paste&height=832&id=u1364c436&originHeight=1664&originWidth=2826&originalType=binary&ratio=2&rotation=0&showTitle=false&size=1616670&status=done&style=none&taskId=u550b4128-2783-4885-8f35-28683c449be&title=&width=1413)
