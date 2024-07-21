# 典型回答

**在JDK 1.7中，ConcurrentHashMap使用了分段锁技术**，即将哈希表分成多个段，每个段拥有一个独立的锁。这样可以在多个线程同时访问哈希表时，只需要锁住需要操作的那个段，而不是整个哈希表，从而提高了并发性能。

虽然JDK 1.7的这种方式可以减少锁竞争，但是在高并发场景下，仍然会出现锁竞争，从而导致性能下降。

**在JDK 1.8中，ConcurrentHashMap的实现方式进行了改进，使用节点锁的思想，即采用“CAS+Synchronized”的机制来保证线程安全。**在JDK 1.8中，ConcurrentHashMap会在添加元素时，如果某个段为空，那么使用CAS操作来添加新节点；如果段不为空，使用Synchronized锁住当前段，再次尝试put。这样可以避免分段锁机制下的锁粒度太大，以及在高并发场景下，由于线程数量过多导致的锁竞争问题，提高了并发性能。

[✅ConcurrentHashMap为什么在JDK 1.8中废弃分段锁？](https://www.yuque.com/hollis666/fo22bm/gzavigfwro6fgs8o?view=doc_embed)

# 扩展知识

## 源码分析

ConcurrentHashMap将哈希表分成多个段，每个段拥有一个独立的锁，这样可以在多个线程同时访问哈希表时，只需要锁住需要操作的那个段，而不是整个哈希表，从而提高了并发性能。下面是ConcurrentHashMap中分段锁的代码实现：

```java
static final class Node<K,V> implements Map.Entry<K,V> {
    final int hash;
    final K key;
    volatile V val;
    volatile Node<K,V> next;

    Node(int hash, K key, V val, Node<K,V> next) {
        this.hash = hash;
        this.key = key;
        this.val = val;
        this.next = next;
    }

    // ...
}

static final class Segment<K,V> extends ReentrantLock implements Serializable {
    private static final long serialVersionUID = 2249069246763182397L;
    transient volatile HashEntry<K,V>[] table;
    transient int count;
    transient int modCount;
    transient int threshold;
    final float loadFactor;
}

```

在上面的代码中，我们可以看到，每个Segment都是ReentrantLock的实现，每个Segment包含一个HashEntry数组，每个HashEntry则包含一个key-value键值对。

接下来再看下在JDK 1.8中，下面是ConcurrentHashMap中CAS+Synchronized机制的代码实现：

```java
public V put(K key, V value) {
    return putVal(key, value, false);
}


public V putVal(K key, V value) {
    if (value == null)
        throw new NullPointerException();

    // 对 key 的 hashCode 进行扰动
    int hash = spread(key.hashCode());
    int binCount = 0;

    // 循环操作
    for (Node<K,V>[] tab = table;;) {
        Node<K,V> f; int n, i, fh;

        // 如果 table 为 null 或长度为 0，则进行初始化
        if (tab == null || (n = tab.length) == 0)
            tab = initTable();

            // 如果哈希槽为空，则通过 CAS 操作尝试插入新节点
        else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) {
            if (casTabAt(tab, i, null,
                         new Node<K,V>(hash, key, value, null)))
                break;
        }

            // 如果哈希槽处已经有节点，且 hash 值为 MOVED，则说明正在进行扩容，需要帮助迁移数据
        else if ((fh = f.hash) == MOVED)
            tab = helpTransfer(tab, f);

            // 如果哈希槽处已经有节点，且 hash 值不为 MOVED，则进行链表/红黑树的节点遍历或插入操作
        else {
            V oldVal = null;

            // 加锁，确保只有一个线程操作该节点的链表/红黑树
            synchronized (f) {
                if (tabAt(tab, i) == f) {
                    if (fh >= 0) {
                        // 遍历链表，找到相同 key 的节点，更新值或插入新节点
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
                                // 将新节点插入到链表末尾
                                if (casNext(pred, new Node<K,V>(hash, key,
                                                                value, null))) {
                                    break;
                                }
                            }
                        }
                    }
                        // 遍历红黑树，找到相同 key 的节点，更新值或插入新节点
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
                }
            }
            // 如果插入或更新成功，则进行可能的红黑树化操作
            if (binCount != 0) {
                if (binCount >= TREEIFY_THRESHOLD)
                    treeifyBin(tab, i);
                // 如果替换旧值成功，则返回旧值
                if (oldVal != null)
                    return oldVal;
                break;
            }
        }
    }
    //

    addCount(1L, binCount);
    return null;
}
```

**在上述代码中，如果某个段为空，那么使用CAS操作来添加新节点；如果某个段中的第一个节点的hash值为MOVED，表示当前段正在进行扩容操作，那么就调用helpTransfer方法来协助扩容；否则，使用Synchronized锁住当前节点，然后进行节点的添加操作。**
