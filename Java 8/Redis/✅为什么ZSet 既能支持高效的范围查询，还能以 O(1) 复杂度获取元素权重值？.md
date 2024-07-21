# 典型回答

Sorted Set 能支持范围查询，这是因为它的核心数据结构设计采用了跳表，而它又能O(1)的复杂度获取元素权重，这是因为它同时采用了哈希表进行索引。

```java
typedef struct zset 
{ 
    dict *dict; 
    zskiplist *zsl;
} zset;
```

以上是zset的数据结构，其中包含了两个成员，分别是哈希表dict和跳表zsl。

dict存储 member->score 之间的映射关系，所以 ZSCORE 的时间复杂度为 O(1)。skiplist 是一个「有序链表 + 多层索引」的结构，查询元素的复杂度是 O(logN)，所以他的查询效率很高。
