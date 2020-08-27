## HashMap存在的问题：

### HashMap线程不安全

因为多线程环境下，使用Hashmap进行put操作可能会引起死循环，导致CPU利用率接近100%，所以在并发情况下不能使用HashMap

### Hashtable线程安全但效率低下

Hashtable容器使用synchronized来保证线程安全，但在线程竞争激烈的情况下Hashtable的效率非常低下。因为当一个线程访问Hashtable的同步方法时，其他线程访问Hashtable的同步方法时，可能会进入阻塞或轮询状态。如线程1使用put进行添加元素，线程2不但不能使用put方法添加元素，并且也不能使用get方法来获取元素，所以竞争越激烈效率越低。



## 解决

### 分段锁

HashTable容器在竞争激烈的并发环境下表现出效率低下的原因，是因为所有访问HashTable的线程都必须竞争同一把锁，那假如容器里有多把锁，每一把锁用于锁容器其中一部分数据，那么当多线程访问容器里不同数据段的数据时，线程间就不会存在锁竞争，从而可以有效的提高并发访问效率，这就是ConcurrentHashMap所使用的锁分段技术，首先将数据分成一段一段的存储，然后给每一段数据配一把锁，当一个线程占用锁访问其中一个段数据的时候，其他段的数据也能被其他线程访问。有些方法需要跨段，比如size()和containsValue()，它们可能需要锁定整个表而而不仅仅是某个段，这需要**按顺序**锁定所有段，操作完毕后，又**按顺序**释放所有段的锁。这里“按顺序”是很重要的，否则极有可能出现死锁，在ConcurrentHashMap内部，段数组是final的，并且其成员变量实际上也是final的，但是，仅仅是将数组声明为final的并不保证数组成员也是final的，这需要实现上的保证。这可以确保不会出现死锁，因为获得锁的顺序是固定的。

 ConcurrentHashMap是由Segment数组结构和HashEntry数组结构组成。Segment是一种可重入锁ReentrantLock，在ConcurrentHashMap里扮演锁的角色，HashEntry则用于存储键值对数据。一个ConcurrentHashMap里包含一个Segment数组，Segment的结构和HashMap类似，是一种数组和链表结构， 一个Segment里包含一个HashEntry数组，每个HashEntry是一个链表结构的元素， 每个Segment守护者一个HashEntry数组里的元素,当对HashEntry数组的数据进行修改时，必须首先获得它对应的Segment锁。

![](A:\gitdir\学习资料\java\java基础\java容器类\img\12.jpg)

JDK1.8的实现已经抛弃了Segment分段锁机制，利用CAS+Synchronized来保证并发更新的安全。数据结构采用：数组+链表+红黑树。

![](A:\gitdir\学习资料\java\java基础\java容器类\img\12.png)

```java
    //构造方法
    public ConcurrentHashMap(int initialCapacity) {
        if (initialCapacity < 0)//判断参数是否合法
            throw new IllegalArgumentException();
        int cap = ((initialCapacity >= (MAXIMUM_CAPACITY >>> 1)) ?
                   MAXIMUM_CAPACITY ://最大为2^30
                   tableSizeFor(initialCapacity + (initialCapacity >>> 1) + 1));//根据参数调整table的大小
        this.sizeCtl = cap;//获取容量
        //ConcurrentHashMap在构造函数中只会初始化sizeCtl值，并不会直接初始化table
    }
    //调整table的大小
    private static final int tableSizeFor(int c) {//返回一个大于输入参数且最小的为2的n次幂的数。
        int n = c - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }
```

tableSizeFor(int c)的原理：将c最高位以下通过|=运算全部变成1，最后返回的时候，返回n+1；
 eg:当输入为25的时候，n等于24，转成二进制为1100，右移1位为0110，将1100与0110进行或("|")操作，得到1110。接下来右移两位得11，再进行或操作得1111，接下来操作n的值就不会变化了。最后返回的时候，返回n+1，也就是10000，十进制为32。按照这种逻辑得到2的n次幂的数。
 那么为什么要先-1再+1呢？输入若是为0，那么不论怎么操作，n还是0，但是HashMap的容量只有大于0时才有意义。

### table初始化：

table初始化操作会延缓到第一次put行为。初始化的同时也保证了线程安全

```java
    final V putVal(K key, V value, boolean onlyIfAbsent) {
        if (key == null || value == null) throw new NullPointerException();
        int hash = spread(key.hashCode());
        int binCount = 0;
        for (Node<K,V>[] tab = table;;) {
            Node<K,V> f; int n, i, fh; K fk; V fv;
            if (tab == null || (n = tab.length) == 0)//判断table还未初始化
                tab = initTable();//初始化table
            else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) {
                if (casTabAt(tab, i, null, new Node<K,V>(hash, key, value)))
                    break;                   // no lock when adding to empty bin
            }
           ...省略一部分源码
        }
    } 
    
    private final Node<K,V>[] initTable() {
        Node<K,V>[] tab; int sc;
        while ((tab = table) == null || tab.length == 0) {
        //如果一个线程发现sizeCtl<0，意味着另外的线程执行CAS操作成功，当前线程只需要让出cpu时间片，
        //由于sizeCtl是volatile的，保证了顺序性和可见性
            if ((sc = sizeCtl) < 0)//sc保存了sizeCtl的值
                Thread.yield(); // lost initialization race; just spin
            else if (U.compareAndSetInt(this, SIZECTL, sc, -1)) {//cas操作判断并置为-1
                try {
                    if ((tab = table) == null || tab.length == 0) {
                        int n = (sc > 0) ? sc : DEFAULT_CAPACITY;//DEFAULT_CAPACITY = 16，若没有参数则大小默认为16
                        @SuppressWarnings("unchecked")
                        Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n];
                        table = tab = nt;
                        sc = n - (n >>> 2);
                    }
                } finally {
                    sizeCtl = sc;
                }
                break;
            }
        }
        return tab;
    }  

```

### put操作

```java
    
    final V putVal(K key, V value, boolean onlyIfAbsent) {
        if (key == null || value == null) throw new NullPointerException();
        int hash = spread(key.hashCode());//哈希算法
        int binCount = 0;
        for (Node<K,V>[] tab = table;;) {//无限循环，确保插入成功
            Node<K,V> f; int n, i, fh; K fk; V fv;
            if (tab == null || (n = tab.length) == 0)//表为空或表长度为0
                tab = initTable();//初始化表
            else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) {//i = (n - 1) & hash为索引值，查找该元素，
            //如果为null,说明第一次插入
                if (casTabAt(tab, i, null, new Node<K,V>(hash, key, value)))
                    break;                   // no lock when adding to empty bin
            }
            else if ((fh = f.hash) == MOVED)//MOVED=-1;当前正在扩容，一起进行扩容操作
                tab = helpTransfer(tab, f);
            else if (onlyIfAbsent && fh == hash &&  // check first node
                     ((fk = f.key) == key || fk != null && key.equals(fk)) &&
                     (fv = f.val) != null)
                return fv;
            else {
                V oldVal = null;
                synchronized (f) {//其他情况加锁同步
                    if (tabAt(tab, i) == f) {
                        if (fh >= 0) {
                            binCount = 1;
                            for (Node<K,V> e = f;; ++binCount) {
                                K ek;
                                if (e.hash == hash &&
                                    ((ek = e.key) == key ||
                                     (ek != null && key.equals(ek)))) {
                                    oldVal = e.val;
                                    if (!onlyIfAbsent)
                                        e.val = value;
                                    break;
                                }
                                Node<K,V> pred = e;
                                if ((e = e.next) == null) {
                                    pred.next = new Node<K,V>(hash, key, value);
                                    break;
                                }
                            }
                        }
                        else if (f instanceof TreeBin) {
                            Node<K,V> p;
                            binCount = 2;
                            if ((p = ((TreeBin<K,V>)f).putTreeVal(hash, key,
                                                           value)) != null) {
                                oldVal = p.val;
                                if (!onlyIfAbsent)
                                    p.val = value;
                            }
                        }
                        else if (f instanceof ReservationNode)
                            throw new IllegalStateException("Recursive update");
                    }
                }
                if (binCount != 0) {
                    if (binCount >= TREEIFY_THRESHOLD)
                        treeifyBin(tab, i);
                    if (oldVal != null)
                        return oldVal;
                    break;
                }
            }
        }
        addCount(1L, binCount);
        return null;
    }
    //哈希算法
    static final int spread(int h) {
        return (h ^ (h >>> 16)) & HASH_BITS;
    }
    //保证拿到最新的数据
    static final <K,V> Node<K,V> tabAt(Node<K,V>[] tab, int i) {
        return (Node<K,V>)U.getObjectAcquire(tab, ((long)i << ASHIFT) + ABASE);
    }
    //CAS操作插入节点，比较数组下标为i的节点是否为c，若是，用v交换，否则不操作。
    //如果CAS成功，表示插入成功，结束循环进行addCount(1L, binCount)看是否需要扩容
    static final <K,V> boolean casTabAt(Node<K,V>[] tab, int i,
                                        Node<K,V> c, Node<K,V> v) {
        return U.compareAndSetObject(tab, ((long)i << ASHIFT) + ABASE, c, v);
    }

```

## table扩容

当table容量不足的时候，即table的元素数量达到容量阈值sizeCtl，需要对table进行扩容。 整个扩容分为两部分：

1. 构建一个nextTable，大小为table的两倍。

2. 把table的数据复制到nextTable中。
    这两个过程在单线程下实现很简单，但是ConcurrentHashMap是支持并发插入的，扩容操作自然也会有并发的出现，这种情况下，第二步可以支持节点的并发复制，这样性能自然提升不少，但实现的复杂度也上升了一个台阶。

    第一步，构建nextTable，毫无疑问，这个过程只能只有单个线程进行nextTable的初始化.

```java
private final void addCount(long x, int check) {
    ... 省略部分代码
    if (check >= 0) {
        Node<K,V>[] tab, nt; int n, sc;
        while (s >= (long)(sc = sizeCtl) && (tab = table) != null &&
               (n = tab.length) < MAXIMUM_CAPACITY) {
            int rs = resizeStamp(n);
            if (sc < 0) {// sc < 0 表明此时有别的线程正在进行扩容
                if ((sc >>> RESIZE_STAMP_SHIFT) != rs || sc == rs + 1 ||
                    sc == rs + MAX_RESIZERS || (nt = nextTable) == null ||
                    transferIndex <= 0)
                    break;
                if (U.compareAndSwapInt(this, SIZECTL, sc, sc + 1))
                // 不满足前面5个条件时，尝试参与此次扩容，把正在执行transfer任务的线程数加1，+2代表有1个，+1代表有0个
                    transfer(tab, nt);
            }
            //试着让自己成为第一个执行transfer任务的线程
            else if (U.compareAndSwapInt(this, SIZECTL, sc,
                                         (rs << RESIZE_STAMP_SHIFT) + 2))
                transfer(tab, null);// 去执行transfer任务
            s = sumCount();// 重新计数，判断是否需要开启下一轮扩容
        }
    }
}
```

节点从table移动到nextTable，大体思想是遍历、复制的过程。遍历过所有的节点以后就完成了复制工作，把table指向nextTable，并更新sizeCtl为新数组大小的0.75倍 ，扩容完成。

## get操作

1. 判断table是否为空，如果为空，直接返回null。
2. 计算key的hash值，并获取指定table中指定位置的Node节点，通过遍历链表或则树结构找到对应的节点，返回value值。

```java
public V get(Object key) {
    Node<K,V>[] tab; Node<K,V> e, p; int n, eh; K ek;
    int h = spread(key.hashCode());
    if ((tab = table) != null && (n = tab.length) > 0 &&
        (e = tabAt(tab, (n - 1) & h)) != null) {
        if ((eh = e.hash) == h) {
            if ((ek = e.key) == key || (ek != null && key.equals(ek)))
                return e.val;
        }
        else if (eh < 0)
            return (p = e.find(h, key)) != null ? p.val : null;
        while ((e = e.next) != null) {
            if (e.hash == h &&
                ((ek = e.key) == key || (ek != null && key.equals(ek))))
                return e.val;
        }
    }
    return null;
}
```

## 和HashTable的区别：

> ConcurrentHashMap 是一个并发散列映射表，它允许完全并发的读取，并且支持给定数量的并发更新。
>  而HashTable和同步包装器包装的 HashMap，使用一个全局的锁来同步不同线程间的并发访问，同一时间点，只能有一个线程持有锁，也就是说在同一时间点，只能有一个线程能访问容器，这虽然保证多线程间的安全并发访问，但同时也导致对容器的访问变成串行化的了。

## 总结：

> Hashtable的任何操作都会把整个表锁住，是阻塞的。好处是总能获取最实时的更新，比如说线程A调用putAll写入大量数据，期间线程B调用get，线程B就会被阻塞，直到线程A完成putAll，因此线程B肯定能获取到线程A写入的完整数据。坏处是所有调用都要排队，效率较低。
>  ConcurrentHashMap 是设计为非阻塞的。在更新时会局部锁住某部分数据，但不会把整个表都锁住。同步读取操作则是完全非阻塞的。好处是在保证合理的同步前提下，效率很高。坏处是严格来说读取操作不能保证反映最近的更新。例如线程A调用putAll写入大量数据，期间线程B调用get，则只能get到目前为止已经顺利插入的部分数据。
>  应该根据具体的应用场景选择合适的HashMap。

