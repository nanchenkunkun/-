# 典型回答

[✅MySQL主从复制的过程](https://www.yuque.com/hollis666/fo22bm/hoi4ql?view=doc_embed)

在MySQL的主从复制中，我们介绍过MySQL的主从复制的原理，在复制过程中，主库的binlog会不断地同步到从库，然后从库有一个SQL线程不断地拉取并重放这些SQL语句，那么，一旦日志内容太多的话，一个线程执行就会有延迟，就会导致主从延迟。

为了降低因为这个原因导致的主从延迟，所以MySQL提供了并行复制的方案。在MySQL的多个版本中，先后推出过很多个并行复制的方案：

- MySQL 5.6推出基于**库级别**的并行复制。
- MySQL 5.7推出基于**组提交**的的并行复制。
- MySQL 8.0 推出基于**WRITESET**的并行复制。

### 库级别并行复制

在MySQL 5.6中，并行是基于Schema的，也就是基于库的，在MySQL 5.6中，可以配置多个库并行进行复制，这意味着每个库都可以有自己的复制线程，可以并行处理来自不同库的写入。这提高了并行复制的性能和效率。

但是，其实大多数业务都是单库的，所以这种方案，在推出之后很多开发者和DBA并不买账，因为实在是太鸡肋了。

### 组提交的的并行复制

因为5.6的并行复制被很多人诟病，于是在MySQL 5.7中推出了基于组提交的的并行复制，这才是真正意义上的并行复制。这就是注明的 MTS (Enhanced Multi-Threaded Slave) ：[https://dev.mysql.com/blog-archive/multi-threaded-replication-performance-in-mysql-5-7/](https://dev.mysql.com/blog-archive/multi-threaded-replication-performance-in-mysql-5-7/)

[✅介绍下MySQL 5.6中的组提交](https://www.yuque.com/hollis666/fo22bm/bb860tpuha0cuza2?view=doc_embed)

先了解下组提交，然后接着往下看。

在组提交的介绍中我们说过，一个组中多个事务，都处于Prepare阶段之后，才会被优化成组提交。那么就意味着**如果多个事务他们能在同一个组内提交，这个就说明了这个几个事务在锁上是一定是没有冲突的。**
```java
binlog_transaction_dependency_tracking  = WRITESET                 #    COMMIT_ORDER          
transaction_write_set_extraction        = XXHASH64

```
**换句话说，就是这几个事务修改的一定不是同一行记录，所以他们之间才能互不影响，同时进入Prepare阶段，并且进行组提交。**

那么，没有冲突的多条SQL，是不是就可以在主备同步过程中，在备库上并行执行回放呢？

答案是可以的，因为一个组中的多条SQL之间互相不影响，谁先执行，谁后执行，结果都是一样的！

所以，这样Slave就可以用多个SQL现成来并行的执行一个组提交中的多条SQL，从而提升效率，降低主从延迟。

### 基于WRITESET的并行复制

前面的组提交大大的提升了主从复制的效率，但是他有一个特点，就是他依赖于主库的并行度，假如主库的并发比较高，那么才可以进行组提交，那么才能用到组提交的并行复制优化。

如果主库的SQL执行并没有那么频繁，那么时间间隔可能就会超过组提交的那两个参数阈值，就不会进行组提交。那么复制的时候就不能用并行复制了。

MySQL 8.0为了解决这个问题，引入了基于WriteSet的并行复制，这种情况下即使主库在串行提交的事务，只有互相不冲突，在备库就可以并行回放。

开启WRITESET：
```java
binlog_transaction_dependency_tracking  = WRITESET                 #    COMMIT_ORDER          
transaction_write_set_extraction        = XXHASH64
```

实际上Writeset是一个集合，使用的是C++ STL中的set容器：

```java
std::set<uint64> write_set_unique;
```

集合中的每一个元素都是hash值，这个hash值和transaction_write_set_extraction参数指定的算法有关（可选OFF、MURMUR32、XXHASH64，默认值 XXHASH64），其来源就是行数据的主键和唯一键。

WriteSet 是通过检测两个事务是否更新了相同的记录来判断事务能否并行回放的，因此需要在运行时保存已经提交的事务信息以记录历史事务更新了哪些行，并且在做更新的时候进行冲突检测，拿新更新的记录计算出来的hash值和WriteSet作比较，如果不存在，那么就认为是不冲突的，这样就可以共用同一个last_committed 、

>  last_committed 指的是该事务提交时，上一个事务提交的编号。



就这样，就能保证同一个write_set中的变更都是不冲突的，那么同一个write_set就可以并行的通过多个线程进行回放SQL了。
