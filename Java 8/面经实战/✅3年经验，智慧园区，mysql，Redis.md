# 面试者背景

:::warning
3年经验，2家公司（新能源，to B业务），智慧园区-智慧物业-能耗管理<br />能耗管理业务介绍一下，智能控制、能耗统计、性能优化、数据准确度提升、稳定性，<br />性能优化、数据准确度、稳定性，分别做了哪些事情？换成adb、业务宽表拆解，<br />数据库切换如何保证的稳定？代码中多数据源的管理是怎么做的？<br />为啥adb查询快知道么？<br />项目中有难度的地方有吗？<br />Netty相比原生的IO模型有啥优势？封装NIO、粘包/拆包、IO多路复用、reactor模型<br />为啥Netty不基于AIO做呢。NIO用的是堆内内存还是堆外内存？directByteBuffer为啥用堆外内存？零拷贝，堆外内存溢出如何排查，dump对堆外内存有用吗？netty适合用在什么场景中？im、通信、<br />Innodb的索引介绍下？B+树。Innodb中的B+树长什么样？只有叶子节点之间有双向指针吗？叶子节点的没条记录之间是如何关联的？<br />Innodb的索引结构和myisam有区别吗？myisam有聚簇索引吗？<br />创建索引的时候需要考虑哪些问题？使用频率、查询条件、前缀长度、<br />什么时候会考虑用联合索引？如果只有一个条件查就没有建联合索引的必要了么？<br />索引是越多越好么？a,b联合索引，在写SQL的时候where a = xx and b=xx 的顺序有关系吗？<br />Mysql优化器都会做哪些方面的优化？索引选择。<br />如果优化器选错了索引怎么办？force index<br />有哪些情况会导致慢SQL？没走索引、索引失效、数据量大、多表join、<br />有没有可能走了索引还是很慢？如何确认一个SQL有没有走索引？type、extra，<br />using index是什么意思？using where一定没走索引吗？<br />Mysql的主从原理介绍一下。主从延迟如何排查？<br />Mysql中用like能走索引吗？‘abc%’可以，‘%abc’这种有办法优化吗？<br />Mysql了解过8.0吗？redis有一批key瞬时过期了，会导致什么问题？怎么解决呢？<br />redis有一批key瞬时过期了，除了影响数据库，还有其他影响吗？读写会变慢吗？单线程。<br />Key过期会立即删除吗？redis的big key问题？很大是多大？会有什么问题？如何解决呢？怎么拆？按照日期<br />分库分表介绍下？单表数据量大导致查询慢，除了考虑分表之外，还有哪些方案？归档（冷热分离）、缓存、<br />数据量的话，你觉得做分库分表一定是一个好的方案吗？
:::
# 题目解析


> Netty相比原生的IO模型有啥优势？封装NIO、粘包/拆包、IO多路复用、reactor模型
> 为啥Netty不基于AIO做呢。NIO用的是堆内内存还是堆外内存？directByteBuffer为啥用堆外内存？零拷贝，堆外内存溢出如何排查，dump对堆外内存有用吗？netty适合用在什么场景中？im、通信、


[https://www.yuque.com/hollis666/fo22bm/itxx9r](https://www.yuque.com/hollis666/fo22bm/itxx9r)

[https://www.yuque.com/hollis666/fo22bm/roit5c9y04z6fqae](https://www.yuque.com/hollis666/fo22bm/roit5c9y04z6fqae)

> Innodb的索引介绍下？B+树。Innodb中的B+树长什么样？只有叶子节点之间有双向指针吗？叶子节点的没条记录之间是如何关联的？
> Innodb的索引结构和myisam有区别吗？myisam有聚簇索引吗？


[https://www.yuque.com/hollis666/fo22bm/uh3cy1](https://www.yuque.com/hollis666/fo22bm/uh3cy1)

[https://www.yuque.com/hollis666/fo22bm/mcl4sn8mcutieesz](https://www.yuque.com/hollis666/fo22bm/mcl4sn8mcutieesz)

> 创建索引的时候需要考虑哪些问题？使用频率、查询条件、前缀长度、
> 什么时候会考虑用联合索引？如果只有一个条件查就没有建联合索引的必要了么？
> 索引是越多越好么？


[https://www.yuque.com/hollis666/fo22bm/ygxb9f](https://www.yuque.com/hollis666/fo22bm/ygxb9f)

> a,b联合索引，在写SQL的时候where a = xx and b=xx 的顺序有关系吗？
> Mysql优化器都会做哪些方面的优化？索引选择。
> 如果优化器选错了索引怎么办？force index


[https://www.yuque.com/hollis666/fo22bm/nwm3ry85o8l0gega](https://www.yuque.com/hollis666/fo22bm/nwm3ry85o8l0gega)

[https://www.yuque.com/hollis666/fo22bm/st7he2np7e9trg9k](https://www.yuque.com/hollis666/fo22bm/st7he2np7e9trg9k)


> 有哪些情况会导致慢SQL？没走索引、索引失效、数据量大、多表join、
> 有没有可能走了索引还是很慢？


[https://www.yuque.com/hollis666/fo22bm/zhfa5g](https://www.yuque.com/hollis666/fo22bm/zhfa5g)

[https://www.yuque.com/hollis666/fo22bm/st7he2np7e9trg9k](https://www.yuque.com/hollis666/fo22bm/st7he2np7e9trg9k)

> 如何确认一个SQL有没有走索引？type、extra，
> using index是什么意思？using where一定没走索引吗？



[https://www.yuque.com/hollis666/fo22bm/fho0bamf4qpcril5](https://www.yuque.com/hollis666/fo22bm/fho0bamf4qpcril5)

> Mysql的主从原理介绍一下。主从延迟如何排查？


[https://www.yuque.com/hollis666/fo22bm/hoi4ql](https://www.yuque.com/hollis666/fo22bm/hoi4ql)

> Mysql中用like能走索引吗？‘abc%’可以，‘%abc’这种有办法优化吗？


[https://www.yuque.com/hollis666/fo22bm/zrt2y30mhdgiremc](https://www.yuque.com/hollis666/fo22bm/zrt2y30mhdgiremc)

> Mysql了解过8.0吗？


[https://www.yuque.com/hollis666/fo22bm/uxdmro](https://www.yuque.com/hollis666/fo22bm/uxdmro)

> redis有一批key瞬时过期了，会导致什么问题？怎么解决呢？
> redis有一批key瞬时过期了，除了影响数据库，还有其他影响吗？读写会变慢吗？单线程。


[https://www.yuque.com/hollis666/fo22bm/abfis3](https://www.yuque.com/hollis666/fo22bm/abfis3)

[https://www.yuque.com/hollis666/fo22bm/ry7g0lxzynxmneq7](https://www.yuque.com/hollis666/fo22bm/ry7g0lxzynxmneq7)


> Key过期会立即删除吗？


[https://www.yuque.com/hollis666/fo22bm/ds8qgg4zmt7l2kvp](https://www.yuque.com/hollis666/fo22bm/ds8qgg4zmt7l2kvp)

> redis的big key问题？很大是多大？会有什么问题？如何解决呢？怎么拆？按照日期


[https://www.yuque.com/hollis666/fo22bm/qiqc1r6r3catcev9](https://www.yuque.com/hollis666/fo22bm/qiqc1r6r3catcev9)


> 分库分表介绍下？单表数据量大导致查询慢，除了考虑分表之外，还有哪些方案？归档（冷热分离）、缓存、
> 数据量的话，你觉得做分库分表一定是一个好的方案吗？


[https://www.yuque.com/hollis666/fo22bm/dk6tpttlf2aex9ap](https://www.yuque.com/hollis666/fo22bm/dk6tpttlf2aex9ap)
