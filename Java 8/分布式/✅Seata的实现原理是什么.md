# 典型回答

Seata是一个阿里开源的分布式事务解决方案（Simple Extensible Autonomous Transaction Architecture），用于在分布式系统中实现分布式事务。它旨在简化分布式事务的开发和管理，帮助解决分布式系统中的数据一致性问题。

因为Seata的开发者坚定地认为：一个分布式事务是有若干个本地事务组成的。所以他们给Seata体系的所有组件定义成了三种，分别是Transaction Coordinator、Transaction Manager和Resource Manager

**Transaction Coordinator(TC)**:  这是一个独立的服务，是一个独立的 JVM 进程，里面不包含任何业务代码，它的主要职责：维护着整个事务的全局状态，负责通知 RM 执行回滚或提交；

**Transaction Manager(TM)**: 在微服务架构中可对应为聚合服务，即将不同的微服务组合起来成一个完成的业务流程，TM 的职责是开启一个全局事务或者提交或回滚一个全局事务；

**Resource Manager(RM)**：RM 在微服务框架中对应具体的某个微服务为事务的分支，RM 的职责是：执行每个事务分支的操作。

看上去好像很难理解？举个例子你就知道了：

![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1691212535940-51d90d22-ad71-4274-a632-1ad27d661aa1.png#averageHue=%23f7efee&clientId=u989134b5-18c5-4&from=paste&height=746&id=u56781373&originHeight=746&originWidth=1034&originalType=binary&ratio=1&rotation=0&showTitle=false&size=550772&status=done&style=none&taskId=u461bc847-4f8e-4da5-b834-758275af7f3&title=&width=1034)

在一个下单事务中，我们有一个聚合的服务，姑且把他叫做TradeCenter吧，他负责接收并处理用户的下单请求，并且下单过程中需要调用订单服务（Order）、库存服务（Stock）及账户服务（Account）进行创建订单、扣减库存及增加积分。

所以TradeCenter担当的就是TM的角色，而Order、Stock及Account三个微服务就是RM的角色。在此之外，还需要一个独立的服务，维护分布式事务的全局状态，他就是TC。

因为TC维护着整个事务的全局状态，负责通知 RM 执行回滚或提交，所以他和TM、RM都是有交互的。并且TM和RM之间也有调用关系。多个RM之间可以是独立的。

上面这个场景中，要想保证分布式事务，就需要Order、Stock及Account三个服务对应的数据库表操作，要么都成功、要么都失败。不能有部分成功、部分失败的情况。

在用了Seata之后，一次分布式事务的大致流程如下（不同的模式略有不同，在介绍具体模式的时候分别展开）：

1、TM在接收到用户的下单请求后，会先调用TC创建一个全局事务，并且从TC获取到他生成的XID。

2、TM开始通过RPC/Restful调用各个RM，调用过程中需要把XID同时传递过去。

![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1691213549843-0ba2065e-cdeb-4341-89f7-3e27c2af14af.png#averageHue=%23fbf5f5&clientId=u66b409cb-b683-4&from=paste&height=793&id=uf09f2992&originHeight=793&originWidth=1025&originalType=binary&ratio=1&rotation=0&showTitle=false&size=537123&status=done&style=none&taskId=uefe2c1d1-2976-492b-b95a-c026c24565d&title=&width=1025)

3、RM通过其接收到的XID,将其所管理的资源且被该调用锁使用到的资源注册为一个事务分支(Branch Transaction)

![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1691213610596-d9084b3b-74a8-4f12-abcf-990717d93060.png#averageHue=%23fbf4f4&clientId=u66b409cb-b683-4&from=paste&height=801&id=u9bb60051&originHeight=801&originWidth=942&originalType=binary&ratio=1&rotation=0&showTitle=false&size=512330&status=done&style=none&taskId=u5e25acf7-f8aa-4089-948b-1779743830a&title=&width=942)

4、当该请求的调用链全部结束时，TM根据本次调用是否有失败的情况，如果所有调用都成功，则决议Commit，如果有超时或者失败，则决议Rollback。

5、TM将事务的决议结果通知TC，TC将协调所有RM进行事务的二阶段动作，该回滚回滚，该提交提交。

![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1691213685250-ac8f2f0b-90d3-4f8f-a12f-892732f7c237.png#averageHue=%23f9f2f2&clientId=u66b409cb-b683-4&from=paste&height=796&id=u61c9d2a2&originHeight=796&originWidth=953&originalType=binary&ratio=1&rotation=0&showTitle=false&size=524812&status=done&style=none&taskId=u7a4b72dd-f2e7-4902-8f2c-bbe7de18a0c&title=&width=953)

这里要求所有的RM都能做到2阶段，第一阶段做事务的预处理，第二阶段做事务的提交或者回滚。具体怎么实现，是否需要自己改代码，这个不同的模式不太一样。


# 扩展知识

## Seata 的四种事务模式

[✅Seata的4种事务模式，各自适合的场景是什么？](https://www.yuque.com/hollis666/fo22bm/cx86tg6tdhmz1dm9?view=doc_embed)


