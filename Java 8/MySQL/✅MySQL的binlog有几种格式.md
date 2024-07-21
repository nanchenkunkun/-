# 每天记录典型回答

[✅binlog、redolog和undolog区别？](https://www.yuque.com/hollis666/fo22bm/tdlgfm?view=doc_embed)

binlog是MySQL用于记录数据库中的所有DDL语句和DML语句的一种二进制日志。它记录了所有对数据库结构和数据的修改操作，如INSERT、UPDATE和DELETE等。**binlog主要用来对数据库进行数据备份、灾难恢复和数据复制等操作**。binlog的格式分为基于语句的格式和基于行的格式。

MySQL的bin log主要支持三种格式，分别是statement、row以及mixed。MySQL是在5.1.5版本开始支持row的、在5.1.8版本中开始支持mixed。

### statement

当binlog的格式为statement时，binlog 里面记录的就是 SQL 语句的原文，也就是说在数据库中执行的SQL会原封不动的记录到binlog中。

如：
```
# at 12345
# statement: UPDATE employees SET name = "Hollis" WHERE id = 101;
```

这种格式现在用的都比较少了，因为他会导致主从同步的数据不一致问题。

比如说，当我们使用DELETE或者UPDATE的时候，指定了LIMIT，但是并没有使用order by，那么最终这条语句在主库和从库上的执行结果可能是不一样的（即使同一个库上面，你执行多次结果可能也不一样）。

还有就是下面这篇文章中提到的情况了：

[✅为什么MySQL默认使用RR隔离级别？](https://www.yuque.com/hollis666/fo22bm/fx5luearutigdcep?view=doc_embed)


### row

在ROW格式中，bin log会记录每个数据更改的具体行的细节。这意味着二进制日志中的每个条目都会详细列出发生变更的行的内容和修改。

这种格式的好处就是不会导致主从不一致的问题。

但是他的缺点就是可能会要记录更多的内容，比如批量修改，就需要把每条记录的变更都记录下来。

所以，带来的问题就是基于这种格式的binlog，在数据恢复的时候，会需要更长的时间，也会导致磁盘IO和网络IO都比较高。

### mixed

这种其实就是把statement和row结合了，MySQL会根据SQL的情况，自动在row和statement中互相切换选择一个他认为合适的格式进行记录。

但是，在RR下，row和statement都可以生效，但是在RC下，只有row格式才能生效。具体见上面我们贴的那个链接的内容。
