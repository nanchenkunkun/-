### 问题发现

线上兼容系统报警，提示有频繁的FullGC以及GC耗时问题比较严重。

![image.png](https://cdn.nlark.com/yuque/0/2024/png/5378072/1705373094495-ad9083b9-6d90-4624-ae55-fac90ff24fe8.png#averageHue=%23fdfdfd&clientId=u3406b14e-ab63-4&from=paste&height=426&id=u64da7407&originHeight=852&originWidth=746&originalType=binary&ratio=2&rotation=0&showTitle=false&size=395044&status=done&style=none&taskId=uf4f051af-fb3d-48c8-b982-85e21204874&title=&width=373)
### 问题定位

在收到FullGC报警之后，登录到内部的监控系统，看一下集群整体的GC情况（如果没有这样的监控系统，可以去机器上查看GC日志）：

![image.png](https://cdn.nlark.com/yuque/0/2024/png/5378072/1705373148732-fd807c4f-f310-4f4f-94db-4b04eb839378.png#averageHue=%23fbfbfa&clientId=u3406b14e-ab63-4&from=paste&height=299&id=uf5a2926b&originHeight=598&originWidth=1253&originalType=binary&ratio=2&rotation=0&showTitle=false&size=395437&status=done&style=none&taskId=u6e68a333-b875-4dda-8400-69063ab14a9&title=&width=626.5)

从图中可以看到，从大概9:03分开始，对内存的老年代就一直在涨，并且在9:13的时候发生了很多次fullGC。

这时候，第一时间去把堆dump下来了，我先在GC前做了一次Dump，然后再做了一次GC，然后做了一次Dump，发现GC前的Dump中很多String占用了比较多的内存，但是GC后就都被回收了。

然后去看这些字符串都是啥，但是很遗憾，没看出什么特别有价值的东西，都是一些游离的字符串（一方面通过堆dump发现他们不可达，另外发现FullGC后就直接回收了）

这时候就在分析，为啥会发生这样的情况。于是我怀疑，可能是因为在9:00-9:20这段时间，请求量比较大，创建了很多对象分配到年轻代，但是年轻代不够了，就被分配到老年代。然后老年代一直在GC，但是因为同时又有大量的操作导致对象也在不断往老年代去。所以看上去老年代在9:13分左右的GC效果并不明显。

为了验证这个猜想，同时看了下年轻代的情况，Eden区没啥明显变化，但是Survivor区的增长比较大：

![image.png](https://cdn.nlark.com/yuque/0/2024/png/5378072/1705373223501-5f649db3-2335-49fe-a4a4-2958f522ef25.png#averageHue=%23fbfbfb&clientId=u3406b14e-ab63-4&from=paste&height=168&id=Vedni&originHeight=336&originWidth=791&originalType=binary&ratio=2&rotation=0&showTitle=false&size=148269&status=done&style=none&taskId=ue22a1187-d065-4726-a332-3278be5672f&title=&width=395.5)

于是感觉这个方向是靠谱的，大概率是这段时间的请求量太大了，导致很多对象被分配到老年代，然后触发了很多次FullGC。

于是开始看系统监控，找这段时间有没有哪些接口或者请求的量比较大。

1、先是翻了我们的RPC接口的调用量监控，发现并无异常。<br />2、继续翻了MQ的消费量监控，发现并无异常。<br />3、查看对外部接口的调用量监控，发现有一个接口的调用量QPS比较高。

![image.png](https://cdn.nlark.com/yuque/0/2024/png/5378072/1705373703428-650e7c95-4eeb-4cfc-9205-b840b93b028e.png#averageHue=%23fcfcfc&clientId=u3406b14e-ab63-4&from=paste&height=176&id=u4b2964bf&originHeight=204&originWidth=793&originalType=binary&ratio=2&rotation=0&showTitle=false&size=87222&status=done&style=none&taskId=u88d7359c-0e79-4fe3-869a-d68e1784342&title=&width=683.5)

4、查看TDDL的QPS（TDDL是阿里内部的数据库访问中间件，可以简单认为是数据库连接池）飙高明显。

![image.png](https://cdn.nlark.com/yuque/0/2024/png/5378072/1705373673899-979feaae-4ff5-4392-b07f-508d30856ef2.png#averageHue=%23fafafa&clientId=u3406b14e-ab63-4&from=paste&height=237&id=u89f59751&originHeight=267&originWidth=789&originalType=binary&ratio=2&rotation=0&showTitle=false&size=116489&status=done&style=none&taskId=u1f284ec0-1d6c-4d6f-975a-d3c4d0959d9&title=&width=701.5)

结合这两个现象，一个是具体哪个接口调用量变大了，一个是哪张表的写操作变多了。而且时间也刚好对得上，于是就继续分析。

开始找这部分代码的调用链，发现是一个定时任务，扫表进行数据操作的。而且时间也对得上，每小时执行一次，基本上是在整点开始执行，大概15分钟执行完。

这里交代一下背景，我们的定时任务是分布式任务，也就是说比如有10000条数据，会有10台机器并发去扫描这些数据来处理。

这里奇怪的是只有一台机器有这个FullGC以及堆被干满的情况，这不应该啊。于是我开始逐台机器查看他最近12小时的堆内存情况。

还真让我发现了一个规律，那就是在不同的时间点，都会有其中某台机器的内存升高，有的时候会有FullGC，有的时候没有，而且基本都是在整点左右开始，并且也伴随着前面发现的TDDL和接口的调用量飙升。

以下是另外一台机器在前一天19点左右的堆内存及GC情况。<br />![image.png](https://cdn.nlark.com/yuque/0/2024/png/5378072/1705373985131-863c535e-6907-4a88-ae4c-a1df9dd63e96.png#averageHue=%23fbfbfb&clientId=u3406b14e-ab63-4&from=paste&height=523&id=uc5531c6b&originHeight=566&originWidth=789&originalType=binary&ratio=2&rotation=0&showTitle=false&size=230989&status=done&style=none&taskId=u81db0f0e-2b21-4d5c-af67-131a1d01285&title=&width=728.5)


再来总结一下现象：

1、每个整点开始，会有一个扫表任务开始执行，任务是分布式，多台机器并发开始执行。<br />2、任务开始执行后，会有某台机器的老年代内存被干满，导致频繁的FullGC<br />3、GC是有效果的，说明这些对象都是垃圾对象。可以被回收的。那就是因为年轻代放不下了导致的，而不是内存泄漏。

到这里，聪明的我马上想到了问题可能出在哪了。（机智脸

然后我开始验证我的想法，找到一个整点时间点，然后去看这个整点之后的15分钟内，有堆内催增长的机器，和没有堆内存增长（或者增长不明显）的机器，对比日志。

假设：9：00是11.11.11.11这台机器FullGC了， 10：00 是22.22.22.22这台机器FullGC了。

那么就这么对比：

11.11.11.11机器在9:00 - 10:00之间的日志，和，11.11.11.11机器在8：00-9：00之间的日志对比。<br />1.11.11.11机器在9:00 - 10:00之间的日志，和，22.22.22.22机器在10：00-11：00之间的日志对比。

然后我就发现了问题：

11.11.11.11机器在9:00 - 10:00之间和22.22.22.22机器在10：00-11：00之间扫描的数据范围是一样的，都是用户ID以22作为开头的数据。

因为我们是分布式任务扫表，为了扫表不重复，所以会根据用户ID的前两位进行分段，然后随机给不同的机器去扫描不同的前缀的用户。

刚好22开头的用户太多（数据严重倾斜）导致分到这段的机器就要处理大量数据，处理过程中会创建很多对象，导致内存被占用。然后因为随机分的，所以不同的时段会有不同的机器内存被干满。

可以看到22开头的明显比别的多很多：<br />![image.png](https://cdn.nlark.com/yuque/0/2024/png/5378072/1705376325424-fb817c4f-738c-4622-bc70-7d5666967956.png#averageHue=%23dbdcdb&clientId=ubd46fc8d-e35d-4&from=paste&height=422&id=u7da50cea&originHeight=844&originWidth=1094&originalType=binary&ratio=2&rotation=0&showTitle=false&size=572539&status=done&style=none&taskId=u38290108-6baf-416d-891a-3b2349a10fc&title=&width=547)

如下图，是一台6:00-7:00之间有FullGC的机器的日志情况：<br />![image.png](https://cdn.nlark.com/yuque/0/2024/png/5378072/1705377149520-5e8f70bc-e605-48c4-bd1b-0acf6dba2b14.png#averageHue=%23f6f5f5&clientId=u8a7d49dc-f5b1-4&from=paste&height=407&id=u9c49faca&originHeight=814&originWidth=3314&originalType=binary&ratio=2&rotation=0&showTitle=false&size=1911168&status=done&style=none&taskId=u7ab9a486-449f-4c50-8caa-18853785efc&title=&width=1657)

### 问题解决

问题定位到了，解决就容易了，想办法让倾斜的数据在分布式任务扫表的时候均分就行了。有几个办法：

1、之前是按照用户ID前两位分的，那么就再分的细一点，按照前3位分一下。<br />2、不再按照用户ID分，而是按照主键ID进行分段。（最开始没用这个方案是因为待扫描数据并不连续，区间长度不太好掌握，还有个重要原因就是需要在SQL中针对相同用户做数据聚合。）

