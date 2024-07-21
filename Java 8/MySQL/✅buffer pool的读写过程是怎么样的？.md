# 典型回答

MySQL的Buffer Pool是一个内存区域，用于缓存数据页，从而提高查询性能。读写过程涉及到数据的从磁盘到内存的读取，以及在内存中的修改和写回磁盘。

[✅什么是buffer pool？](https://www.yuque.com/hollis666/fo22bm/cskzcn42f9dggat0?view=doc_embed)

### 读过程

当我们在MySQL执行一个查询请求的时候，他的过程是这样的：

1. MySQL首先检查Buffer Pool中是否存在本次查询的数据。如果数据在Buffer Pool中，就直接返回结果。
2. 如果数据不在Buffer Pool中，MySQL会从磁盘读取数据。
3. 读取的数据页被放入Buffer Pool，同时MySQL会返回请求的数据给应用程序。

读的过程比较简单的，而Buffer Pool的写的过程就有点复杂了

### 写过程

当我们执行一次更新语句，如INSERT、UPDATE或DELETE等时，会进行以下过程

1. 当应用程序执行写操作时，MySQL首先将要修改的数据页加载到Buffer Pool中。
2. 在Buffer Pool中，对数据页进行修改，以满足写请求。这些修改只在内存中进行，不会立即写回磁盘。
3. 如果Buffer Pool中的数据页被修改过，MySQL会将这个页标记为“脏页”（Dirty Page）。
4. 脏页被写回磁盘，此时写入操作完成，数据持久化。

但是需要注意的是，脏页写回磁盘是由一个后台线程进行的，在MySQL服务器空闲或负载较低时，InnoDB会进行脏页刷盘，以减少对用户线程的影响，降低对性能的影响。（[https://dev.mysql.com/doc/refman/8.0/en/innodb-buffer-pool-flushing.html](https://dev.mysql.com/doc/refman/8.0/en/innodb-buffer-pool-flushing.html) ）

![](https://cdn.nlark.com/yuque/0/2023/png/5378072/1692360052478-7a123e12-d590-438f-9abd-c968f0b7b2bb.png#averageHue=%23fdfdfd&from=url&id=NUgoT&originHeight=649&originWidth=1273&originalType=binary&ratio=1.5&rotation=0&showTitle=false&status=done&style=none&title=)（图源自：[✅InnoDB的一次更新事务是怎么实现的？](https://www.yuque.com/hollis666/fo22bm/wmmyt7a9vr7qlwsl?view=doc_embed)）

当脏页的百分比达到innodb_max_dirty_pages_pct_lwm变量定义的低水位标记时，将启动缓冲池刷新。缓冲池页的默认低水位标记为10%。innodb_max_dirty_pages_pct_lwm值设为0会禁用这种提前刷新行为。

InnoDB还使用了一种适应性刷新算法，根据redo log的生成速度和当前的刷新率动态调整刷新速度。其目的是通过确保刷新活动与当前工作负载保持同步，来平滑整体性能。

当然，我们也可以手动触发脏页的刷新到磁盘，例如通过执行SET GLOBAL innodb_buffer_pool_dump_now=ON 来进行一次脏页刷新。

还有一种情况，就是在MySQL服务器正常关闭或重启时，所有的脏页都会被刷新到磁盘。这样才能保证数据可以持久化下来。


