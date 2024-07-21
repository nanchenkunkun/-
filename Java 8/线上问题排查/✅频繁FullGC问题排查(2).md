### 问题发现

线上兼容系统报警，提示有频繁的FullGC以及GC耗时问题比较严重。

![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1702880427123-d4534019-4b0f-470a-995b-9c2c8b42d2fb.png#averageHue=%23fcfbfb&clientId=u4e0b93ab-3969-4&from=paste&height=402&id=u49113349&originHeight=804&originWidth=724&originalType=binary&ratio=2&rotation=0&showTitle=false&size=408756&status=done&style=none&taskId=ub882407a-30b2-463c-8a94-5a39460b155&title=&width=362)

### 问题定位

在收到FullGC报警之后，登录到内部的监控系统，看一下集群整体的GC情况（如果没有这样的监控系统，可以去机器上查看GC日志）：

![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1702879034460-ec898ac3-abed-4ae2-acf7-13777385b45c.png#averageHue=%23fbfbfb&clientId=u4e0b93ab-3969-4&from=paste&height=298&id=ua0cd6a2d&originHeight=596&originWidth=1170&originalType=binary&ratio=2&rotation=0&showTitle=false&size=466008&status=done&style=none&taskId=u491498e7-0db1-47af-891a-4b0965d6324&title=&width=585)

可以看到，集群的GC次数是3小时内有十几次了，那么去看一下单机的情况。

这里我先去看了一下报警的那台机器，然后有随机挑了几台线上机器去查看，看到一个比较特殊的现象：

那就是并不是所有机器都存在FullGC的情况，有些机器的堆内存的水位甚至还挺低的。但是这里没多想，接着去看堆dump去了。

我们因为内部有进行堆dump以及分析的工具，如果么有的话可以使用jmap或者arthas获取堆dump。然后再使用Java VisualVM、Memory Analyzer Tool等工具进行分析。

我这里是分别对存在FullGC的机器、当前堆内存占用比较高的机器、以及内存占用并不高也没有频繁GC的机器进行了dump。

**之所有多次dump，主要是为了作对比。**

在分析过程中，发现堆内存占用比较高的机器和FullGC比较频繁的机器中，存在着一些比较特殊的现象。

首先是有大对象占了2个多G的内存。

![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1702879386755-26739c8b-7cc8-4b2d-987d-da1dd6940342.png#averageHue=%23f9f3f3&clientId=u4e0b93ab-3969-4&from=paste&height=123&id=u289a8799&originHeight=245&originWidth=1753&originalType=binary&ratio=2&rotation=0&showTitle=false&size=377897&status=done&style=none&taskId=u9d8ec025-10e8-4bdd-8eed-02889502c77&title=&width=876.5)

然后再进一步查看大对象内容：

![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1702879491271-715cd913-fe9b-4a3d-9ea0-13013ebeba1a.png#averageHue=%23f9f8f8&clientId=u4e0b93ab-3969-4&from=paste&height=534&id=u26ef7272&originHeight=1068&originWidth=1751&originalType=binary&ratio=2&rotation=0&showTitle=false&size=1614954&status=done&style=none&taskId=ua9bfd622-e911-4841-aeb6-a679e569195&title=&width=875.5)

发现有一个ArrayList中存放了60多万个CollectionCaseDO对象。

这个CollectionCaseDO对象我就比较熟悉了，是我们自己的业务模型。但是竟然会在内存中加载这么多就很奇怪了。

看到这里我有两个猜测：

1、在一个bean中有一个List<CollectionCaseDO>的成员变量，在代码中会多次向其中add，导致他有这么大的量。<br />2、在代码中有一个地方在做查询的时候没有做好条件过滤及分页，导致数据库查询了大量数据。

第一个猜测很快被我排除了，因为我去全局搜索了代码，并没有发现这种用法。那么就只剩第二种了，接下来排查在哪里出现的这个问题查询。

然后我又想到，线上不是所有机器都有这个现象，只有部分机器，并且通过监控发现，出问题的机器堆内存是逐步增长起来的：

![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1702879718972-49d2feb6-48f6-41d9-b2fa-96edb3449a93.png#averageHue=%23fafaf9&clientId=u4e0b93ab-3969-4&from=paste&height=263&id=u889e3118&originHeight=277&originWidth=786&originalType=binary&ratio=2&rotation=0&showTitle=false&size=162475&status=done&style=none&taskId=u79d70471-e128-4164-98c7-a7b61a05a27&title=&width=745)

于是，根据问题发生的时间点，去查日志。

在查日志的之前，我根据上面的情况，以及dump的信息，进一步定位到这个问题应该和我们的一个查询接口有关。

![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1702879979493-fc330314-721b-4f40-bc74-a6e3af642b8a.png#averageHue=%23f9f8f8&clientId=u4e0b93ab-3969-4&from=paste&height=463&id=uc5836f03&originHeight=926&originWidth=1254&originalType=binary&ratio=2&rotation=0&showTitle=false&size=1064069&status=done&style=none&taskId=uc57182b3-12c3-4f02-8e12-2ba35454849&title=&width=627)

于是通过这个接口的关键日志进行查询，还真的让我查到了端倪。

![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1702880468284-890e1eb5-f25e-4c21-9843-4001383d0a6c.png#averageHue=%23f4f4f4&clientId=u4e0b93ab-3969-4&from=paste&height=234&id=u695aae90&originHeight=468&originWidth=3312&originalType=binary&ratio=2&rotation=0&showTitle=false&size=1163160&status=done&style=none&taskId=u37292402-ea82-4790-8e69-ae31407e624&title=&width=1656)

在内存两次增长的时间点，刚好有两条特殊的日志。

正常的查询，参数中是要带一个查询的id或者当前的坐席的，如：

![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1702880138834-76381c2f-b580-4e4b-b80e-a76d27fe98a8.png#averageHue=%23f3f2f2&clientId=u4e0b93ab-3969-4&from=paste&height=60&id=u46af34ee&originHeight=120&originWidth=1855&originalType=binary&ratio=2&rotation=0&showTitle=false&size=164741&status=done&style=none&taskId=ue523585c-e548-4abc-9ff4-ac3cfe5f294&title=&width=927.5)

但是上面的问题查询没有带这个ID，那么看了一下代码，这是一个根据ID查询详情的接口，但是发现同事的代码中并没有对这个caseId做非空校验，然后在用户未传递caseId的时候，用了个queryList，就会把所有的案件都查出来放到List中。。。。

![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1702880259932-ce1a3b43-accd-45c5-8c6b-a749c0cbb969.png#averageHue=%23302a29&clientId=u4e0b93ab-3969-4&from=paste&height=228&id=u45d2522e&originHeight=333&originWidth=1031&originalType=binary&ratio=2&rotation=0&showTitle=false&size=314265&status=done&style=none&taskId=uf253fdb6-9eed-4d54-81c6-758df22dcb2&title=&width=704.5)

截止到这里，后端的问题基本上定位到了，因为没有传ID，并没有做校验，导致一次查询把所有数据都查出来，放到了List中，然后导致大对象被放到老年代占用了大量空间，因为有多次查询，导致FullGC多次。

后面为啥没传caseId就让前端检查了一下，发现是前端的bug，但是也确实是后端没做好校验导致的。
### 问题解决

问题定位了，解决很简单了，就对caseID做一下非空校验就行了。如果发现没传，直接报错返回即可。
