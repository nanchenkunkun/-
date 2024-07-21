# 典型回答

CAS是一项乐观锁技术，是Compare And Swap的简称，顾名思义就是先比较再替换。CAS 操作包含三个操作数 —— 内存位置（V）、预期原值（A）和新值(B)。在进行并发修改的时候，会先比较A和V中取出的值是否相等，如果相等，则会把值替换成B，否则就不做任何操作。

当多个线程尝试使用CAS同时更新同一个变量时，只有其中一个线程能更新变量的值，而其它线程都失败，失败的线程并不会被挂起，而是被告知这次竞争中失败，并可以再次尝试。

在JDK1.5 中新增java.util.concurrent(J.U.C)就是建立在CAS之上的。相对于synchronized这种阻塞算法，CAS是非阻塞算法的一种常见实现。所以J.U.C在性能上有了很大的提升。

CAS的主要应用就是实现乐观锁和锁自旋。
# 扩展知识

## ABA问题

CAS会导致“ABA问题”。

CAS算法实现一个重要前提需要取出内存中某时刻的数据，而在下时刻比较并替换，那么在这个时间差类会导致数据的变化。

比如说一个线程1从内存位置V中取出A，这时候另一个线程2也从内存中取出A，并且2进行了一些操作变成了B，然后2又将V位置的数据变成A，这时候线程1进行CAS操作发现内存中仍然是A，然后1操作成功。尽管线程1的CAS操作成功，但是不代表这个过程就是没有问题的。

举个例子，线程1和线程2同时通过CAS尝试修改用户A余额，线程1和线程2同时查询当前余额为100元，然后线程2因为用户A要把钱借给用户B，先把余额从100改成50。然后又有用户C还给用户A 50元，线程2则又把50改成了100。这是线程1继续修改，把余额从100改成200。

虽然过程上金额都没问题，都改成功了，但是对于用户余额来说，丢失了两次修改的过程，在修改前用户C欠用户A 50元，但是修改后，用户C不欠钱了，而用户B欠用户A 50元了。而这个过程数据是很重要的。

部分乐观锁的实现是通过版本号（version）的方式来解决ABA问题，乐观锁每次在执行数据的修改操作时，都会带上一个版本号，一旦版本号和数据的版本号一致就可以执行修改操作并对版本号执行+1操作，否则就执行失败。因为每次操作的版本号都会随之增加，所以不会出现ABA问题，因为版本号只会增加不会减少。  

**在Java中，可以借助AtomicStampedReference**，它是 Java 并发编程中的一个类，用于解决多线程环境下的“ABA”问题。AtomicStampedReference 通过同时维护一个引用和一个时间戳，可以解决 ABA 问题。它允许线程在执行 CAS 操作时，不仅检查引用是否发生了变化，还要检查时间戳是否发生了变化。这样，即使一个变量的值被修改后又改回原值，由于时间戳的存在，线程仍然可以检测到这中间的变化。

```
import java.util.concurrent.atomic.AtomicStampedReference;

public class Example {
    public static void main(String[] args) {
        String initialRef = "hollis";
        int initialStamp = 0;

        AtomicStampedReference<String> atomicStampedRef =
            new AtomicStampedReference<>(initialRef, initialStamp);

        String newRef = "hollis666";
        int newStamp = initialStamp + 1;

        boolean updated = atomicStampedRef.compareAndSet(initialRef, newRef, initialStamp, newStamp);
        System.out.println("Updated: " + updated);
    }
}

```

在这个例子中，AtomicStampedReference 初始化时不仅包含了一个初始引用 "hollis"，还包含了一个初始的时间戳 0。当我们尝试使用 compareAndSet 方法更新引用时，我们同时提供了期望的引用值和时间戳，以及新的引用值和时间戳。这样，只有当当前引用和时间戳都匹配期望值时，更新操作才会成功，从而避免了 ABA 问题。

## 忙等待
因为CAS基本都是要自旋的，这种情况下，如果并发冲突比较大的话，就会导致CAS一直在不断地重复执行，就会进入忙等待。

[✅CAS一定有自旋吗？](https://www.yuque.com/hollis666/fo22bm/cle1ag1rfu3uuwzg?view=doc_embed)

> 忙等待定义 一种进程执行状态。 进程执行到一段循环程序的时候，由于循环判断条件不能满足而导致处理器反复循环，处于繁忙状态，该进程虽然繁忙但无法前进。


所以，一旦CAS进入忙等待状态一直执行不成功的话，会对CPU造成较大的执行开销。

## 乐观锁悲观锁

乐观锁（ Optimistic Locking）其实是一种思想。乐观锁假设认为数据一般情况下不会造成冲突，所以在数据进行提交更新的时候，才会正式对数据的冲突与否进行检测，如果发现冲突了，则让返回用户错误的信息，让用户决定如何去做。

乐观锁的具体实现细节：主要就是两个步骤：冲突检测和数据更新。其实现方式有一种比较典型的就是CAS

相对于乐观锁，还有悲观锁，这是一种对数据的修改抱有悲观态度的并发控制方式，一般在认为数据被并发修改的概率比较大的时候，需要在修改之前先加锁的时候使用。

JAVA中的synchronized就是一种悲观锁，但是这种悲观锁机制存在以下问题：

- 在多线程竞争下，加锁、释放锁会导致比较多的上下文切换和调度延时，引起性能问题。
- 一个线程持有锁会导致其它所有需要此锁的线程挂起。
- 如果一个优先级高的线程等待一个优先级低的线程释放锁会导致优先级倒置，引起性能风险。

## Java对CAS的支持

在JDK1.5 中新增java.util.concurrent(J.U.C)就是建立在CAS之上的。相对于synchronized这种阻塞算法，CAS是非阻塞算法的一种常见实现。所以J.U.C在性能上有了很大的提升。<br />我们以java.util.concurrent中的AtomicInteger为例，看一下在不使用锁的情况下是如何保证线程安全的。主要理解getAndIncrement方法，该方法的作用相当于 i++ 操作。

```
public class AtomicInteger extends Number implements java.io.Serializable {  

        private volatile int value;  

    public final int get() {  
        return value;  
    }  

    public final int getAndIncrement() {  
        for (;;) {  
            int current = get();  
            int next = current + 1;  
            if (compareAndSet(current, next))  
                return current;  
        }  
    }  

    public final boolean compareAndSet(int expect, int update) {  
        return unsafe.compareAndSwapInt(this, valueOffset, expect, update);  
    }  
}
```

在没有锁的机制下需要字段value要借助volatile原语，保证线程间的数据是可见的。这样在获取变量的值的时候才能直接读取。然后来看看++i是怎么做到的。

getAndIncrement采用了CAS操作，每次从内存中读取数据然后将此数据和+1后的结果进行CAS操作，如果成功就返回结果，否则重试直到成功为止。而compareAndSet利用JNI来完成CPU指令的操作。

## CAS与对象创建

另外，CAS还有一个应用，那就是在JVM创建对象的过程中。

对象创建在虚拟机中是非常频繁的。即使是仅仅修改一个指针所指向的位置，在并发情况下也不是线程安全的，可能正在给对象A分配内存空间，指针还没来得及修改，对象B又同时使用了原来的指针来分配内存的情况。

解决这个问题的方案有两种，其中一种就是采用CAS配上失败重试的方式保证更新操作的原子性。

## 不使用synchronized如何实现线程安全的单例？

[✅不使用锁如何实现线程安全的单例？](https://www.yuque.com/hollis666/fo22bm/zfwetw?view=doc_embed)
