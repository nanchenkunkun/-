# 典型回答

[✅InnoDB的一次更新事务是怎么实现的？](https://www.yuque.com/hollis666/fo22bm/wmmyt7a9vr7qlwsl?view=doc_embed)

看本文之前，请先了解一下binlog、redolog等，知道他们的作用及写入过程。

看到这里默认大家了解以上背景了，那么，正文开始：

MySQL的主从复制，是基于binlog实现的，主要过程是这样的：<br />![image.png](https://cdn.nlark.com/yuque/0/2024/png/5378072/1710418626146-1bacdddd-68eb-4eff-80fa-1c006e814fc5.png#averageHue=%23fafaf9&clientId=u5a31ea62-bb5f-4&from=paste&height=444&id=uc4c29031&originHeight=666&originWidth=1337&originalType=binary&ratio=1.5&rotation=0&showTitle=false&size=79401&status=done&style=none&taskId=u7c3ae114-411b-48c5-9bf1-48892daa07d&title=&width=891.3333333333334)

1、从服务器在开启主从复制后，会创建出两个线程：I/O线程和SQL线程

2、从服务器的I/O线程，会尝试和主服务器建立连接，相对应的，主服务中也有一个binlog dump线程， 是专门来和从服务器的I/O线程做交互的。

3、从服务器的I/O线程会告诉主服务的dump线程自己要从什么位置开始接收binlog

4、主服务器在更新过程中，将更改记录保存到自己的binlog中，根据不同的binlog格式，记录的内容可能不一样。

5、在dump线程检测到binlog变化时，会从指定位置开始读取内容，然后会被slave的I/O线程把他拉取过去。

- 这里需要注意，有些资料上面说这里是主服务器向从服务器推的，但是，**实际上是从服务器向主服务器拉的。**（[https://dev.mysql.com/doc/refman/8.0/en/replication-implementation.html](https://dev.mysql.com/doc/refman/8.0/en/replication-implementation.html) ）

<br />
> 拉的模式，从库可以自行管理同步进度和处理延迟。


6、从服务器的I/O线程接收到通知事件后，会把内容保存在relay log中。

7、从服务器还有一个SQL线程，他会不断地读取他自己的relay log中的内容，把他解析成具体的操作，然后写入到自己的数据表中。

# 扩展知识

## 复制方式

MySQL目前支持多种复制方式，其中包括了全同步复制、异步复制和半同步复制。

**异步复制**：这是MySQL默认的复制方式，在异步复制的方式中主库在执行完事务操作以后，会立刻给客户端返回。他不需要关心从库是否完成该事务的执行。

这种方式会导致一个问题，那就是当主库出现故障时，主库虽然事务执行完了，但是可能还没来得及把数据同步给从库，就挂了。那么当从库升级为主库之后，他会丢失了这次事务的变更内容。

**全同步复制**：全同步复制的这个方式中，当主库执行完一个事务之后，他会等待所有的从库完成数据复制之后，才会给客户端反馈。

这种方式安全性可以保障了，但是性能很差。如果从库比较多的话，会导致整个过程更加长。

**半同步复制**：半同步复制是介于全同步复制和异步复制之间的一种方案。他再执行完一个事务之后，也不会立刻给客户端反馈，但是也不会等所有从库都完成事务，而是等其中一个从库完成接收到事件之后，再反馈给客户端。

在半同步复制这个方案中，会在事务提交的2阶段都完成之后，等待从库接收到binlog，然后再返回成功。

[✅InnoDB的一次更新事务是怎么实现的？](https://www.yuque.com/hollis666/fo22bm/wmmyt7a9vr7qlwsl?view=doc_embed)

在上面这篇中我们画过的一张图，如果把半同步复制的过程也加进去，那么就会变成：

![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1692363516478-340255ed-f1cb-4cfb-aae1-cf21970d78c3.png#averageHue=%23fdfdfd&clientId=u9fa0fa87-f19f-4&from=paste&height=580&id=u0c2e16b3&originHeight=580&originWidth=1494&originalType=binary&ratio=1&rotation=0&showTitle=false&size=91791&status=done&style=none&taskId=u5b837a3b-b3a2-4cae-b3bc-aa80564b548&title=&width=1494)


## 主从延迟

[✅什么是数据库的主从延迟，如何解决？](https://www.yuque.com/hollis666/fo22bm/weszn2kock8k8wld?view=doc_embed)
## 并行复制

[✅MySQL的并行复制原理](https://www.yuque.com/hollis666/fo22bm/igarxy867n7bgq1q?view=doc_embed)






