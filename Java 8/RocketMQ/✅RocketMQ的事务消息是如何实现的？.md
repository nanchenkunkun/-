# 典型回答

RocketMQ的事务消息是通过TransactionListener接口来实现的。

在发送事务消息时，首先向RocketMQ Broker发送一条“half消息”（即半消息），半消息将被存储在Broker端的事务消息日志中，但是这个消息还不能被消费者消费。

接下来，在半消息发送成功后，应用程序通过执行本地事务来确定是否要提交该事务消息。

如果本地事务执行成功，就会通知RocketMQ Broker提交该事务消息，使得该消息可以被消费者消费；否则，就会通知RocketMQ Broker回滚该事务消息，该消息将被删除，从而保证消息不会被消费者消费。

![](https://cdn.nlark.com/yuque/0/2023/jpeg/5378072/1676883263316-1e97eaa2-df90-44f6-8cc9-3037a438d0a0.jpeg#averageHue=%23f3f2f2&clientId=u8be50315-99d8-4&from=paste&id=ucc76a7ba&originHeight=460&originWidth=1147&originalType=url&ratio=1.100000023841858&rotation=0&showTitle=false&status=done&style=none&taskId=u64db11c1-fcd2-4e77-b7a7-37ab6d2d97e&title=)

拆解下来的话，主要有以下4个步骤：

1. 发送半消息：应用程序向RocketMQ Broker发送一条半消息，该消息在Broker端的事务消息日志中被标记为“prepared”状态。

2. 执行本地事务：RocketMQ会通知应用程序执行本地事务。如果本地事务执行成功，应用程序通知RocketMQ Broker提交该事务消息。

3. 提交事务消息：RocketMQ收到提交消息以后，会将该消息的状态从“prepared”改为“committed”，并使该消息可以被消费者消费。

4. 回滚事务消息：如果本地事务执行失败，应用程序通知RocketMQ Broker回滚该事务消息，RocketMQ将该消息的状态从“prepared”改为“rollback”，并将该消息从事务消息日志中删除，从而保证该消息不会被消费者消费。

# 扩展知识

## 如果一直没收到COMMIT或者ROLLBACK怎么办？

在RocketMQ的事务消息中，如果半消息发送成功后，RocketMQ Broker在规定时间内没有收到COMMIT或者ROLLBACK消息。

RocketMQ会向应用程序发送一条检查请求，应用程序可以通过回调方法返回是否要提交或回滚该事务消息。如果应用程序在规定时间内未能返回响应，RocketMQ会将该消息标记为“UNKNOW”状态。

在标记为“UNKNOW”状态的事务消息中，如果应用程序有了明确的结果，还可以向MQ发送COMMIT或者ROLLBACK。

但是MQ不会一直等下去，如果过期时间已到，RocketMQ会自动回滚该事务消息，将其从事务消息日志中删除。


## 第一次发送半消息失败了怎么办？

在事务消息的一致性方案中，我们是先发半消息，再做业务操作的

所以，如果半消息发失败了，那么业务操作也不会进行，不会有不一致的问题。

遇到这种情况重试就行了。（可以自己重试，也可以依赖上游重试）

## 为什么要用事务消息？

很多人看完事务消息会有一个疑惑：本地事务执行完成之后再发送消息有什么区别？为什么要有事务消息呢？

主要是因为：本地事务执行完成之后再发送消息可能会发消息失败。

一旦发送消息失败了，那么本地事务提交了，但是消息没成功，那么监听者就收不到消息，那么就产生数据不一致了。

那如果用事务消息。先提交一个半消息，然后执行本地事务，再发送一个commit的半消息。如果后面这个commit半消息失败了，MQ是可以基于第一个半消息不断反查来推进状态的。这样只要本地事务提交成功，最终MQ也会成功。如果本地事务rolllback，那么MQ的消息也会rollback。保证了一致性。

## MQ实现分布式事务

[✅如何基于MQ实现分布式事务](https://www.yuque.com/hollis666/fo22bm/yuku2qztfb8ki6wg?view=doc_embed)
