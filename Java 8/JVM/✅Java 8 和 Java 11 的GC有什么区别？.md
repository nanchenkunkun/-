# 典型回答

Java 8 和 Java 11都是LTS版本的JDK，所以会有人经常问他们之间的区别。特别是在GC上面的差别。

首先，在垃圾收集器上面，**Java 8 中默认的Parallel Scavenge GC+Parallel Old GC的**，分别用来做新生代和老年代的垃圾回收。而在**Java 11中默认采用的是G1进行整堆回收的**（Java 9中就是默认的了）。

另外，**Java 11中还新增了一种垃圾收集器，那就是ZGC**，他可以在保证高吞吐量的同时保证最短的暂停时间。

[✅JDK 11中新出的ZGC有什么特点？](https://www.yuque.com/hollis666/fo22bm/qpu0uu6em1ompzeh?view=doc_embed)

在知道了垃圾收集器上面的区别之后，就可以基于Parallel Scavenge GC+Parallel Old GC 和 G1的区别进一步说一下GC上面的区别了。

在垃圾识别及回收上面，**Java 8基于的是单纯地可达性分析，而Java 11中的G1采用的是三色标记法，可以大大降低STW的时长。**


[✅什么是三色标记算法？](https://www.yuque.com/hollis666/fo22bm/lva8a9gfhagbrw2g?view=doc_embed)

另外，**G1的内存划分是自适应的，它会根据堆的大小和使用情况来动态调整各个区域的大小和比例。而Parallel Scavenge GC+Parallel Old GC都是固定分配的策略。**

