# 典型回答

B树（B- Tree）和B+树（B+ Tree）是两种常用的树结构，B是Balanced首字母，平衡的意思，他们经常用在各种数据库中，比如MySQL的InnoDB引擎使用的就是B+树，而MongoDB使用的就是B树存储。

B树属于多叉树又名平衡多路查找树，B树的设计目的是为了减少磁盘访问次数，提高查询性能。

B树是一种平衡的多叉树，通常我们说m阶的B树，它必须满足如下条件：

- 每个节点最多只有m个子节点。
- 每个非叶子节点（除了根）具有至少⌈ m/2⌉子节点。
- 如果根不是叶节点，则根至少有两个子节点。
- 具有k个子节点的非叶节点包含k -1个键。
- 所有叶子都出现在同一水平，没有任何信息（高度一致）。

![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1688460675265-1237e32b-f43d-4ba4-aba6-975026b9949b.png#averageHue=%23f4f4f4&clientId=u71f828ec-fabe-4&from=paste&height=498&id=u0185169a&originHeight=996&originWidth=2896&originalType=binary&ratio=2&rotation=0&showTitle=false&size=1799920&status=done&style=none&taskId=u0a2cef75-d5f9-4cae-9068-12e20a84d1a&title=&width=1448)

B树具有以下特点：

1. B树是一种多路搜索树，每个节点可以包含多个子节点。相对于二叉搜索树，B树可以存储更多的关键字和子节点，从而降低了树的高度，减少了磁盘访问次数。
2. B树通过在插入和删除操作时进行节点分裂和合并，保持树的平衡状态。这样可以确保树的高度始终保持在可接受的范围内，保证了较快的查询性能。
3. B树的节点中的关键字按照升序排列，使得范围查询操作更加高效。通过遍历树中的叶子节点，可以顺序获取连续的数据。
4. B树的关键字和数据项可以存储在叶子节点和非叶子节点上。并且每个关键字出现且只出现在一个节点中。
5. B树的搜索可能在非叶子节点上结束，他的搜索性能相当于在关键字全集中做二分查找。

B+树是B-树的变体，也是一种多路搜索树, 它与 B- 树的不同之处在于以下三个方面

1. 数据存储位置：在B树中，数据项存储在叶子节点和非叶子节点上，而在B+树中，数据项只存储在叶子节点上。非叶子节点只包含键值信息。
2. 叶子节点指针：B树的叶子节点之间没有指针连接，每个叶子节点独立存储数据项。而B+树的叶子节点通过指针连接成一个链表，可以方便地进行范围查询。

![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1693117444889-9b121593-a91f-478c-9413-9e40af0298f8.png#averageHue=%23f6f6f6&clientId=u70e95fe2-0e7f-4&from=paste&height=543&id=u949da06b&originHeight=543&originWidth=1367&originalType=binary&ratio=1&rotation=0&showTitle=false&size=449797&status=done&style=none&taskId=ue9ff0f1b-3ba9-4f59-a657-71cb3a88795&title=&width=1367)

由于B+树的叶子节点之间通过双向指针链接进而形成了链表，所以B+树在范围查询时更加高效。通过遍历叶子节点链表，可以获取连续的数据项，而不需要回溯到非叶子节点。





