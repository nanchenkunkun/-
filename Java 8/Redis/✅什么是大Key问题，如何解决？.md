Big Key是Redis中存储了大量数据的Key，不要误以为big key只是表示Key的值很大，他还包括这个Key对应的value占用空间很多的情况，通常在String、list、hash、set、zset等类型中出现的问题比较多。其中String类型就是字符串的值比较大，而其他几个类型就是其中元素过多的情况。

Redis的Big Key可能存在以下几个危害：

**1、影响性能：**由于big key的values占用的内存会很大，所以读取它们的速度会很慢，会影响系统的性能。<br />**2、占用内存：** 大量的big key也会占满Redis的内存，让Redis无法继续存储新的数据，而且也会导致Redis卡住<br />**3、内存空间不均匀：**比如在 Redis 集群中，可能会因为某个节点上存储了Big Key，导致多个节点之间内存使用不均匀。<br />**4、影响Redis备份和恢复：**如果从RDB文件中恢复全量数据时，可能需要大量的时间，甚至无法正常恢复。<br />**5、搜索困难：**由于大key可能非常大，因此搜索key内容时非常困难，并且可能需要花费较长的时间完成搜索任务。<br />**6、迁移困难：**大对象的迁移和复制压力较大，极易破坏缓存的一致性<br />**7、过期执行耗时：**如果 Bigkey 设置了过期时间，当过期后，这个 key 会被删除，而大key的删除过程也比较耗时

对于Big Key问题的处理，重点要在识别和解决上面。

# 扩展知识

## 多大算大？

Redis中多大的key算作大key并没有一个固定的标准，因为这主要取决于具体的场景和应用需求。一般来说，如果一个key的value比较大，占用的内存比较多，或者某个key包含的元素数量比较多，这些都可以被认为是大key。

通常情况下，建议不要超过以下设定，超过这些数量就可能会影响Redis的性能。

- 对于 String 类型的 Value 值，值超过 5MB（腾讯云定义是10M，阿里云定义是5M，我认为5M合适一点）。
- 对于 Set 类型的 Value 值，含有的成员数量为 10000 个（成员数量多）。
- 对于 List 类型的 Value 值，含有的成员数量为 10000 个（成员数量多）。
- 对于 Hash 格式的 Value 值，含有的成员数量 1000 个，但所有成员变量的总 Value 值大小为 100MB（成员总的体积过大）。

但是，这些并不是绝对的限制，而是一个经验值，具体的情况还需要根据应用场景和实际情况进行调整。

## 识别big key

在识别方面，Redis中的big key可以识别的程序是“redis-cli”，用户可以通过在终端中输入“redis-cli –bigkeys” 来获取Redis中的big key。当redis-cli被调用时，它将搜索所有Redis数据库中包含大量内存数据的key，并且会将其保存在本地标准输出文件中：

```
# Scanning the entire keyspace to find biggest keys as well as
# average sizes per key type. You can use -i 0.1 to sleep 0.1 sec
# per 100 SCAN commands (not usually needed).

Biggest string found so far 'mykey' with 160012 bytes
Biggest list found so far 'mylist' with 2304 items
Biggest set found so far 'myset' with 1230 members
Biggest zset found so far 'myzset' with 3220 members
Biggest hash found so far 'myhash' with 412 fields
```

## 处理Big Key

想要解决Big Key的问题，根据具体的业务情况有很多不同的方案，下面简单列几个：

**1、有选择地删除Big Key：**针对Big Key，我们可以针对一些访问频率低的进行有选择性的删除，删除Big Key来优化内存占用。

2、除了手动删除以外，还可以通过**合理的设置缓存TTL**，避免过期缓存不及时删除而增大key大小。

3、Big Key的主要问题就是Big，所以我们可以想办法解决big的问题，那就是拆分呗，**把big的key拆分开**：<br />a、在业务代码中，将一个big key有意的进行拆分，比如根据日期或者用户尾号之类的进行拆分。使用小键替代大键可以有效减小存储空间，从而避免影响系统性能<br />b、使用Cluster集群模式，以将大 key 分散到不同服务器上，以加快响应速度。

4、**部分迁移：**将大键存放在单独的数据库中，从而实现对大键的部分迁移

