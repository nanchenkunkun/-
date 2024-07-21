# 典型回答
当我们使用Kafka发送消息时，一般有两种方式，分别是同步发送（`producer.send(msg).get()` ）及异步发送（`producer.send(msg, callback)`）。

同步发送的时候，可以在发送消息后，通过get方法等待消息结果：`producer.send(record).get();` ，这种情况能够准确的拿到消息最终的发送结果，要么是成功，要么是失败。

而异步发送，是采用了callback的方式进行回调的，可以大大的提升消息的吞吐量，也可以根据回调来判断消息是否发送成功。

不管是同步发送还是异步发送，最终都需要在Producer端把消息发送到Broker中，那么这个过程大致如下：

![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1684314627784-48da01a2-45c4-4a04-bb2e-1031018df8c7.png#averageHue=%23fcf6f3&clientId=uce348ae1-0067-4&from=paste&height=534&id=u2009b425&originHeight=1068&originWidth=1390&originalType=binary&ratio=2&rotation=0&showTitle=false&size=531542&status=done&style=none&taskId=u41fc8a3b-bb78-4681-99da-143e3919ce8&title=&width=695)![](media/16843057638079/16843120535902.jpg#id=GCKJp&originalType=binary&ratio=1&rotation=0&showTitle=false&status=done&style=none&title=)

Kafka 的 Producer 在发送消息时通常涉及两个线程，**主线程（Main）和发送线程（Sender）和一个消息累加器（RecordAccumulator）**

**Main线程**是 Producer 的入口，负责初始化 Producer 的配置、创建 KafkaProducer 实例并执行发送逻辑。它会按照用户定义的发送方式（同步或异步）发送消息，然后等待消息发送完成。

一条消息的发送，在调用send方法后，会经过**拦截器、序列化器及分区器**。

- 拦截器主要用于在消息发送之前和之后对消息进行定制化的处理，如对消息进行修改、记录日志、统计信息等。
- 序列化器负责将消息的键和值对象转换为字节数组，以便在网络上传输。
- 分区器决定了一条消息被发送到哪个 Partition 中。它根据消息的键（如果有）或者特定的分区策略，选择出一个目标 Partition。

**RecordAccumulator**在 Kafka Producer 中起到了消息积累和批量发送的作用，当 Producer 发送消息时，不会立即将每条消息发送到 Broker，而是将消息添加到 RecordAccumulator 维护的内部缓冲区中，RecordAccumulator 会根据配置的条件（如batch.size、linger.ms）对待发送的消息进行批量处理。

当满足指定条件时，RecordAccumulator 将缓冲区中的消息组织成一个批次（batch），然后一次性发送给 Broker。如果发送失败或发生错误，RecordAccumulator 可以将消息重新分配到新的批次中进行重试。这样可以确保消息不会丢失，同时提高消息的可靠性。

**Send线程**是负责实际的消息发送和处理的。发送线程会定期从待发送队列中取出消息，并将其发送到对应的 Partition 的 Leader Broker 上。它主要负责网络通信操作，并处理发送请求的结果，包括确认的接收、错误处理等。

**NetworkClient 和 Selector **是两个重要的组件，分别负责网络通信和 I/O 多路复用。

发送线程会把消息发送到Kafka集群中对应的Partition的Partition Leader，Partition Leader 接收到消息后，会对消息进行一系列的处理。它会将消息写入本地的日志文件（Log）

为了保证数据的可靠性和高可用性，Kafka 使用了消息复制机制。Leader Broker 接收到消息后，会将消息复制到其他副本（Partition Follower）。副本是通过网络复制数据的，它们会定期从 Leader Broker 同步消息。

每一个Partition Follower在写入本地log之后，会向Leader发送一个ACK。

但是我们的Producer其实也是需要依赖ACK才能知道消息有没有投递成功的，而这个ACK是何时发送的，Producer又要不要关心呢？这就涉及到了kafka的ack机制，生产者会根据设置的 request.required.acks 参数不同，选择等待或或直接发送下一条消息：

-  request.required.acks = 0 
   - 表示 Producer 不等待来自 Leader 的 ACK 确认，直接发送下一条消息。在这种情况下，如果 Leader 分片所在服务器发生宕机，那么这些已经发送的数据会丢失。
-  request.required.acks = 1 
   - 表示 Producer 等待来自 Leader 的 ACK 确认，当收到确认后才发送下一条消息。在这种情况下，消息一定会被写入到 Leader 服务器，但并不保证 Follow 节点已经同步完成。所以如果在消息已经被写入 Leader 分片，但是还未同步到 Follower 节点，此时Leader 分片所在服务器宕机了，那么这条消息也就丢失了，无法被消费到。
-  request.required.acks = -1 
   - Leader会把消息复制到集群中的所有ISR（In-Sync Replicas，同步副本），要等待所有ISR的ACK确认后，再向Producer发送ACK消息，然后Producer再继续发下一条消息。
