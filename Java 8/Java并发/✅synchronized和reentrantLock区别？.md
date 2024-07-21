# 典型回答
ReentrantLock 和 synchronized 都是用于线程的同步控制，但它们在功能上来说差别还是很大的。对比下来 ReentrantLock 功能明显要丰富的多。

二者相同点是，都是可重入锁。二者也有很多不同，如：

- synchronized是Java内置特性，而ReentrantLock是通过Java代码实现的。
- **synchronized是可以自动获取/释放锁的**，但是**ReentrantLock需要手动获取/释放锁**。
- ReentrantLock还具有响应中断、超时等待等特性。
- **ReentrantLock可以实现公平锁和非公平锁**，而**synchronized只是非公平锁**。（[✅sychronized是非公平锁，那么是如何体现的？](https://www.yuque.com/hollis666/fo22bm/ihq8bdg4q3ts8mpo?view=doc_embed) ）

另外，随着JDK21的发布，虚拟线程已经推出，在虚拟线程中，不建议使用synchronized，而是建议用ReentrantLock。

[✅为什么虚拟线程不能用synchronized？](https://www.yuque.com/hollis666/fo22bm/cdp5h287x61w7uyc?view=doc_embed)

# 扩展知识

## ReentrantLock用法

Java语言直接提供了synchronized关键字用于加锁，但这种锁一是很重，二是获取时必须一直等待，没有额外的尝试机制。

java.util.concurrent.locks包提供的ReentrantLock用于替代synchronized加锁，ReentrantLock 内部是基于 AbstractQueuedSynchronizer（简称AQS）实现的。

ReentrantLock是可重入锁，它和synchronized一样，一个线程可以多次获取同一个锁。

用法：
```
public class Counter {
	private final Lock lock = new ReentrantLock();
	private int count;
	public void add(int n) {
		lock.lock();
		try {
			count += n;
		} finally {
			lock.unlock();
		}
	}
}
```

### 怎么创建公平锁？

new ReentrantLock() 默认创建的为非公平锁，如果要创建公平锁可以使用 new ReentrantLock(true)。
### lock() 和 lockInterruptibly() 的区别

lock() 和 lockInterruptibly() 的区别在于获取锁的途中如果所在的线程中断，lock() 会忽略异常继续等待获取锁，而 lockInterruptibly() 则会抛出 InterruptedException 异常。

### tryLock() 

tryLock(5, TimeUnit.SECONDS) 表示获取锁的最大等待时间为 5 秒，期间会一直尝试获取，而不是等待 5 秒之后再去获取锁。

## ReentrantLock 如何实现可重入的

可重入锁指的是同一个线程中可以多次获取同一把锁。比如在JAVA中，当一个线程调用一个对象的加锁的方法后,还可以调用其他加同一把锁的方法，这就是可重入锁。

ReentrantLock 加锁的时候，看下当前持有锁的线程和当前请求的线程是否是同一个，一样就可重入了。 只需要简单得将state值加1，记录当前线程的重入次数即可。

```
if (current == getExclusiveOwnerThread()) {
     int nextc = c + acquires;
     if (nextc < 0)
     	throw new Error("Maximum lock count exceeded");
     setState(nextc);
     return true;
 }
```

同时，在锁进行释放的时候，需要确保state=0的时候才能执行释放资源的动作，也就是说，一个可重入锁，重入了多少次，就得解锁多少次。

```
protected final boolean tryRelease(int releases) {
    int c = getState() - releases;
    if (Thread.currentThread() != getExclusiveOwnerThread())
        throw new IllegalMonitorStateException();
    boolean free = false;
    if (c == 0) {
        free = true;
        setExclusiveOwnerThread(null);
    }
    setState(c);
    return free;
}
```
