# 典型回答

在 Redis 中，他的hash表结构随着数据量的增大可能会导致扩容，通常是将数组大小扩大为原来的两倍，而在扩容过程中，因为容量变化了，所以元素在新的hash表中所处的位置也会随之变化，这个变化过程就是通过rehash实现的。

可以参考下hashmap的rehash过程：<br />[✅HashMap是如何扩容的？](https://www.yuque.com/hollis666/fo22bm/co1ul8?view=doc_embed)

而随着Redis的hash表越来越大，rehash的成本也会越来越高。Redis中实现了一种渐进式rehash的方案，他可以在哈希表rehash操作时，分多个步骤逐渐完成的方式，这样不会因为要一次性把所有元素都完成迁移而导致IO升高，线程阻塞。这个特性使得Redis可以在继续提供读写服务的同时，逐步迁移数据到新的哈希表，而不会对性能造成明显的影响。

在 Redis 中，他的hash结构其实底层是使用了两个全局哈希表的。我们把他们称之为哈希表 1 和哈希表 2。并且会维护一个rehashindex ，初始值为-1，来记录当前rehash的下标位置。

当我们开始向hash表中插入数据时，只使用哈希表 1，不断向其中添加数据。

![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1692419575799-517aced7-3c0b-480a-be5e-447a50ca375d.png#averageHue=%23fbfaf5&clientId=u94c06ba4-2c10-4&from=paste&height=660&id=ufe23182c&originHeight=660&originWidth=1375&originalType=binary&ratio=1&rotation=0&showTitle=false&size=33335&status=done&style=none&taskId=u18e5b25c-a72d-4b8e-bff6-a8f543c243e&title=&width=1375)

而随着数据逐渐增多，当元素个数和hash表中的数组长度一致时，就会触发rehash动作，这时候，会把哈希表2的容量扩大一倍。然后就开始进入rehash流程。

![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1692419598612-bd29a7c5-0da4-48f4-b324-8cf56f7a91b6.png#averageHue=%23fcfaf7&clientId=u94c06ba4-2c10-4&from=paste&height=876&id=ubc591374&originHeight=876&originWidth=1318&originalType=binary&ratio=1&rotation=0&showTitle=false&size=40023&status=done&style=none&taskId=u55dc1e11-9115-4c93-ba76-2faae5a0d14&title=&width=1318)


在进入rehash过程中，不会立刻把哈希表1中的数据全部rehash到哈希表2中，而是在后续有新的增删改查操作时，会从头开始进行rehash动作。

假如，我们现在要新增一个元素：

![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1692419675045-d8066838-5c20-4d5b-b92a-d22a5e074e3b.png#averageHue=%23fcfbf7&clientId=u94c06ba4-2c10-4&from=paste&height=791&id=u2119f6d9&originHeight=791&originWidth=1573&originalType=binary&ratio=1&rotation=0&showTitle=false&size=57230&status=done&style=none&taskId=ud472ea1f-8fe3-438e-969c-1da1474e0ec&title=&width=1573)

那么就会从当前的hashindex开始，把这个哈希表1的hashindex这个位置的桶中的数据全部rehash到哈希表2中，然后rehashindex +1 。

然后再在哈希表2中进行添加操作：

![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1692419800439-5a62ebb9-35f6-4814-bce1-98de88fb8a20.png#averageHue=%23fbfaf6&clientId=u94c06ba4-2c10-4&from=paste&height=642&id=u4d60bd2b&originHeight=642&originWidth=809&originalType=binary&ratio=1&rotation=0&showTitle=false&size=27420&status=done&style=none&taskId=u9cc25c42-85b1-48ed-b30c-bb87eb47001&title=&width=809)

在后续的其他操作中也一样，会沿着hashindex一直往后开始进行逐个桶的rehash，一直到哈希表1中的元素全部完成rehash。

然后再把哈希表1和哈希表2的指针互换一下（后续会再把哈希表2给直接置为NULL），后续的增删改查继续在新的哈希表1中操作，直到下一次rehash开始。


# 扩展知识

## 查询怎么办

在Rehash开始时，Redis会创建一个新的哈希表（称为哈希表2），而旧的哈希表（称为哈希表1）仍然保留。这时，Redis同时维护这两个哈希表。

当执行查询操作时，Redis首先会在哈希表1中查找键。如果在哈希表1中没找到，Redis会接着在哈希表2中查找。这确保了即使在Rehash过程中，所有的键都是可查询的。

当哈希表1中的所有数据都迁移到哈希表2后，Rehash操作完成。此时，哈希表1会被释放，哈希表2成为当前使用的哈希表。 查询就直接查询哈希表2即可
