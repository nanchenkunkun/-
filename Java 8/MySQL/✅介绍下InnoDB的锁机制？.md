# 典型回答

Innodb中的锁在锁的级别上一般分为两种，一种是共享锁（S锁），一种是排他锁（X锁）。

## 共享锁&排他锁

**共享锁又称读锁，是读取操作创建的锁**。其他用户可以并发读取数据，但任何事务都不能对数据进行修改（获取数据上的排他锁），直到已释放所有共享锁。

**排他锁又称写锁，**如果事务T对数据A加上排他锁后，则其他事务不能再对A加任任何类型的锁。**获得排他锁的事务既能读数据，又能修改数据。**

[✅什么是排他锁和共享锁？](https://www.yuque.com/hollis666/fo22bm/ec5yhfon858vcq5p?view=doc_embed)

这就是我们经常会看到的X锁和S锁。即排他锁和共享锁。

除了S锁和X锁之外，Innodb还有两种锁，是IX锁和IS锁，这里的I是Intention 的意思，即意向锁。IX就是意向排他锁，IS就是意向共享锁。

## 意向锁

[✅什么是意向锁？](https://www.yuque.com/hollis666/fo22bm/zf7nalngrigml547?view=doc_embed)

**当一个事务请求获取一个行级锁或表级锁时，MySQL会自动获取相应的表的意向锁。**

1. IS锁： 表示事务打算在资源上设置共享锁（读锁）。这通常用于表示事务计划读取资源，并不希望在读取时有其他事务设置排它锁。
2. IX锁： 表示事务打算在资源上设置排它锁（写锁）。这表示事务计划修改资源，并不希望有其他事务同时设置共享或排它锁。



意向锁其实是一个表级锁！

以下是MySQL官网上给出的这几种锁之间的冲突关系：<br />![image.png](https://cdn.nlark.com/yuque/0/2024/png/5378072/1709373174066-8297e99e-15e4-41bc-bd0c-775b907dae67.png#averageHue=%23f7f6f5&clientId=u5374ebbd-cbef-4&from=paste&height=242&id=u4f60da4a&originHeight=173&originWidth=473&originalType=binary&ratio=1&rotation=0&showTitle=false&size=13999&status=done&style=none&taskId=u50787dda-9497-461d-ab2c-97c40512881&title=&width=663)


## 记录锁

**Record Lock，翻译成记录锁，是加在索引记录上的锁。**例如，`SELECT c1 FROM t WHERE c1 = 10 For UPDATE;`会对c1=10这条记录加锁，为了防止任何其他事务查询、插入、更新或删除c1值为10的行。

**Record Lock是一个典型的行级锁**，但是需要特别注意的是，Record锁的不是这行记录，而是锁索引记录。并且**Record lock锁且只锁索引**！

如果没有索引怎么办？对于这种情况，InnoDB 会创建一个隐藏的聚簇索引，并使用这个索引进行记录锁定。

> **如果我们在一张表中没有定义主键，那么，MySQL会默认选择一个唯一的非空索引作为聚簇索引。如果没有适合的非空唯一索引，则会创建一个隐藏的主键（row_id）作为聚簇索引。**


关于记录锁的加锁原则，以及Gap Lock和Next Key Lock，请看这篇：

[✅MySQL的行级锁锁的到底是什么？](https://www.yuque.com/hollis666/fo22bm/kfygzw?view=doc_embed)

## 插入意向锁

插入意向锁是一种由插入操作在行插入之前设置的间隙锁。这种锁表明了插入的意图，以这样一种方式，如果多个事务插入到同一索引间隙中但不在间隙内的相同位置插入，则它们不需要相互等待。

假设有索引记录的值为 4 和 7。分别尝试插入值为 5 和 6 的不同事务，在获取插入行的独占锁之前，各自用插入意向锁锁定 4 和 7 之间的间隙，但由于行不冲突，所以它们不会相互阻塞。但是如果他们的都要插入6，那么就会需要阻塞了。

## AUTO-INC 锁

AUTO-INC 锁是一种特殊的表级锁，由插入带有 AUTO_INCREMENT 列的表的事务获取。在最简单的情况下，如果一个事务正在向表中插入值，任何其他事务都必须等待，以便执行它们自己的插入操作，这样第一个事务插入的行就会接收到连续的主键值。

innodb_autoinc_lock_mode 变量控制用于自增锁定的算法。它允许你选择如何在可预测的自增值序列和插入操作的最大并发性之间进行权衡。

[✅高并发情况下自增主键会不会重复，为什么？](https://www.yuque.com/hollis666/fo22bm/oxdeyunw5v65gqen?view=doc_embed)

在MySQL 5.1之前，AUTO-INC锁是一种表级锁。

# 扩展知识

## 更多锁的分类

按**锁的粒度**划分，可分为全局锁、表级锁、行级锁、页级锁（Innodb中没有）<br />[✅InnoDB中的表级锁、页级锁、行级锁？](https://www.yuque.com/hollis666/fo22bm/vef33zs32vyylktv?view=doc_embed)

按**锁的级别**划分，可分为共享锁、排他锁 <br />[✅什么是排他锁和共享锁？](https://www.yuque.com/hollis666/fo22bm/ec5yhfon858vcq5p?view=doc_embed)<br />按**加锁方式**划分，可分为自动锁、显示锁<br />按**使用方式**划分，可分为乐观锁、悲观锁<br />[✅乐观锁与悲观锁如何实现？](https://www.yuque.com/hollis666/fo22bm/ionc18?view=doc_embed)<br />按**锁的对象**划分，可分为记录锁、间隙锁、临键锁<br />[✅MySQL的行级锁锁的到底是什么？](https://www.yuque.com/hollis666/fo22bm/kfygzw?view=doc_embed)

另外还有MDL锁、DDL锁、意向锁等

[✅什么是意向锁？](https://www.yuque.com/hollis666/fo22bm/zf7nalngrigml547?view=doc_embed)

