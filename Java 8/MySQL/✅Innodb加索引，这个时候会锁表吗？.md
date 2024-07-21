# 典型回答

在 MySQL 5.6 之前，InnoDB 索引构建期间会对表进行排它锁定，这意味着在索引构建期间，其他会话不能读取或修改表中的任何数据。这将导致长时间阻塞和性能问题。

**自 MySQL 5.6 开始，InnoDB 使用一种称为“Online DDL”的技术，允许在不阻塞其他会话的情况下创建或删除索引**。Online DDL 针对不同的操作，有多种实现方式，包括COPY，INSTANT以及INPLACE

因为DDL有很多种操作，比如创建索引、增加字段、增加外键等，所以不同的操作支持的类型也不一样，具体支持方式可以在mysql官方可以看到（[https://dev.mysql.com/doc/refman/5.7/en/innodb-online-ddl-operations.html](https://dev.mysql.com/doc/refman/5.7/en/innodb-online-ddl-operations.html) ），拿索引创建举例：

![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1699088229537-758c4d4a-4363-4d1b-9581-ce8f532d0141.png#averageHue=%23f6f6f5&clientId=u2b1c6859-fd0e-4&from=paste&height=330&id=u1164123d&originHeight=413&originWidth=1762&originalType=binary&ratio=1.25&rotation=0&showTitle=false&size=52711&status=done&style=none&taskId=udff64c72-4432-428b-8d2f-a5cbc251f67&title=&width=1409.6)

从上表中可以看到，当我们创建、删除或者重命名一个索引的时候，是会用到in place的模式。

需要注意的是，虽然Online DDL 可以减少锁定时间和对性能的影响，但在索引构建期间仍然可能出现锁定和阻塞。例如，在添加索引时，如果表中有许多未提交的事务，则需要等待这些事务提交后才能开始索引构建。**因此，建议在非高峰期进行此类操作，以避免影响用户的正常使用。**在进行任何DDL操作之前，最好进行充分的测试和规划，并且确保有备份和回滚计划，以防意外情况的发生。

# 扩展知识

## Online DDL原理

[✅什么是OnlineDDL](https://www.yuque.com/hollis666/fo22bm/lwxtmggon7ir4zzz?view=doc_embed)


