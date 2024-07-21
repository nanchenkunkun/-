# 典型回答

Safe Point（安全点）是JVM中的一个关键概念。官方的解释是（[https://openjdk.org/groups/hotspot/docs/HotSpotGlossary.html](https://openjdk.org/groups/hotspot/docs/HotSpotGlossary.html)）：
> A point during program execution at which all GC roots are known and all heap object contents are consistent. From a global point of view, all threads must block at a safepoint before the GC can run. (As a special case, threads running JNI code can continue to run, because they use only handles. During a safepoint they must block instead of loading the contents of the handle.) From a local point of view, a safepoint is a distinguished point in a block of code where the executing thread may block for the GC. Most call sites qualify as safepoints. There are strong invariants which hold true at every safepoint, which may be disregarded at non-safepoints. Both compiled Java code and C/C++ code be optimized between safepoints, but less so across safepoints. The JIT compiler emits a GC map at each safepoint. C/C++ code in the VM uses stylized macro-based conventions (e.g., TRAPS) to mark potential safepoints.


但是这玩意基本看不懂，**安全点，简单点说就是代码执行过程中的一些特殊位置，当线程执行到这个位置的时候，可以被认为处于“安全状态”，如果有需要，可以在这里暂停，在这里暂停是安全的！**

这些安全点通常出现在不会改变共享数据状态的位置，例如在方法调用、循环迭代和异常抛出的地方。

因为在JVM中，有一些操作并不是说随时随地就可以随意就开始执行的，比如GC，所以需要安全点，这样就可以在安全点时执行这些特殊操作。

哪些操作需要等到安全点呢？其实网上总结了很多，难道要死记硬背吗？不需要，只需要记住：**当JVM需要对线程进行挂起的时候，会等到安全点在执行。**

因为安全点确保了线程在可预测和一致的状态下停止。这是非常重要的，因为我们后面要提及的各种操作，如GC、JIT优化、偏向锁撤销、线程Dump等的，都需要JVM能保证线程不会再修改共享数据，那如何保证呢？那就是安全点了。

那么，JVM中什么情况会把线程挂起呢？主要有以下这些：

**垃圾回收**：

- 当JVM进行垃圾回收时，需要暂停应用中的所有线程（称为“Stop-The-World”，STW），以防止它们在内存回收过程中修改对象。在这个过程中，只有当所有线程都运行到了安全点，JVM才会开始垃圾收集。

[✅什么是Stop The World？](https://www.yuque.com/hollis666/fo22bm/am0cl3?view=doc_embed)

**偏向锁撤销**：

- 偏向锁是针对单一线程优化的。当另一个线程尝试获取相同锁时，JVM需要撤销原有线程的偏向锁状态，以便其他线程能够竞争该锁。为了安全地完成这个过程，JVM必须确保持有偏向锁的线程不在执行与该锁相关的代码。因此，JVM会将这个线程挂起，直到它到达安全点。

[✅为什么JDK 15要废弃偏向锁？](https://www.yuque.com/hollis666/fo22bm/kzigekbg6ark71m3?view=doc_embed)

**代码热替换**：

   - 在某些情况下，比如使用JVM的调试工具时，开发者可能需要在运行时替换或修改类的定义。为了确保这种替换可以安全地发生，JVM会等到线程到达安全点。

**获取Dump**：

- 对线程/堆进行Dump时（执行jstack、jmap等命令时），是想要获取线程或者堆在特定时刻的状态和信息。为了确保这些信息的准确性和一致性，JVM在进行Dump时会暂停所有线程。也需要进入安全点才行。

[✅什么是Java Dump，如何获取？](https://www.yuque.com/hollis666/fo22bm/tmcw0o39ws6vi6ug?view=doc_embed)

**死锁检测**：

- 和Dump一样，当JVM执行死锁检测时，需要挂起线程，以获取线程间锁的精确状态。也需要进入安全点。

**JIT编译优化**：

- 在进行JIT编译时，如果相关代码正在被线程执行，那么这些线程需要被挂起，以确保编译过程中代码的一致性和稳定性。这样可以防止正在运行的线程执行未完成优化的代码，确保代码优化的正确性和程序的稳定运行。同理，也需要进入安全点。

[✅简单介绍一下JIT优化技术？](https://www.yuque.com/hollis666/fo22bm/nkr4ge?view=doc_embed)

**定时进入**：

- JVM有一个参数`-XX:GuaranteedSafepointInterval`，他的作用是设置JVM在执行长时间运行的代码时强制进行安全点检查的时间间隔。这有助于确保即使在执行长时间的计算操作时，JVM也能定期进行如垃圾回收等全局操作。这个参数通常用于性能调优，以在长时间计算和系统管理操作之间取得平衡。

# 扩展知识

## GC与安全点

当垃圾收集器决定执行垃圾回收时，它会首先通知JVM的运行时系统需要一个停顿（Stop-The-World）。

然后，JVM运行时系统会设置一个安全点，并要求所有线程在执行到下一个安全点时暂停。

线程到达安全点的方式可能是通过定期检查是否需要到达安全点的标志，或者在执行某些特定的操作时自动检查。

这样，当所有线程都到达安全点时，JVM就可以安全地进行垃圾回收，因为此时所有线程都处于一种可预测和一致的状态。
## 安全区域

安全区域（Safe Region）是JVM的一种机制，用于处理无法立即响应到达安全点请求的线程。

当JVM要执行全局操作（如垃圾收集）且需要所有线程都处于安全点时，如果某些线程由于正在执行无法立即中断的操作而无法立即到达安全点，如这几种情况。

> 1. 长时间的计算操作：没有涉及到任何可以触发安全点检查的点（如方法调用、循环迭代）的长时间运行的计算。
> 2. 阻塞I/O操作：线程在执行阻塞的I/O操作时，可能无法立即响应安全点请求。
> 3. 特定的系统调用：某些系统调用可能导致线程在操作完成前无法响应安全点请求。


那怎么办呢？总不能一直等吧。

于是安全区域就有用了，上面说的这些情况中，比如长时间的内存计算，其实是不会和GC这样的操作相冲突的，他不会改变对象的引用关系。所以，这种代码区域就可以称之为安全区域。

当线程运行到安全的代码时，会告诉JVM自己在安全区域了。这时候JVM再通知别人要到安全点等待的时候，他就会认为这些线程虽然没在安全点，但是因为处于安全区域，所以也可以进行正常的GC。

当这段代码执行完了，要退出安全区域的时候，就需要检查一下，自己能不能退出去，比如看看GC是否在运行。


