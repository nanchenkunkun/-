# 典型回答

[✅介绍一下Redis的集群模式？](https://www.yuque.com/hollis666/fo22bm/namhuv165lorwudw?view=doc_embed)

Redis Cluster采用主从复制模式来提高可用性。每个分片都有一个主节点和多个从节点。主节点负责处理写操作，而从节点负责复制主节点的数据并处理读请求。在Redis的Cluster 集群模式中，会对数据进行数据分片，将整个数据集分配给不同节点。

这个思想就和我们在MySQL 中做分库分表是一样的，都是通过一定的分片算法，把数据分散到不同的节点上进行存储。

**那么和 MySQL 对跨库事务支持存在限制一样，在 Redis Cluster 中使用事务和 Lua 脚本时，也是有一定的限制的。**

**在 Redis Cluster 中，事务不能跨多个节点执行**。事务中涉及的所有键必须位于同一节点上。如果尝试在一个事务中包含多个分片的键，事务将失败。另外，对 **WATCH** 命令也用同样的限制，要求他只能监视位于同一分片上的键。

和事务相同，**执行 Lua 脚本时，脚本中访问的所有键也必须位于同一节点。**Redis 不会在节点之间迁移数据来支持跨节点的脚本执行。Lua 脚本执行为原子操作，但是如果脚本因为某些键不在同一节点而失败，整个脚本将终止执行，可能会影响数据的一致性。

当我们要跨节点执行 lua 的时候，会报错提示：command keys must in same slot。（详见：[https://redis.io/docs/latest/operate/oss_and_stack/reference/cluster-spec/](https://redis.io/docs/latest/operate/oss_and_stack/reference/cluster-spec/) ）

### 如何解决？

[✅如何在 Redis Cluster 中执行 lua 脚本？](https://www.yuque.com/hollis666/fo22bm/hrbvqgdg21k8znhw?view=doc_embed)
