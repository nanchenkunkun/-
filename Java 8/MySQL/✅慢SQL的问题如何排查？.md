# 典型回答
慢查询是指数据库中查询时间超过指定阈值的SQL，这个阈值根据不同的业务来说一般是不一样的，在我们内部，这个阈值是1s，也就是说，如果一条SQL执行超过1秒钟，就认为是一个慢SQL。

慢SQL的问题排查一般分为几个步骤。

### 发现问题
一般来说，慢SQL的问题，是比较容易能够发现的。首先如果有很成熟的监控体系的话，会把慢SQL进行统计，然后以报警的形式推送出来。

如果用了一些数据库的中间件，他们也会有慢SQL的日志，如TDDL：

```java
Cause: ERR-CODE: [TDDL-4202][ERR_SQL_QUERY_TIMEOUT] Slow query leads to a timeout exception, please contact DBA to check slow sql. SocketTimout:12000 ms, 
```

如果中间件也没用，那么数据库自己也是可以配置慢SQL日志的，配置方式如下：

1、找到MySQL的配置文件 my.cnf（或 my.ini，取决于操作系统），通常位于MySQL安装目录下的 etc 或 conf 文件夹。<br />2、在配置文件中启用慢查询日志：找到或添加以下配置项，并取消注释（如果有注释），确保以下行存在或添加到配置文件中：

```java
slow_query_log = 1
slow_query_log_file = /path/to/slow-query.log
long_query_time = 1
```

3、重启MySQL服务：保存配置文件并重新启动MySQL服务，使配置生效。

配置生效后，如果有SQL的执行时长超过long_query_time 配置的时间阈值，那么就会打印慢SQL日志

4、查看慢查询日志：使用文本编辑器打开慢查询日志文件，路径为在配置文件中指定的路径。例如，在Linux上可以使用以下命令：

```java
vim /path/to/slow-query.log
```

如果有慢SQL，内容如下：

```java
# Time: 2023-06-04T12:00:00.123456Z
# User@Host: hollis[192.168.0.1]:3306
# Query_time: 2.345678  Lock_time: 0.012345 Rows_sent: 10  Rows_examined: 100
SET timestamp=1650000000;
SELECT * FROM orders WHERE status = 'pending' ORDER BY gmt_created DESC;
```

### 定位问题

在如上的各种监控、报警以及日志中，我们就可以找到对应的慢SQL的具体SQL了，然后就可以进一步分析为什么这个SQL是慢SQL了，主要就是排查一下他慢在哪里。

一般来说，导致一个SQL慢的常见原因有以下几个：

1、没有索引<br />2、用错索引（没有遵守最左前缀、索引区分度不高）<br />3、查询字段太多<br />4、回表次数多<br />5、多表join<br />6、深度分页<br />7、...其他...

详见：

[✅如何进行SQL调优？](https://www.yuque.com/hollis666/fo22bm/mgpczmz7la99dkft?view=doc_embed)


一般来说，大多数情况，是可以通过执行计划分析出一条SQL的慢的原因的，大部分来说，主要是索引的问题和join的问题了。

关于执行计划的分析，索引失效等可以参考下面几篇，介绍的很详细了：

[✅SQL执行计划分析的时候，要关注哪些信息？](https://www.yuque.com/hollis666/fo22bm/fho0bamf4qpcril5?view=doc_embed)

[✅索引失效的问题如何排查？](https://www.yuque.com/hollis666/fo22bm/gux80i?view=doc_embed)

### 解决问题

在定位到问题之后，就是解决了

其实最难的不是解决问题，而是定位问题，因为一旦一个问题被定位到了，解决起来都比较容易。缺索引就加索引，join太多就拆分就好了。这里就不展开说了。

下面是一个慢SQL排查的实际案例，和本文的思路是一样的：

[✅慢SQL问题排查](https://www.yuque.com/hollis666/fo22bm/dxmpt2?view=doc_embed)


