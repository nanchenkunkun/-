# 典型回答

我们知道，想要把高级语言转变成计算机认识的机器语言有两种方式，分别是编译和解释，虽然Java转成机器语言的过程中有一个步骤是要编译成字节码，但是，这里的字节码并不能在机器上直接执行。

所以，JVM中内置了解释器(interpreter)，在运行时对字节码进行解释翻译成机器码，然后再执行。

解释器的执行方式是一边翻译，一边执行，因此执行效率很低。为了解决这样的低效问题，HotSpot引入了JIT技术（Just-In-Time）。

有了JIT技术之后，JVM还是通过解释器进行解释执行。但是，**当JVM发现某个方法或代码块运行时执行的特别频繁的时候，就会认为这是“热点代码”（Hot Spot Code)。然后JIT会把部分“热点代码”翻译成本地机器相关的机器码，并进行优化，然后再把翻译后的机器码缓存起来，以备下次使用。**<br />![](https://cdn.nlark.com/yuque/0/2022/jpeg/5378072/1667466529876-3467ee10-0d71-4f1a-a42c-76fdcfe224ad.jpeg#averageHue=%23fefefe&clientId=u809fbcd6-6024-4&from=paste&id=ued347647&originHeight=967&originWidth=1389&originalType=url&ratio=1&rotation=0&showTitle=false&status=done&style=none&taskId=u158acfa1-ac74-4594-8557-0ec377d0bea&title=)

# 扩展知识

HotSpot虚拟机中内置了两个JIT编译器：Client Complier和Server Complier，分别用在客户端和服务端，目前主流的HotSpot虚拟机中默认是采用解释器与其中一个编译器直接配合的方式工作。

当 JVM 执行代码时，它并不立即开始编译代码（因为Java默认是解释执行的）。首先，如果这段代码本身在将来只会被执行一次，那么从本质上看，编译就是在浪费精力。因为将代码翻译成 java 字节码相对于编译这段代码并执行代码来说，要快很多。第二个原因是最优化，当 JVM 执行某一方法或遍历循环的次数越多，就会更加了解代码结构，那么 JVM 在编译代码的时候就做出相应的优化。

[✅Java是编译型还是解释型?](https://www.yuque.com/hollis666/fo22bm/ylde5u?view=doc_embed)

在机器上，执行java -version命令就可以看到自己安装的JDK中JIT是哪种模式:

[![](https://cdn.nlark.com/yuque/0/2022/png/5378072/1667466581344-a66e636c-af2a-4a78-9b0f-be259b5fca0f.png#averageHue=%230f0d0b&clientId=u809fbcd6-6024-4&from=paste&id=ub795ad76&originHeight=87&originWidth=452&originalType=url&ratio=1&rotation=0&showTitle=false&status=done&style=none&taskId=u96ecf1ab-0b28-43a3-ab04-f7669f19eb6&title=)](http://www.hollischuang.com/wp-content/uploads/2018/04/javaversion.png)

上图是我的机器上安装的jdk1.8，可以看到，他是Server Compile，但是，需要说明的是，无论是Client Complier还是Server Complier，解释器与编译器的搭配使用方式都是混合模式，即上图中的mixed mode。
## 热点检测

上面我们说过，要想触发JIT，首先需要识**别出热点代码**。目前主要的热点代码识别方式是热点探测（Hot Spot Detection），有以下两种：

**1、基于采样的方式探测（Sample Based Hot Spot Detection) **：周期性检测各个线程的栈顶，发现某个方法经常出现在栈顶，就认为是热点方法。好处就是简单，缺点就是无法精确确认一个方法的热度。容易受线程阻塞或别的原因干扰热点探测。

**2、基于计数器的热点探测（Counter Based Hot Spot Detection)**。采用这种方法的虚拟机会为每个方法，甚至是代码块建立计数器，统计方法的执行次数，某个方法超过阀值就认为是热点方法，触发JIT编译。

在HotSpot虚拟机中使用的是第二种——基于计数器的热点探测方法，因此它为每个方法准备了两个计数器：方法调用计数器和回边计数器。

方法计数器。顾名思义，就是记录一个方法被调用次数的计数器。<br />回边计数器。是记录方法中的for或者while的运行次数的计数器。

## 编译优化
前面提到过，JIT除了具有缓存的功能外，还会对代码做各种优化。说到这里，不得不佩服HotSpot的开发者，他们在JIT中对于代码优化真的算是面面俱到了。

主要的优化有：逃逸分析、 锁消除、 锁膨胀、 方法内联、 空值检查消除、 类型检测消除、 公共子表达式消除。接下来挑几个重点的介绍一下。

### 逃逸分析

[✅什么是逃逸分析？](https://www.yuque.com/hollis666/fo22bm/vwrawt9lig6whl4o?view=doc_embed)

### 锁消除
在动态编译同步块的时候，JIT编译器可以借助逃逸分析来判断同步块所使用的锁对象是否只能够被一个线程访问而没有被发布到其他线程。

如果同步块所使用的锁对象通过这种分析被证实只能够被一个线程访问，那么JIT编译器在编译这个同步块的时候就会取消对这部分代码的同步。这个取消同步的过程就叫同步省略，也叫**锁消除**。

如以下代码：
```
public void f() {
    Object hollis = new Object();
    synchronized(hollis) {
        System.out.println(hollis);
    }
}

```
代码中对hollis这个对象进行加锁，但是hollis对象的生命周期只在f()方法中，并不会被其他线程所访问到，所以在JIT编译阶段就会被优化掉。优化成：

```
public void f() {
    Object hollis = new Object();
    System.out.println(hollis);
}
```

所以，在使用synchronized的时候，如果JIT经过逃逸分析之后发现并无线程安全问题的话，就会做锁消除。

### 标量替换&栈上分配

[Java中的对象一定在堆上分配内存吗？](https://www.yuque.com/hollis666/fo22bm/bx3qiz80wclfbmpw?view=doc_embed)


### 方法内联

方法内联是Java中的一个优化技术，即时编译器（JIT）用它来提高程序的运行效率。在Java中，方法内联意味着将一个方法的代码直接插入到调用它的地方，从而避免了方法调用的开销。这种优化对于小型且频繁调用的方法特别有用。

```java
public class InlineExample {
    public int add(int a, int b) {
        return a + b;
    }

    public void exampleMethod() {
        int result = add(5, 3);
        // 其他操作
    }
}
```

在这个例子中，add 方法很简单，JIT 编译器可能会选择将 add方法 直接内联在 exampleMethod 中，避免了对 add 方法的实际调用。这样可以减少调用开销，提高程序的执行速度，类似

```java
public class InlineExample {

    public void exampleMethod() {
        int result = 5 + 3; // 这里直接使用了add方法的内部逻辑
        // 其他操作
    }
}

```

## JIT优化可能带来的问题
大家理解了JIT编译的原理之后，其实可以知道，**JIT优化是在运行期进行的**，并且也不是Java进程刚一启动就能优化的，是需要先执行一段时间的，因为他需要先知道哪些是热点代码。

所以，在JIT优化开始之前，我们的所有请求，都是要经过解释执行的，这个过程就会相对慢一些。

而且，如果你们的应用的请求量比较大的的话，这种问题就会更加明显，在应用启动过程中，会有大量的请求过来，这就会导致解释器持续的在努力工作。

一旦解释器对CPU资源占用比较大的话，就会间接的导致CPU、LOAD等飙高，导致应用的性能进一步下降。这也是为什么很多应用在发布过程中，会出现刚刚重启好的应用会发生大量的超时问题了。

而随着请求的不断增多，JIT优化就会被触发，这就是使得后续的热点请求的执行可能就不需要在通过解释执行了，直接运行JIT优化后缓存的机器码就行了。

### 如何解决
那么，怎么解决这样的问题呢？

主要有两种思路：

**1、提升JIT优化的效率**<br />**2、降低瞬时请求量**

在提升JIT优化效率的设计上，大家可以了解一下阿里研发的JDK——Dragonwell。

这个相比OpenJDK提供了一些专有特性，其中一项叫做JwarmUp的技术就是解决JIT优化效率的问题的。

这个技术主要是通过记录Java应用上一次运行时候的编译信息到文件中，在下次应用启动时，读取该文件，从而在流量进来之前，提前完成类的加载、初始化和方法编译，从而跳过解释阶段，直接执行编译好的机器码。

除了针对JDK做优化之外，还可以采用另外一种方式来解决这个问题，那就是做预热。

很多人都听说过缓存预热，其实思想是类似的。

就是说在应用刚刚启动的时候，通过调节负载均衡，不要很快的把大流量分发给他，而是先分给他一小部分流量，通过这部分流量来触发JIT优化，等优化好了之后，再把流量调大。

