# 典型回答

支持，Elasticsearch 支持通过使用文档版本控制来实现乐观锁。（[https://www.elastic.co/guide/en/elasticsearch/reference/current/optimistic-concurrency-control.html](https://www.elastic.co/guide/en/elasticsearch/reference/current/optimistic-concurrency-control.html) ）

在 ES 中，每个文档存储时都有一个 `_version `字段，这个版本号在每次文档更新时自动增加。当我们执行更新、删除或者使用脚本处理文档时，可以指定这个版本号来确保正在操作的文档是预期中的版本。如果操作中的版本号与存储在索引中的文档版本号不一致，说明文档已被其他操作更改，当前操作将会失败。（CAS）

但是，从Elasticsearch 6.7 版本开始，使用 `_version` 关键字进行乐观锁已经被废弃了，替代方法是使用 if_seq_no 和  if_primary_term 来指定版本。

假设有一个文档：

```
{
  "_index": "products",
  "_type": "_doc",
  "_id": "1",
  "_version": 10,
  "_source": {
    "name": "Coffee",
    "price": 20
  }
}

```

基于他，我们可以在更新时进行乐观锁控制，避免发生并发修改：

```
POST /products/_doc/1?if_seq_no=312&if_primary_term=2
{
  "name": "Coffee",
  "price": 22
}
```
<br />这里的 if_seq_no 和 if_primary_term 是 Elasticsearch 中的字段，用于管理乐观锁。如果文档自上次你读取以来没有被更改，if_seq_no 和 if_primary_term 会匹配，你的更改就会被应用。如果不匹配，更新操作会失败。

- seq_no 是一个递增的序列号，表示文档的每次修改；
- primary_term 表示主分片的当前任期，每当主分片发生变化时，这个值会增加。

# 扩展知识

## 为什么version 被废弃

原来的 _version 机制是基于单一递增整数，它主要适用于简单的冲突检测，但在复杂的分布式系统中，仅依靠版本号可能无法准确地反映数据的历史和复制状态。尤其是在发生网络分区或节点故障时，仅凭 _version 可能导致数据丢失或过时的数据被错误地写入。

并且在高并发和高可用的环境下，单一的版本号不足以处理因节点故障或网络问题导致的多个副本之间的数据不一致问题。

而if_seq_no 和 if_primary_term 提供了一种方式来跟踪每个文档变更的序列号和主分片的期限。这允许系统更精确地控制和理解文档的变化历史，特别是在分布式和高并发的环境中。

使用这两个参数可以更准确地确定操作是否应当被执行。seq_no 是一个递增的序列号，表示文档的每次修改；primary_term 表示主分片的当前任期，每当主分片发生变化时，这个值会增加。这两个值共同工作，可以帮助系统在主分片发生变化时，侦测并防止冲突。

这种机制帮助确保即使在集群状态发生变化（如分片重新分配到新的节点）的情况下，也不会应用基于过时副本的更新。它有效地防止了脑裂问题。

[✅什么是脑裂？如何解决？](https://www.yuque.com/hollis666/fo22bm/xuxwgui3f8ti2a0y?view=doc_embed)



## ES 支持悲观锁吗？

不支持！


