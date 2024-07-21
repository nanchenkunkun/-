# 面试者背景

5年经验，做过：低代码、组件、k8s开发，流计算引擎、配置中心、流程编排、RPA

## 面试过程

:::warning
为什么不用MQ，而是用Redis造轮子？消息丢的问题怎么解决？<br />Redis的集群模式是怎么样的？挂了就挂了？你了解哪些集群模式？<br />Redis的Stream底层实现原理与数据结构。唯一消息ID和内容，消费者。<br />Redis的Stream的消息怎么避免重复消费？XACK、XPENDING、XREADGROUP<br />Redis的Stream有重平衡的机制吗？<br />为啥要造轮子搞个配置中心？AP的还是CP的？<br />配置数据发生变化，客户端是如何感知到的？长轮询。<br />除了长轮询还有其他的方案吗？区别是啥？实时性、socket.io是长连接吗？<br />有高并发经验吗？高可用有保障吗？做了哪些事情保证高可用？集群架构、<br />SLA标准是什么？<br />流量突增100倍，如何解决？流量来源分析、限流（令牌桶）、<br />介绍下令牌桶的原理，为啥令牌桶可以应对突发流量？漏桶如果出现突发流量会怎么样<br />压测怎么做的？要做一次压测的步骤怎么样的？<br />压测怎么来探测系统水位？
:::
# 题目解析

> 为什么不用MQ，而是用Redis造轮子？消息丢的问题怎么解决？


造轮子，一定要能说清楚：<br />1、为什么造轮子<br />2、已有的轮子为啥不能用<br />3、你造的轮子有什么好处<br />4、你造的轮子缺点是什么<br />5、已有的轮子中的XX特性让你实现你怎么做

> Redis的集群模式是怎么样的？挂了就挂了？你了解哪些集群模式？
> Redis的Stream底层实现原理与数据结构。唯一消息ID和内容，消费者。
> Redis的Stream的消息怎么避免重复消费？XACK、XPENDING、XREADGROUP
> Redis的Stream有重平衡的机制吗？


[✅介绍一下Redis的集群模式？](https://www.yuque.com/hollis666/fo22bm/namhuv165lorwudw?view=doc_embed)

[✅Redis 5.0中的 Stream是什么？](https://www.yuque.com/hollis666/fo22bm/qehw9x86oxl0r0sc?view=doc_embed)

> 为啥要造轮子搞个配置中心？AP的还是CP的？
> 配置数据发生变化，客户端是如何感知到的？长轮询。
> 除了长轮询还有其他的方案吗？区别是啥？实时性、socket.io是长连接吗？


造了个配置中心的轮子，那么就需要能讲清楚，你的配置中心是怎么做的，一些方案是如何做的选择。

[✅Nacos是AP的还是CP的？](https://www.yuque.com/hollis666/fo22bm/ed9gu0mf5q4u1pw6?view=doc_embed)

[✅Nacos如何实现的配置变化客户端可以感知到？](https://www.yuque.com/hollis666/fo22bm/icbk1rndq13ku07o?view=doc_embed)

> 有高并发经验吗？高可用有保障吗？做了哪些事情保证高可用？集群架构、


[✅如何设计一个高可用架构？](https://www.yuque.com/hollis666/fo22bm/vyg778x53xe6elwe?view=doc_embed)

> 流量突增100倍，如何解决？


[✅如果你的业务量突然提升100倍QPS你会怎么做？](https://www.yuque.com/hollis666/fo22bm/vmymwg4epv4o24lc?view=doc_embed)

> 介绍下令牌桶的原理，为啥令牌桶可以应对突发流量？漏桶如果出现突发流量会怎么样


[✅什么是限流？常见的限流算法有哪些？](https://www.yuque.com/hollis666/fo22bm/aw1zho?view=doc_embed)

> 压测怎么做的？要做一次压测的步骤怎么样的？
> 压测怎么来探测系统水位？


[✅什么是压测，怎么做压测？](https://www.yuque.com/hollis666/fo22bm/wrzi8qgk7ridgslp?view=doc_embed)

