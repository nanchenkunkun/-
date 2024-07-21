# 典型回答

不一定，在HotSpot虚拟机中，存在JIT优化的机制，JIT优化中可能会进行逃逸分析，当经过逃逸分析发现某一个局部对象没有逃逸到线程和方法外的话，那么这个对象就可能不会在堆上分配内存，而是进行栈上分配。

# 扩展知识

### 逃逸分析

[✅简单介绍一下JIT优化技术？](https://www.yuque.com/hollis666/fo22bm/nkr4ge?view=doc_embed&inner=S0gWS)

### 标量替换

标量（Scalar）是指一个无法再分解成更小的数据的数据。Java中的原始数据类型就是标量。相对的，那些还可以分解的数据叫做聚合量（Aggregate），Java中的对象就是聚合量，因为他可以分解成其他聚合量和标量。

在JIT阶段，如果经过逃逸分析，发现一个对象不会被外界访问的话，那么经过JIT优化，就会把这个对象拆解成若干个其中包含的若干个成员变量来代替。这个过程就是标量替换。

```
public static void main(String[] args) {
   alloc();
}

private static void alloc() {
   Point point = new Point（1,2）;
   System.out.println("point.x="+point.x+"; point.y="+point.y);
}
class Point{
    private int x;
    private int y;
}
```

以上代码中，point对象并没有逃逸出alloc方法，并且point对象是可以拆解成标量的。那么，JIT就不会直接创建Point对象，而是直接使用两个标量int x ，int y来替代Point对象。

以上代码，经过标量替换后，就会变成：

```
private static void alloc() {
   int x = 1;
   int y = 2;
   System.out.println("point.x="+x+"; point.y="+y);
}
```

可以看到，Point这个聚合量经过逃逸分析后，发现他并没有逃逸，就被替换成两个标量了。那么标量替换有什么好处呢？就是可以大大减少堆内存的占用。因为一旦不需要创建对象了，那么就不再需要分配堆内存了。

标量替换为栈上分配提供了很好的基础。
### 栈上分配

我们知道，在一般情况下，对象和数组元素的内存分配是在堆内存上进行的。但是随着JIT编译器的日渐成熟，很多优化使这种分配策略并不绝对。JIT编译器就可以在编译期间根据逃逸分析的结果，来决定是否可以将对象的内存分配从堆转化为栈。

> hotspot的栈上分配其实就是通过前面我们说过的标量替换实现的！


我们来看以下代码：

```
public static void main(String[] args) {
    long a1 = System.currentTimeMillis();
    for (int i = 0; i < 1000000; i++) {
        alloc();
    }
    // 查看执行时间
    long a2 = System.currentTimeMillis();
    System.out.println("cost " + (a2 - a1) + " ms");
    // 为了方便查看堆内存中对象个数，线程sleep
    try {
        Thread.sleep(100000);
    } catch (InterruptedException e1) {
        e1.printStackTrace();
    }
}

private static void alloc() {
    User user = new User();
}

static class User {

}
```

其实代码内容很简单，就是使用for循环，在代码中创建100万个User对象。

我们在alloc方法中定义了User对象，但是并没有在方法外部引用他。也就是说，这个对象并不会逃逸到alloc外部。经过JIT的逃逸分析之后，就可以对其内存分配进行优化。

我们指定以下JVM参数并运行：

```
-Xmx4G -Xms4G -XX:+PrintGCDetails -XX:+HeapDumpOnOutOfMemoryError
```

在程序打印出 `cost XX ms` 后，代码运行结束之前，我们使用`[jmap][1]`命令，来查看下当前堆内存中有多少个User对象：

```
➜  ~ jps
2809 StackAllocTest
2810 Jps
➜  ~ jmap -histo 2809

 num     #instances         #bytes  class name
----------------------------------------------
   1:           524       87282184  [I
   2:       1000000       16000000  StackAllocTest$User
   3:          6806        2093136  [B
   4:          8006        1320872  [C
   5:          4188         100512  java.lang.String
   6:           581          66304  java.lang.Class
```

从上面的jmap执行结果中我们可以看到，堆中共创建了100万个`StackAllocTest$User`实例。

在关闭逃避分析的情况下（-XX:-DoEscapeAnalysis），虽然在alloc方法中创建的User对象并没有逃逸到方法外部，但是还是被分配在堆内存中。也就说，如果没有JIT编译器优化，没有逃逸分析技术，正常情况下就应该是这样的。即所有对象都分配到堆内存中。

接下来，我们开启逃逸分析，再来执行下以上代码。

```
-Xmx4G -Xms4G -XX:+DoEscapeAnalysis -XX:+PrintGCDetails -XX:+HeapDumpOnOutOfMemoryError
```

在程序打印出 `cost XX ms` 后，代码运行结束之前，我们使用`jmap`命令，来查看下当前堆内存中有多少个User对象：

```
➜  ~ jps
709
2858 Launcher
2859 StackAllocTest
2860 Jps
➜  ~ jmap -histo 2859

 num     #instances         #bytes  class name
----------------------------------------------
   1:           524      101944280  [I
   2:          6806        2093136  [B
   3:         83619        1337904  StackAllocTest$User
   4:          8006        1320872  [C
   5:          4188         100512  java.lang.String
   6:           581          66304  java.lang.Class
```

从以上打印结果中可以发现，开启了逃逸分析之后（-XX:+DoEscapeAnalysis），在堆内存中只有8万多个`StackAllocTest$User`对象。也就是说在经过JIT优化之后，堆内存中分配的对象数量，从100万降到了8万。

> 除了以上通过jmap验证对象个数的方法以外，读者还可以尝试将堆内存调小，然后执行以上代码，根据GC的次数来分析，也能发现，开启了逃逸分析之后，在运行期间，GC次数会明显减少。正是因为很多堆上分配被优化成了栈上分配，所以GC次数有了明显的减少。


## <br />
