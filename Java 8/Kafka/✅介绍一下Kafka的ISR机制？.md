# 典型回答

ISR，是In-Sync Replicas，同步副本的意思。

在Kafka中，每个主题分区可以有多个副本(replica)。。ISR是与主副本（Leader Replica）保持同步的副本集合。ISR机制就是用于确保数据的可靠性和一致性的。

当消息被写入Kafka的分区时，它首先会被写入Leader，然后Leader将消息复制给ISR中的所有副本。只有当ISR中的所有副本都成功地接收到并确认了消息后，主副本才会认为消息已成功提交。这种机制确保了数据的可靠性和一致性。

# 扩展知识

## ISR列表维护

在Kafka中，ISR（In-Sync Replicas）列表的维护是通过副本状态和配置参数来进行的。具体的ISR列表维护机制在不同的Kafka版本中有所变化。

### before 0.9.x

在0.9.x之前的版本，Kafka 有一个核心的参数：`replica.lag.max.messages`，表示如果Follower落后Leader的消息数量超过了这个参数值，就认为Follower就会从ISR列表里移除。

但是，基于`replica.lag.max.messages`这种实现，在瞬间高并发访问的情况下会有问题：比如Leader瞬间接收到几万条消息，然后所有Follower还没来得及同步过去，此时所有follower都会被踢出ISR列表。

### after 0.9.x

Kafka从0.9.x版本开始，引入了`replica.lag.max.ms`参数，表示如果某个Follower的LEO（latest end offset）一直落后Leader超过了10秒，那么才会被从ISR列表里移除。

这样的话，即使出现瞬间流量，导致Follower落后很多数据，但是只要在限定的时间内尽快追上来就行了。


