# 典型回答

[✅ConcurrentHashMap是如何保证线程安全的？](https://www.yuque.com/hollis666/fo22bm/seuqd9oynk2enp9t?view=doc_embed)

通过上文，你可以知道，在JDK 1.8中修改了ConcurrentHashMap中的加锁的策略。

在 Java 1.7 及之前版本中，`ConcurrentHashMap` 使用了一种称为分段锁的机制。这种机制的核心思想是将哈希表分成多个段（Segment），每个段实际上是一个独立的哈希表，并拥有自己的锁。通过这种方式，`ConcurrentHashMap` 减少了锁的粒度，从而允许多个写操作可以并发执行，只要它们操作的是不同的段。

这种分段锁相比Hashtable在并发度上有了很大的提高，因为每个段有自己的锁，不同的段可以并行更新。但是也不是完全没有限制。

**首先，虽然分段锁提高了并发性，但在段数固定的情况下，并发很高的时候仍可能导致热点段，从而成为性能瓶颈。另外，由于每个段都是独立的结构，这可能导致较高的内存占用。**

所以，JDK 1.8 对 `ConcurrentHashMap` 的实现进行了重大改进，**不再使用分段锁，而是采用了一种基于节点锁的方法，并且在内部大量使用了 CAS 操作来管理状态。**这种新的设计旨在提供更高的并发级别并减少锁的争用。

具体源码参考：

[✅ConcurrentHashMap是如何保证线程安全的？](https://www.yuque.com/hollis666/fo22bm/seuqd9oynk2enp9t?view=doc_embed)


JDK 1.8中的ConcurrentHashMap的加锁机制有以下特点：

- **更细的锁粒度**：通过对单个节点的锁定而不是整个段，大幅降低了锁的竞争。
- **CAS 操作**：对数据结构的很多更新操作使用无锁的 CAS 操作，提高了效率，尤其是在读多写少的场景下。
- **性能和扩展性**：Java 1.8 的实现在高并发环境下提供了更好的性能，特别是通过减少锁的竞争和提高数据结构的效率。
- **内存效率**：Java 1.8 的实现通过减少锁的数量和使用更简洁的数据结构，提高了内存效率。


# 扩展知识

## 为什么用synchronized而不是ReentrantLock

[✅ConcurrentHashMap为什么在JDK1.8中使用synchronized而不是ReentrantLock](https://www.yuque.com/hollis666/fo22bm/zfcsv292hkbiclzu?view=doc_embed)
