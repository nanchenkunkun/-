# 典型回答

高水位（HW，High Watermark）是Kafka中的一个重要的概念，主要是用于管理消费者的进度和保证数据的可靠性的。

高水位标识了一个特定的消息偏移量（offset），即一个分区中已提交消息的最高偏移量（offset），消费者只能拉取到这个 offset 之前的消息。消费者可以通过跟踪高水位来确定自己消费的位置。

> 这里的已提交指的是ISRs中的所有副本都记录了这条消息


在Kafka中，HW主要有两个作用：

- 消费进度管理：消费者可以通过记录上一次消费的偏移量，然后将其与分区的高水位进行比较，来确定自己的消费进度。消费者可以在和高水位对比之后继续消费新的消息，确保不会错过任何已提交的消息。这样，消费者可以按照自己的节奏进行消费，不受其他消费者的影响。
- 数据的可靠性：高水位还用于确保数据的可靠性。在Kafka中，只有消息被写入主副本（Leader Replica）并被所有的同步副本（In-Sync Replicas，ISR）确认后，才被认为是已提交的消息。高水位表示已经被提交的消息的边界。只有高水位之前的消息才能被认为是已经被确认的，其他的消息可能会因为副本故障或其他原因而丢失。

还有一个概念，叫做LEO，即 Log End Offset，他是日志最后消息的偏移量。 它标识当前日志文件中下一条待写入消息的 offset。

![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1685778254153-71a9824b-b249-44b9-8368-e8b238a5dd16.png#averageHue=%23fdfcfb&clientId=u7c78b0a1-61e6-4&from=paste&height=453&id=u11872c34&originHeight=453&originWidth=1481&originalType=binary&ratio=1&rotation=0&showTitle=false&size=44437&status=done&style=none&taskId=ue4ff2db7-f28e-4e36-bee2-45fdcf2f87b&title=&width=1481)

当消费者消费消息时，它可以使用高水位作为参考点，只消费高水位之前的消息，以确保消费的是已经被确认的消息，从而保证数据的可靠性。如上图，只消费offet为6之前的消息。


我们都知道，在Kafka中，每个分区都有一个Leader副本和多个Follower副本。

当Leader副本发生故障时，Kafka会选择一个新的Leader副本。这个切换过程中，需要保证数据的一致性，即新的Leader副本必须具有和旧Leader副本一样的消息顺序。

为了实现这个目标，Kafka引入了Leader Epoch的概念。Leader Epoch是一个递增的整数，每次副本切换时都会增加。它用于标识每个Leader副本的任期。

每个副本都会维护自己的Leader Epoch记录。它记录了副本所属的分区在不同Leader副本之间切换时的任期。

在副本切换过程中，新的Leader会检查旧Leader副本的Leader Epoch和高水位。只有当旧Leader副本的Leader Epoch小于等于新Leader副本的Leader Epoch，并且旧Leader副本的高水位小于等于新Leader副本的高水位时，新Leader副本才会接受旧Leader副本的数据。

通过使用Leader Epoch和高水位的验证，Kafka可以避免新的Leader副本接受旧Leader副本之后的消息，从而避免数据回滚。只有那些在旧Leader副本的Leader Epoch和高水位之前的消息才会被新Leader副本接受。

# 扩展知识

## Leader Epoch的过程

每个分区都有一个初始的Leader Epoch，通常为0。

当Leader副本发生故障或需要进行切换时，Kafka会触发副本切换过程。

副本切换过程中，Kafka会从ISR（In-Sync Replicas，同步副本）中选择一个新的Follower副本作为新的Leader副本。

新的Leader副本会增加自己的Leader Epoch，使其大于之前的Leader Epoch。这表示进入了一个新的任期。

新的Leader副本会验证旧Leader副本的状态以确保数据的一致性。它会检查旧Leader副本的Leader Epoch和高水位。

如果旧Leader副本的Leader Epoch小于等于新Leader副本的Leader Epoch，并且旧Leader副本的高水位小于等于新Leader副本的高水位，则验证通过。

一旦验证通过，新的Leader副本会开始从ISR中的一部分副本复制数据，以确保新Leader上的数据与旧Leader一致。

一旦新的Leader副本复制了旧Leader副本的所有数据，并达到了与旧Leader副本相同的高水位，副本切换过程就完成了。
