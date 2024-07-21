# 典型回答

MySQL的组提交（Group Commit）是一项优化技术，用于提高数据库系统的性能和事务处理的效率。它通过将多个事务的提交操作合并成一个批处理操作来减少磁盘IO和锁定开销，从而加速事务的处理。

我们的数据库需要不断地执行很多次数据变更，并且每次变更都需要把数据持久化下来，以方便进行崩溃恢复及主从同步、还有回滚等，这就涉及到binlog、redolog以及undolog的写入。

而频繁的文件写入需要会触发频繁磁盘写入操作，为了减少提交操作的开销，MySQL引入了组提交技术，就是**将多个事务的提交操作可以合并成一个批处理操作，以减少磁盘IO次数。**这个批处理操作将包含多个事务的修改，并一次性写入二进制日志。

通过以下命令可以查看关于组提交的配置：

```java
mysql> show variables like '%group_commit%';                                                                                                                                         
+-----------------------------------------+-------+                                                                                                                                  
| Variable_name                           | Value |                                                                                                                                  
+-----------------------------------------+-------+                                                                                                                                  
| binlog_group_commit_sync_delay          | 0     |                                                                                                                                  
| binlog_group_commit_sync_no_delay_count | 0     |                                                                                                                                  
+-----------------------------------------+-------+                                                                                                                                  
2 rows in set (0.00 sec) 
```

- binlog_group_commit_sync_delay
   - 延迟多长时间再通过fsync进行刷盘，把数据持久化
- binlog_group_commit_sync_no_delay_count
   - 累积多少次操作再通过fsync进行刷盘，把数据持久化

注意，这两个条件是或的关系，只要满足一个，就会触发提交动作。


# 扩展知识

## 有了组提交后的2阶段

[✅什么是事务的2阶段提交？](https://www.yuque.com/hollis666/fo22bm/geuks1bbiwd39h1r?view=doc_embed)

在有了组提交之后，2阶段就会有一些变化，因为日志的刷盘过程会因为组提交而需要等待，所以会变成这样：

![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1692967827189-6aab9276-25ee-4d75-8dea-0506e886387e.png#averageHue=%23fcfbf7&clientId=u93673464-99ad-4&from=paste&height=760&id=u8356d233&originHeight=760&originWidth=815&originalType=binary&ratio=1&rotation=0&showTitle=false&size=36592&status=done&style=none&taskId=u4d19f437-3065-40b1-b56a-b10a9c8c227&title=&width=815)

这里面write 和 fsync 是与文件系统和磁盘IO相关的两个不同的操作。

write 操作将数据写入文件的缓冲区，这意味着 write 操作完成后，并不一定立即将数据持久化到磁盘上，而是将数据暂时存储在内存中。

fsync 用于强制将文件的修改持久化到磁盘上。它通常与 write 配合使用，以确保文件的修改在 fsync 操作完成后被写入磁盘。

所以，用于将缓冲区内容持久化到磁盘的fsync这一步，被延迟了。他会等一个组中多个事务都处于Prepare阶段后，然后进行一次组提交，即把日志持久化到磁盘中。
