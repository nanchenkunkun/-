# 典型回答

[✅ConcurrentHashMap为什么在JDK 1.8中废弃分段锁？](https://www.yuque.com/hollis666/fo22bm/gzavigfwro6fgs8o?view=doc_embed)

通过上文，我们知道，为了进一步提升并发度，降低内容占用，JDK 1.8 对 `ConcurrentHashMap` 的实现进行了重大改进，**不再使用分段锁，而是采用了一种基于节点锁的方法，采用“CAS+synchronized”的机制来保证线程安全。**这种新的设计旨在提供更高的并发级别并减少锁的争用。

那么，用CAS我们比较好理解，无锁化更加轻量，更加高效，但是加锁的时候为啥要用synchronized，而不是用ReentrantLock呢？

关于synchronized和ReentrantLock的区别，这里我就不展开介绍了，默认大家都是知道的，不知道的看下面这个即可：

[✅synchronized和reentrantLock区别？](https://www.yuque.com/hollis666/fo22bm/bitupp?view=doc_embed)

我们知道，大多数情况下，reentrantLock是要比synchronized更好的，也更加推荐使用，但是这里为啥不用呢？

首先，在JDK 1.7中，`ConcurrentHashMap` 使用了一种称为分段锁的机制。这种机制的核心思想是将哈希表分成多个段（Segment），每个段实际上是一个独立的哈希表，并拥有自己的锁。这里的Segment为了实现额可以单独加锁，继承了ReentrantLock，也就是说Segment本身就是一个ReentrantLock。

```java
static final class Segment<K,V> extends ReentrantLock implements Serializable {
    private static final long serialVersionUID = 2249069246763182397L;
    transient volatile HashEntry<K,V>[] table;
    transient int count;
    transient int modCount;
    transient int threshold;
    final float loadFactor;
}
```


在 Java 1.8 中，ConcurrentHashMap 的设计采用了锁细化的策略，其中每个节点（Node）可以单独上锁。那么，其实也可以像1.7一样，让Node实现ReentrantLock就行了，这样也能实现针对节点进行上锁。

但是为啥不这么做呢？明明ReentrantLock要比synchronized轻的多呀，性能也会更好一些呀。

抛开场景谈技术方案都是耍流氓，我们来看看JDK 1.8中锁的场景，是针对节点加锁的，并不是像1.7中基于段，那么也就意味着，这种情况下，并发冲突要小得多了。毕竟同一个hashMap中，同时去写同一个节点的概率还是很低的。所以，这种情况下，并发冲突并不高。

而这种并发冲突不高时，synchronized就不会频繁的升级为重量级锁，大部分情况下偏向锁和轻量级锁就搞定了。所以，这时候，synchronized和ReentrantLock相比，其实加锁的性能上的差别几乎可以忽略不计了；

那加锁性能差不多了，就来看看synchronized有啥优势吧。

首先就是**synchronized 无需手动锁管理**，编程模型更简单，因为开发者不需要明确地调用锁的获取和释放方法，这减少了因忘记释放锁导致的死锁风险。

其次，**synchronized则是JVM内置的语义，JVM能够在运行时作出相应的优化措施**，比如锁粗化、锁消除等。这些是ReentrantLock不具备的。

另外，**当获取锁获取失败时，synchronized会通过自旋避免线程被挂起**，而ReentrantLock 会导致线程挂起。而线程不需要挂起的话就**可以减少线程上下文切换的开销。**

并且，**不需要挂起，就意味着也不需要唤醒，所以synchronized的获取锁的效率也就会更高一些。**

还有就是，**ReentrantLock 是一个独立的对象**，而 synchronized 是利用对象头（Object Header）中的一部分位标记来实现的锁。对于 ReentrantLock，每次使用都需要实例化一个 ReentrantLock 对象。这个对象除了存储锁的状态外，还可能包含其他一些控制并发访问的状态信息，如持有锁的线程、等待队列等，并且还有AQS的支持中需要存储队列。**所以他的内存开销会更大一些。**
