# 典型回答

ThreadLocal是java.lang下面的一个类，是用来解决java多线程程序中并发问题的一种途径；通过为每一个线程创建一份共享变量的副本来保证各个线程之间的变量的访问和修改互相不影响；

ThreadLocal存放的值是线程内共享的，线程间互斥的，主要用于线程内共享一些数据，避免通过参数来传递，这样处理后，能够优雅的解决一些实际问题。

比如一次用户的页面操作请求，我们可以在最开始的filter中，把用户的信息保存在ThreadLocal中，在同一次请求中，再使用到用户信息，就可以直接到ThreadLocal中获取就可以了。

ThreadLocal有四个方法，分别为：

- initialValue
   - 返回此线程局部变量的初始值
- get
   - 返回此线程局部变量的当前线程副本中的值。如果这是线程第一次调用该方法，则创建并初始化此副本。
- set
   - 将此线程局部变量的当前线程副本中的值设置为指定值。许多应用程序不需要这项功能，它们只依赖于 initialValue() 方法来设置线程局部变量的值。
- remove
   - 移除此线程局部变量的值。

# 扩展知识

## ThreadLocal的实现原理

ThreadLocal中用于保存线程的独有变量的数据结构是一个内部类：ThreadLocalMap，也是k-v结构。<br />key就是当前的ThreadLocal对象，而v就是我们想要保存的值。

![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1688455495250-dba1fb17-44bb-4ec7-8270-1319f708086f.png#averageHue=%23f8f7f6&clientId=u19db4d16-f323-4&from=paste&height=329&id=u84547a50&originHeight=518&originWidth=1220&originalType=binary&ratio=2&rotation=0&showTitle=false&size=518581&status=done&style=none&taskId=uc1b358c6-5432-4839-8e62-192889c4390&title=&width=775)

上图中基本描述出了Thread、ThreadLocalMap以及ThreadLocal三者之间的包含关系。

**Thread类对象中维护了ThreadLocalMap成员变量，而ThreadLocalMap维护了以ThreadLocal为key，需要存储的数据为value的Entry数组。**这是它们三者之间的基本包含关系，我们需要进一步到源码中寻找踪迹。

查看Thread类，内部维护了两个变量，threadLocals和inheritableThreadLocals，它们的默认值是null，它们的类型是ThreadLocal.ThreadLocalMap，也就是ThreadLocal类的一个静态内部类ThreadLocalMap。

在静态内部类ThreadLocalMap维护一个数据结构类型为Entry的数组，节点类型如下代码所示：

```
static class Entry extends WeakReference<ThreadLocal<?>> {
    /** The value associated with this ThreadLocal. */
    Object value;

    Entry(ThreadLocal<?> k, Object v) {
        super(k);
        value = v;
    }
}
```

从源码中我们可以看到，Entry结构实际上是继承了一个ThreadLocal类型的弱引用并将其作为key，value为Object类型。这里使用弱引用是否会产生问题，我们这里暂时不讨论，在文章结束的时候一起讨论一下，暂且可以理解key就是ThreadLocal对象。对于ThreadLocalMap，我们一起来了解一下其内部的变量：

```
// 默认的数组初始化容量
private static final int INITIAL_CAPACITY = 16;
// Entry数组，大小必须为2的幂
private Entry[] table;
// 数组内部元素个数
private int size = 0;
// 数组扩容阈值，默认为0，创建了ThreadLocalMap对象后会被重新设置
private int threshold;
```
这几个变量和HashMap中的变量十分类似，功能也类似。<br />ThreadLocalMap的构造方法如下所示：
```
/**
 * Construct a new map initially containing (firstKey, firstValue).
 * ThreadLocalMaps are constructed lazily, so we only create
 * one when we have at least one entry to put in it.
 */
ThreadLocalMap(ThreadLocal<?> firstKey, Object firstValue) {
    // 初始化Entry数组，大小 16
    table = new Entry[INITIAL_CAPACITY];
    // 用第一个键的哈希值对初始大小取模得到索引，和HashMap的位运算代替取模原理一样
    int i = firstKey.threadLocalHashCode & (INITIAL_CAPACITY - 1);
    // 将Entry对象存入数组指定位置
    table[i] = new Entry(firstKey, firstValue);
    size = 1;
    // 初始化扩容阈值，第一次设置为10
    setThreshold(INITIAL_CAPACITY);
}
```
从构造方法的注释中可以了解到，该构造方法是懒加载的，只有当我们创建一个Entry对象并需要放入到Entry数组的时候才会去初始化Entry数组。

## 应用场景

[✅ThreadLocal的应用场景有哪些？](https://www.yuque.com/hollis666/fo22bm/bpm9cgs19qwlgc1k?view=doc_embed)

## ThreadLocal内存泄露问题

[✅ThreadLocal为什么会导致内存泄漏？如何解决的？](https://www.yuque.com/hollis666/fo22bm/bueq7weva8ha9f1p?view=doc_embed)


