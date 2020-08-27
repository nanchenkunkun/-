# BlockingQueue深入解析

本篇将详细介绍BlockingQueue，以下是涉及的主要内容：

- BlockingQueue的核心方法

- 阻塞队列的成员的概要介绍

- 详细介绍DelayQueue、ArrayBlockingQueue、LinkedBlockingQueue的原理

- 线程池与BlockingQueue

  

**1、初识阻塞队列**

​		在新增的Concurrent包中，BlockingQueue很好的解决了多线程中，如何高效安全“传输”数据的问题。通过这些高效并且线程安全的队列类，为我们快速搭建高质量的多线程程序带来极大的便利。本文详细介绍了BlockingQueue家庭中的所有成员，包括他们各自的功能以及常见使用场景。

BlockingQueue的核心方法：

```json
public interface BlockingQueue<E> extends Queue<E> {

    //将给定元素设置到队列中，如果设置成功返回true, 否则返回false。如果是往限定了长度的队列中设置值，推荐使用offer()方法。
    boolean add(E e);

    //将给定的元素设置到队列中，如果设置成功返回true, 否则返回false. e的值不能为空，否则抛出空指针异常。
    boolean offer(E e);

    //将元素设置到队列中，如果队列中没有多余的空间，该方法会一直阻塞，直到队列中有多余的空间。
    void put(E e) throws InterruptedException;

    //将给定元素在给定的时间内设置到队列中，如果设置成功返回true, 否则返回false.
    boolean offer(E e, long timeout, TimeUnit unit)
        throws InterruptedException;

    //从队列中获取值，如果队列中没有值，线程会一直阻塞，直到队列中有值，并且该方法取得了该值。
    E take() throws InterruptedException;

    //在给定的时间里，从队列中获取值，时间到了直接调用普通的poll方法，为null则直接返回null。
    E poll(long timeout, TimeUnit unit)
        throws InterruptedException;

    //获取队列中剩余的空间。
    int remainingCapacity();

    //从队列中移除指定的值。
    boolean remove(Object o);

    //判断队列中是否拥有该值。
    public boolean contains(Object o);

    //将队列中值，全部移除，并发设置到给定的集合中。
    int drainTo(Collection<? super E> c);

    //指定最多数量限制将队列中值，全部移除，并发设置到给定的集合中。
    int drainTo(Collection<? super E> c, int maxElements);
}
```

在深入之前先了解下下ReentrantLock 和 Condition:
重入锁ReentrantLock:
ReentrantLock锁在同一个时间点只能被一个线程锁持有；而可重入的意思是，ReentrantLock锁，可以被单个线程多次获取。
ReentrantLock分为“公平锁”和“非公平锁”。它们的区别体现在获取锁的机制上是否公平。“锁”是为了保护竞争资源，防止多个线程同时操作线程而出错，ReentrantLock在同一个时间点只能被一个线程获取(当某线程获取到“锁”时，其它线程就必须等待)；ReentraantLock是通过一个FIFO的等待队列来管理获取该锁所有线程的。**在“公平锁”的机制下，线程依次排队获取锁；而“非公平锁”在锁是可获取状态时，不管自己是不是在队列的开头都会获取锁。**
主要方法：

- lock()获得锁
- lockInterruptibly()获得锁，但优先响应中断
- tryLock()尝试获得锁，成功返回true,否则false，该方法不等待，立即返回
- tryLock(long time,TimeUnit unit)在给定时间内尝试获得锁
- unlock()释放锁

Condition：await()、signal()方法分别对应之前的Object的wait()和notify()

- 和重入锁一起使用
- await()是当前线程等待同时释放锁
- awaitUninterruptibly()不会在等待过程中响应中断
- signal()用于唤醒一个在等待的线程，还有对应的singalAll()方法

**2、阻塞队列的成员**

| 队列                  | 有界性             | 锁   | 数据结构   |
| :-------------------- | :----------------- | :--- | ---------- |
| ArrayBlockingQueue    | bounded(有界)      | 加锁 | arrayList  |
| LinkedBlockingQueue   | optionally-bounded | 加锁 | linkedList |
| PriorityBlockingQueue | unbounded          | 加锁 | heap       |
| DelayQueue            | unbounded          | 加锁 | heap       |
| SynchronousQueue      | bounded            | 加锁 | 无         |
| LinkedTransferQueue   | unbounded          | 加锁 | heap       |
| LinkedBlockingDeque   | unbounded          | 无锁 | heap       |

下面分别简单介绍一下：

​	ArrayBlockingQueue：是一个用数组实现的有界阻塞队列，此队列按照先进先出（FIFO）的原则对元素进行排序。支持公平锁和非公平锁。【注：每一个线程在获取锁的时候可能都会排队等待，如果在等待时间上，先获取锁的线程的请求一定先被满足，那么这个锁就是公平的。反之，这个锁就是不公平的。公平的获取锁，也就是当前等待时间最长的线程先获取锁】

​	LinkedBlockingQueue：一个由链表结构组成的有界队列，此队列的长度为Integer.MAX_VALUE。此队列按照先进先出的顺序进行排序。

​	PriorityBlockingQueue： 一个支持线程优先级排序的无界队列，默认自然序进行排序，也可以自定义实现compareTo()方法来指定元素排序规则，不能保证同优先级元素的顺序。

​	DelayQueue： 一个实现PriorityBlockingQueue实现延迟获取的无界队列，在创建元素时，可以指定多久才能从队列中获取当前元素。只有延时期满后才能从队列中获取元素。（DelayQueue可以运用在以下应用场景：1.缓存系统的设计：可以用DelayQueue保存缓存元素的有效期，使用一个线程循环查询DelayQueue，一旦能从DelayQueue中获取元素时，表示缓存有效期到了。2.定时任务调度。使用DelayQueue保存当天将会执行的任务和执行时间，一旦从DelayQueue中获取到任务就开始执行，从比如TimerQueue就是使用DelayQueue实现的。）

​	SynchronousQueue： 一个不存储元素的阻塞队列，每一个put操作必须等待take操作，否则不能添加元素。支持公平锁和非公平锁。SynchronousQueue的一个使用场景是在线程池里。Executors.newCachedThreadPool()就使用了SynchronousQueue，这个线程池根据需要（新任务到来时）创建新的线程，如果有空闲线程则会重复使用，线程空闲了60秒后会被回收。

​	LinkedTransferQueue： 一个由链表结构组成的无界阻塞队列，相当于其它队列，LinkedTransferQueue队列多了transfer和tryTransfer方法。

​	LinkedBlockingDeque： 一个由链表结构组成的双向阻塞队列。队列头部和尾部都可以添加和移除元素，多线程并发时，可以将锁的竞争最多降到一半。

**3、阻塞队列原理以及使用**

#### (1)DelayQueue

DelayQueue的泛型参数需要实现Delayed接口，Delayed接口继承了Comparable接口，DelayQueue内部使用非线程安全的优先队列（PriorityQueue），并使用Leader/Followers模式，最小化不必要的等待时间。DelayQueue不允许包含null元素。

Leader/Followers模式：

1. 有若干个线程(一般组成线程池)用来处理大量的事件
2. 有一个线程作为领导者，等待事件的发生；其他的线程作为追随者，仅仅是睡眠。
3. 假如有事件需要处理，领导者会从追随者中指定一个新的领导者，自己去处理事件。
4. 唤醒的追随者作为新的领导者等待事件的发生。
5. 处理事件的线程处理完毕以后，就会成为追随者的一员，直到被唤醒成为领导者。
6. 假如需要处理的事件太多，而线程数量不够(能够动态创建线程处理另当别论)，则有的事件可能会得不到处理。

所有线程会有三种身份中的一种：leader和follower，以及一个干活中的状态：proccesser。它的基本原则就是，永远最多只有一个leader。而所有follower都在等待成为leader。线程池启动时会自动产生一个Leader负责等待网络IO事件，当有一个事件产生时，Leader线程首先通知一个Follower线程将其提拔为新的Leader，然后自己就去干活了，去处理这个网络事件，处理完毕后加入Follower线程等待队列，等待下次成为Leader。这种方法可以增强CPU高速缓存相似性，及消除动态内存分配和线程间的数据交换。
参数以及构造函数：

```java
   // 可重入锁
    private final transient ReentrantLock lock = new ReentrantLock();
    
    // 存储队列元素的队列——优先队列
    private final PriorityQueue<E> q = new PriorityQueue<E>();

    //用于优化阻塞通知的线程元素leader，Leader/Followers模式
    private Thread leader = null;

    //用于实现阻塞和通知的Condition对象
    private final Condition available = lock.newCondition();
    
    public DelayQueue() {}
    
    public DelayQueue(Collection<? extends E> c) {
        this.addAll(c);
    }
```

先看offer()方法：

```java
    public boolean offer(E e) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            q.offer(e);
            // 如果原来队列为空，重置leader线程，通知available条件
            if (q.peek() == e) {
                leader = null;
                available.signal();
            }
            return true;
        } finally {
            lock.unlock();
        }
    }

    //因为DelayQueue不限制长度，因此添加元素的时候不会因为队列已满产生阻塞，因此带有超时的offer方法的超时设置是不起作用的
    public boolean offer(E e, long timeout, TimeUnit unit) {
        // 和不带timeout的offer方法一样
        return offer(e);
    }
```

普通的poll()方法：如果延迟时间没有耗尽的话，直接返回null：

```java
    public E poll() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            E first = q.peek();
            if (first == null || first.getDelay(TimeUnit.NANOSECONDS) > 0)
                return null;
            else
                return q.poll();
        } finally {
            lock.unlock();
        }
    }
```

再看看take()方法：

```java
 public E take() throws InterruptedException {
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            for (;;) {
                // 如果队列为空，需要等待available条件被通知
                E first = q.peek();
                if (first == null)
                    available.await();
                else {
                    long delay = first.getDelay(TimeUnit.NANOSECONDS);
                    // 如果延迟时间已到，直接返回第一个元素
                    if (delay <= 0)
                        return q.poll();
                    // leader线程存在表示有其他线程在等待，那么当前线程肯定需要等待
                    else if (leader != null)
                        available.await();
                    else {
                        Thread thisThread = Thread.currentThread();
                        leader = thisThread;
                        // 如果没有leader线程，设置当前线程为leader线程
                        // 尝试等待直到延迟时间耗尽（可能提前返回，那么下次
                        // 循环会继续处理）
                        try {
                            available.awaitNanos(delay);
                        } finally {
                            // 如果leader线程还是当前线程，重置它用于下一次循环。
                            // 等待available条件时，锁可能被其他线程占用从而导致
                            // leader线程被改变，所以要检查
                            if (leader == thisThread)
                                leader = null;
                        }
                    }
                }
            }
        } finally {
            // 如果没有其他线程在等待，并且队列不为空，通知available条件
            if (leader == null && q.peek() != null)
                available.signal();
            lock.unlock();
        }
    }
```

最后看看带有timeout的poll方法:

```java
 public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        final ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            for (;;) {
                E first = q.peek();
                if (first == null) {
                    if (nanos <= 0)
                        return null;
                    else
                        // 尝试等待available条件，记录剩余的时间
                        nanos = available.awaitNanos(nanos);
                } else {
                    long delay = first.getDelay(TimeUnit.NANOSECONDS);
                    if (delay <= 0)
                        return q.poll();
                    if (nanos <= 0)
                        return null;
                    // 当leader线程不为空时（此时delay>=nanos），等待的时间
                    // 似乎delay更合理，但是nanos也可以，因为排在当前线程前面的
                    // 其他线程返回时会唤醒available条件从而返回，
                    if (nanos < delay || leader != null)
                        nanos = available.awaitNanos(nanos);
                    else {
                        Thread thisThread = Thread.currentThread();
                        leader = thisThread;
                        try {
                            long timeLeft = available.awaitNanos(delay);
                            // nanos需要更新
                            nanos -= delay - timeLeft;
                        } finally {
                            if (leader == thisThread)
                                leader = null;
                        }
                    }
                }
            }
        } finally {
            if (leader == null && q.peek() != null)
                available.signal();
            lock.unlock();
        }
    }
```

