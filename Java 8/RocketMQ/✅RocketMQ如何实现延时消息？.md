# 典型回答

RocketMQ是支持**延迟消息**的，延迟消息写入到Broker后，不会立刻被消费者消费，需要等待指定的时长后才可被消费处理的消息，称为延时消息。

当消息发送到Broker后，Broker会将消息根据延迟级别进行存储。RocketMQ的延迟消息实现方式是：将消息先存储在内存中，然后使用Timer定时器进行消息的延迟，到达指定的时间后再存储到磁盘中，最后投递给消费者。

但是，RocketMQ的延迟消息并不是支持任意时长的延迟的，它只支持（5.0之前）：1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h这几个时长。

另外，**RocketMQ 5.0中新增了基于时间轮实现的定时消息。**

前面提到的延迟消息，并使用Timer定时器来实现延迟投递。但是，由于Timer定时器有一定的缺陷，比如在定时器中有大量任务时，会导致定时器的性能下降，从而影响消息投递。

因此，在RocketMQ 5.0中，采用了一种新的实现方式：**基于时间轮的定时消息。时间轮是一种高效的定时器算法，能够处理大量的定时任务，并且能够在O(1)时间内找到下一个即将要执行的任务，因此能够提高消息的投递性能。**

并且，**基于时间轮的定时消息能够支持更高的消息精度**，可以实现秒级、毫秒级甚至更小时间粒度的定时消息。

具体实现方式如下：

1. RocketMQ在Broker端使用一个时间轮来管理定时消息，将消息按照过期时间放置在不同的槽位中，这样可以大幅减少定时器任务的数量。

2. 时间轮的每个槽位对应一个时间间隔，比如1秒、5秒、10秒等，每次时间轮的滴答，槽位向前移动一个时间间隔。

3. 当Broker接收到定时消息时，根据消息的过期时间计算出需要投递的槽位，并将消息放置到对应的槽位中。

4. 当时间轮的滴答到达消息的过期时间时，时间轮会将该槽位中的所有消息投递给消费者。

使用方式：

```
//创建一个消息生产者
DefaultMQProducer producer = new DefaultMQProducer("ProducerGroupName");
producer.setNamesrvAddr("localhost:9876");
producer.start();


Message message = new Message("TopicTest", "TagA", "Hello RocketMQ".getBytes(RemotingHelper.DEFAULT_CHARSET));
// 设置消息的延迟级别为3，即延迟10s
message.setDelayTimeLevel(3);

// 消息发送
SendResult sendResult = producer.send(message);
System.out.printf("%s%n", sendResult);
```
