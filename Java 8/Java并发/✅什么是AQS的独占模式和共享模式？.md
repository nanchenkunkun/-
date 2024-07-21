# 典型回答

AbstractQueuedSynchronizer（AQS）是Java并发包中的一个核心框架，用于构建锁和其他同步组件。

[✅如何理解AQS？](https://www.yuque.com/hollis666/fo22bm/qka9yt?view=doc_embed)

**它提供了一套基于FIFO队列的同步器框架，并支持独占模式和共享模式，这两种模式是用于实现同步组件的关键。**

- 独占模式意味着一次只有一个线程可以获取同步状态。这种模式通常用于实现互斥锁，如ReentrantLock。
- 共享模式允许多个线程同时获取同步状态。这种模式通常用于实现如信号量（Semaphore）和读写锁（ReadWriteLock的读锁）等同步组件。

AQS的各种实现类中，要么是基于独占模式实现的， 要么是基于共享模式实现的。

在AQS中提供了很多和锁操作相关的方法，如：

- tryAcquire、tryRelease、acquire、release等。
- tryAcquireShared、tryReleaseShared、releaseShared、acquireShared等。

如果是独占模式，则需要实现tryAcquire、tryRelease、acquire、release等方法，如ReentrantLock：

![image.png](https://cdn.nlark.com/yuque/0/2024/png/5378072/1706941054967-75daa8c3-92d1-4c20-8438-34bf758b83be.png#averageHue=%23f6f7f8&clientId=ube3f0db6-5afd-4&from=paste&height=424&id=ua003f48a&originHeight=424&originWidth=1375&originalType=binary&ratio=1&rotation=0&showTitle=false&size=71682&status=done&style=none&taskId=uf651803a-8c58-4fbf-96dd-03197b31358&title=&width=1375)

![image.png](https://cdn.nlark.com/yuque/0/2024/png/5378072/1706941064938-8a217efd-4422-4607-a081-a674e79589c7.png#averageHue=%23f5f6f8&clientId=ube3f0db6-5afd-4&from=paste&height=436&id=ua8559f6a&originHeight=436&originWidth=1379&originalType=binary&ratio=1&rotation=0&showTitle=false&size=73046&status=done&style=none&taskId=u444802d3-92f7-4a9d-a76a-36369553066&title=&width=1379)

如Semaphore则是实现了tryAcquireShared、tryReleaseShared等方法：

![image.png](https://cdn.nlark.com/yuque/0/2024/png/5378072/1706941098539-acd4a92e-ae2f-4649-a086-ac0d6fd4eae2.png#averageHue=%23fefefe&clientId=ube3f0db6-5afd-4&from=paste&height=577&id=u114d9d6a&originHeight=577&originWidth=1393&originalType=binary&ratio=1&rotation=0&showTitle=false&size=80904&status=done&style=none&taskId=uc9a15406-80c9-4f37-a722-a80368d44f8&title=&width=1393)

其中也有tryAcquire的实现，但是也是调用了tryAcquireShared来实现的：

![image.png](https://cdn.nlark.com/yuque/0/2024/png/5378072/1706941185066-7229f58a-95ef-421e-9b31-3e3da71e809e.png#averageHue=%23f2e4bb&clientId=ube3f0db6-5afd-4&from=paste&height=666&id=u0830b18f&originHeight=666&originWidth=1377&originalType=binary&ratio=1&rotation=0&showTitle=false&size=118759&status=done&style=none&taskId=u3cf6be3b-e0e4-49e9-84db-97ec987b096&title=&width=1377)

### 
在独占模式中，状态通常表示是否被锁定（0表示未锁定，1表示锁定）。在共享模式中，状态可以表示可用的资源数量。

**当需要保证某个资源或一段代码在同一时间内只能被一个线程访问时，独占模式是最合适的选择。如我们经常用的ReentrantLock和ReadWriteLock中的写锁。**

**当资源或数据主要被多个线程读取，而写操作相对较少时，共享模式能够提高并发性能。如我们经常使用的Semaphore和CountDownLatch，用来多个线程控制共享资源的。还有ReadWriteLock中的读锁允许多个线程同时读取数据，只要没有线程在写入数据。**
