# 典型回答

Kafka 的重平衡机制是指在消费者组中新增或删除消费者时，Kafka 集群会重新分配主题分区给各个消费者，以保证每个消费者消费的分区数量尽可能均衡。

重平衡机制的目的是实现消费者的负载均衡和高可用性，以确保每个消费者都能够按照预期的方式消费到消息。

![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1678605797572-80a61f6e-b9d6-4d55-9f43-d0f2bd28af12.png#averageHue=%23fbf9f7&clientId=ucd94bd98-b82d-4&from=paste&id=u813e9123&originHeight=2018&originWidth=3605&originalType=url&ratio=1&rotation=0&showTitle=false&size=1540049&status=done&style=none&taskId=ua754224d-4f5a-4b70-8eb7-910a23274b7&title=)

**重平衡的 3 个触发条件：**

- 消费者组成员数量发生变化。
- 订阅主题数量发生变化。
- 订阅主题的分区数发生变化。

当Kafka 集群要触发重平衡机制时，大致的步骤如下：

1. **暂停消费**：在重平衡开始之前，Kafka 会暂停所有消费者的拉取操作，以确保不会出现重平衡期间的消息丢失或重复消费。

2. **计算分区分配方案**：Kafka 集群会根据当前消费者组的消费者数量和主题分区数量，计算出每个消费者应该分配的分区列表，以实现分区的负载均衡。

3. **通知消费者**：一旦分区分配方案确定，Kafka 集群会将分配方案发送给每个消费者，告诉它们需要消费的分区列表，并请求它们重新加入消费者组。

4. **重新分配分区**：在消费者重新加入消费者组后，Kafka 集群会将分区分配方案应用到实际的分区分配中，重新分配主题分区给各个消费者。

5. **恢复消费**：最后，Kafka 会恢复所有消费者的拉取操作，允许它们消费分配给自己的分区。

Kafka 的重平衡机制能够有效地实现消费者的负载均衡和高可用性，提高消息的处理能力和可靠性。但是，由于重平衡会带来一定的性能开销和不确定性，因此在设计应用时需要考虑到重平衡的影响，并采取一些措施来降低重平衡的频率和影响。

**在重平衡过程中，所有 Consumer 实例都会停止消费，等待重平衡完成。但是目前并没有什么好的办法来解决重平衡带来的STW，只能尽量避免它的发生。**

# 扩展知识

## 消费者的五种状态

Kafka的Consumer实例五种状态，分别是：


| 状态 | 描述 |
| --- | --- |
| Empty | 组内没有任何成员，但是消费者可能存在已提交的位移数据，而且这些位移尚未过期 |
| Dead | 同样是组内没有任何成员，但是组的元数据信息已经被协调者端移除，协调者保存着当前向他注册过的所有组信息 |
| PreparingRebalance | 消费者组准备开启重平衡，此时所有成员都需要重新加入消费者组 |
| CompletingRebalance | 消费者组下所有成员已经加入，各个成员中等待分配方案 |
| Stable | 消费者组的稳定状态，该状态表明重平衡已经完成，组内成员能够正常消费数据 |

状态的流转过程：


![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1678606209834-a484dcf7-dece-4eb1-988b-17ff3affcc5f.png#averageHue=%23f4f4f4&clientId=ucd94bd98-b82d-4&from=paste&id=u9b10feb4&originHeight=1505&originWidth=3580&originalType=url&ratio=1&rotation=0&showTitle=false&size=713833&status=done&style=none&taskId=u819fea87-d2e4-4a89-91ed-f046c3aa719&title=)
