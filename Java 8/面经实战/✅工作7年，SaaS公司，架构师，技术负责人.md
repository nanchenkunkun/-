# 面试者背景

人脸识别SaaS公司、7年工作经验，云平台核心业务开发，架构师，负责了项目0到1搭建

:::warning
支持1w并发是怎么做的？redis缓存、分库分表、future<br />如果让你支持10w并发，还能做哪些事情？消息队列解耦、打批处理、集群部署、<br />缓存用在什么场景？预热。用户的数据变了怎么感知？<br />一个租户特别大，热点问题怎么解决？缓存的热点问题怎么办？热key拆分、二级缓存。<br />近端缓存是什么？相比于本地缓存和分布式缓存有什么优缺点？一致性问题如何解决？<br />缓存和数据库的一致性该如何解决？更新数据库、删除缓存、延迟双删？为什么删除缓存而不是更新缓存？除了延迟双删，还有其他保证一致性方案吗？监听Bin log处理<br />如何实现一个本地缓存呢？map、guava，LRU是什么？LFU如何实现的？为啥要做缓存淘汰？<br />Redis相比memcached优势？<br />Redis的线程模型是怎样的？多路复用如何实现的？redis所有模块都是单线程的吗？为啥6.0要引入多线程？哪个模块是单线程的？多线程有什么缺点？<br />解决过OOM问题，介绍下过程。如何发现的？dump，如何解决的？<br />堆内存大小多大？8G，垃圾回收器用的是哪个？为什么不用CMS？CMS能降低STW？<br />G1 有哪些特点？garbage first，REGION是什么？STW时长如何预测？小内存为啥不适合用G1呢？<br />FullGC的触发条件是什么？老年代满了、空间分配担保失败、<br />什么是空间分配担保？<br />Dubbo一次服务调用过程是怎样的？<br />如果让你实现一个dubbo，会考虑用那些技术解决哪些问题？<br />Zk的选主过程是怎么样的？zk脑裂的问题？<br />Zk可以用来做什么？用在哪些场景中？注册中心、分布式锁、分布式ID、master选举<br />Seata框架用了干什么？分布式事务有哪些方案？TCC详细介绍下。Commit的时候失败了怎么办？<br />架构设计的时候，首先考虑的三个要素是什么？可扩展性、高内聚低耦合、可用性
:::
# 题目解析

> 支持1w并发是怎么做的？redis缓存、分库分表、future
> 如果让你支持10w并发，还能做哪些事情？消息队列解耦、打批处理、集群部署、


因为简历中他提到负责的业务支持了1W的并发， 所以面试的时候会重点关注，如何抗高并发。

接口性能优化方案：[https://www.yuque.com/hollis666/fo22bm/ifuuagaqo3yd8vqb](https://www.yuque.com/hollis666/fo22bm/ifuuagaqo3yd8vqb)<br />高并发思路：[https://www.yuque.com/hollis666/fo22bm/gfgqpua8gu3oag44](https://www.yuque.com/hollis666/fo22bm/gfgqpua8gu3oag44)

> 缓存用在什么场景？预热。用户的数据变了怎么感知？
> 一个租户特别大，热点问题怎么解决？缓存的热点问题怎么办？热key拆分、二级缓存。
> 近端缓存是什么？相比于本地缓存和分布式缓存有什么优缺点？一致性问题如何解决？
> 缓存和数据库的一致性该如何解决？更新数据库、删除缓存、延迟双删？为什么删除缓存而不是更新缓存？除了延迟双删，还有其他保证一致性方案吗？监听Bin log处理
> 如何实现一个本地缓存呢？map、guava，LRU是什么？LFU如何实现的？为啥要做缓存淘汰？


热点问题：[https://www.yuque.com/hollis666/fo22bm/lysd3t](https://www.yuque.com/hollis666/fo22bm/lysd3t)<br />缓存一致性问题：[https://www.yuque.com/hollis666/fo22bm/tmcgo0](https://www.yuque.com/hollis666/fo22bm/tmcgo0)<br />本地缓存：[https://www.yuque.com/hollis666/fo22bm/iy5loh8gvzlqolxo](https://www.yuque.com/hollis666/fo22bm/iy5loh8gvzlqolxo)<br />LRU:[https://www.yuque.com/hollis666/fo22bm/gl3fivks74z4d10e](https://www.yuque.com/hollis666/fo22bm/gl3fivks74z4d10e)<br />LRU实现：[https://www.yuque.com/hollis666/fo22bm/qk8y0w5wa0vpcyzp](https://www.yuque.com/hollis666/fo22bm/qk8y0w5wa0vpcyzp)


> Redis相比memcached优势？
> Redis的线程模型是怎样的？多路复用如何实现的？redis所有模块都是单线程的吗？为啥6.0要引入多线程？哪个模块是单线程的？多线程有什么缺点？


Redis&Memcached:[https://www.yuque.com/hollis666/fo22bm/ink6os3bm19gafx7](https://www.yuque.com/hollis666/fo22bm/ink6os3bm19gafx7)<br />Redis线程模型：[https://www.yuque.com/hollis666/fo22bm/lrhzxqbur0eywnfu](https://www.yuque.com/hollis666/fo22bm/lrhzxqbur0eywnfu)<br />Redis多线程：[https://www.yuque.com/hollis666/fo22bm/zfpgxa93bmn9png9](https://www.yuque.com/hollis666/fo22bm/zfpgxa93bmn9png9)

> 解决过OOM问题，介绍下过程。如何发现的？dump，如何解决的？


OOM问题排查：[https://www.yuque.com/hollis666/fo22bm/vdnaxh](https://www.yuque.com/hollis666/fo22bm/vdnaxh)

> 堆内存大小多大？8G，垃圾回收器用的是哪个？为什么不用CMS？CMS能降低STW？
> G1 有哪些特点？garbage first，REGION是什么？STW时长如何预测？小内存为啥不适合用G1呢？
> FullGC的触发条件是什么？老年代满了、空间分配担保失败、
> 什么是空间分配担保？


垃圾回收器区别：[https://www.yuque.com/hollis666/fo22bm/nqra2l](https://www.yuque.com/hollis666/fo22bm/nqra2l)<br />G1：[https://www.yuque.com/hollis666/fo22bm/hgquufzt6m9psmtp](https://www.yuque.com/hollis666/fo22bm/hgquufzt6m9psmtp)<br />FullGC触发条件&分配担保：[https://www.yuque.com/hollis666/fo22bm/akr0h4yk44r57g5x](https://www.yuque.com/hollis666/fo22bm/akr0h4yk44r57g5x)

> Dubbo一次服务调用过程是怎样的？
> 如果让你实现一个dubbo，会考虑用那些技术解决哪些问题？


Dubbo调用过程：[https://www.yuque.com/hollis666/fo22bm/io1pkwin43mkwaup](https://www.yuque.com/hollis666/fo22bm/io1pkwin43mkwaup)<br />如何实现一个RPC框架：[https://www.yuque.com/hollis666/fo22bm/ve4eci8re6blimh9](https://www.yuque.com/hollis666/fo22bm/ve4eci8re6blimh9)

> Zk的选主过程是怎么样的？zk脑裂的问题？
> Zk可以用来做什么？用在哪些场景中？注册中心、分布式锁、分布式ID、master选举


zk选主：[https://www.yuque.com/hollis666/fo22bm/tsfqf463g4mbh41k](https://www.yuque.com/hollis666/fo22bm/tsfqf463g4mbh41k)<br />zk脑裂：[https://www.yuque.com/hollis666/fo22bm/xuxwgui3f8ti2a0y](https://www.yuque.com/hollis666/fo22bm/xuxwgui3f8ti2a0y)<br />zk使用场景：[https://www.yuque.com/hollis666/fo22bm/bxldoz3kvfpdsv1g](https://www.yuque.com/hollis666/fo22bm/bxldoz3kvfpdsv1g)

> Seata框架用了干什么？分布式事务有哪些方案？TCC详细介绍下。Commit的时候失败了怎么办？


分布式事务：[https://www.yuque.com/hollis666/fo22bm/yr0lu6](https://www.yuque.com/hollis666/fo22bm/yr0lu6)<br />TCC：[https://www.yuque.com/hollis666/fo22bm/xhvbak3ouy6xqiml](https://www.yuque.com/hollis666/fo22bm/xhvbak3ouy6xqiml)

> 架构设计的时候，首先考虑的三个要素是什么？可扩展性、高内聚低耦合、可用性


架构设计原则：[https://www.yuque.com/hollis666/fo22bm/uedwqv2xttnp2ze4](https://www.yuque.com/hollis666/fo22bm/uedwqv2xttnp2ze4)<br />[https://www.yuque.com/hollis666/fo22bm/impo4wc9yzn3mmu2](https://www.yuque.com/hollis666/fo22bm/impo4wc9yzn3mmu2)
