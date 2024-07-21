# 典型回答

JVM工具主要用来监控JVM的，这类工具主要分为两大类，第一类是JVM自带的，比如jstat、jmap等，还有依赖是第三方的，如VisualVM等。

这些工具的用途都不太一样，监控的方向也不一样。下面是简单的介绍：

**jps：JDK 1.5提供的一个显示当前所有java进程pid的命令，简单实用，非常适合在linux/unix平台上简单察看当前java进程的一些简单情况。**<br />[✅jps命令的作用是什么？](https://www.yuque.com/hollis666/fo22bm/zqvswmpgrsr2x4lg?view=doc_embed)

**jstack：Java虚拟机自带的命令行工具，主要用于生成线程的堆栈信息，用于诊断死锁及线程阻塞等问题。**

[✅jstack命令的作用是什么？](https://www.yuque.com/hollis666/fo22bm/hc8uutqs3wnsenr9?view=doc_embed)

**jmap：Java虚拟机自带的命令行工具，可以生成JVM中堆内存的Dump文件，用于分析堆内存的使用情况。排查内存泄漏等问题。**

[✅jmap命令的作用是什么？](https://www.yuque.com/hollis666/fo22bm/inr6hifpadl24nao?view=doc_embed)

**jstat：Java虚拟机自带的命令行工具，主要用来监控JVM中的类加载、GC、线程等信息。**

[✅jstat命令的作用是什么？](https://www.yuque.com/hollis666/fo22bm/nl7i1d66zs9g3lgb?view=doc_embed)

**jhat：使用jmap可以生成Java堆的Dump文件，生成dump文件之后就可以用jhat命令，将dump文件转成html的形式，然后通过http访问可以查看堆情况。**

[✅jhat有什么用，如何用他分析堆dump](https://www.yuque.com/hollis666/fo22bm/nhd29y82stcf64bi?view=doc_embed)

JConsole：一个基于JMX（Java Management Extensions）的监控工具，可以用来监视JVM中的内存、线程、GC等信息，并可以执行一些诊断和故障排除任务。<br />VisualVM：一个基于NetBeans平台的可视化工具，可以监视本地和远程JVM进程的性能和资源使用情况，包括CPU、内存、线程、GC等信息，并可以进行故障排除和性能分析。<br />YourKit：一个商业的JVM分析工具，可以进行内存、CPU、线程等方面的分析，提供了一些高级功能如内存泄漏检测、代码热替换等。<br />JProfiler：一个商业的JVM分析工具，可以监视JVM中的内存、线程、GC等信息，并提供一些高级功能如代码分析、内存泄漏检测等。<br />**Arthas：Arthas 是Alibaba开源的Java诊断工具，非常强大，非常推荐，**
