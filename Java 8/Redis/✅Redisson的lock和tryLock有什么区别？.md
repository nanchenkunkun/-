# 典型回答

当我们在使用Redisson实现分布式锁的时候，会经常用到lock和tryLock两个方法，那么他们有啥区别吗？

先来看看代码中是如何解释这两个方法的：

```
/**
 * Tries to acquire the lock with defined <code>leaseTime</code>.
 * Waits up to defined <code>waitTime</code> if necessary until the lock became available.
 *
 * Lock will be released automatically after defined <code>leaseTime</code> interval.
 *
 * @param waitTime the maximum time to acquire the lock
 * @param leaseTime lease time
 * @param unit time unit
 * @return <code>true</code> if lock is successfully acquired,
 *          otherwise <code>false</code> if lock is already set.
 * @throws InterruptedException - if the thread is interrupted
 */
boolean tryLock(long waitTime, long leaseTime, TimeUnit unit) throws InterruptedException;

/**
 * Acquires the lock with defined <code>leaseTime</code>.
 * Waits if necessary until lock became available.
 *
 * Lock will be released automatically after defined <code>leaseTime</code> interval.
 *
 * @param leaseTime the maximum time to hold the lock after it's acquisition,
 *        if it hasn't already been released by invoking <code>unlock</code>.
 *        If leaseTime is -1, hold the lock until explicitly unlocked.
 * @param unit the time unit
 *
 */
void lock(long leaseTime, TimeUnit unit);
```

这两个方法声明和注释上，主要由以下3个区别：

- tryLock
   - Tries to acquire the lock with defined leaseTime
   - Waits up to defined waitTime if necessary until the lock became available.
   - 返回值为boolean
- lock
   - Acquires the lock with defined leaseTime
   - Waits if necessary until lock became available.
   - 返回值为void

这其实已经把这个两个方法的区别描述的很清楚了。

那么，介绍一下就是**tryLock是尝试获取锁，如果能获取到直接返回true，如果无法获取到锁，他会按照我们指定的waitTime进行阻塞，在这个时间段内他还会再尝试获取锁。如果超过这个时间还没获取到则返回false。如果我们没有指定waitTime，那么他就在未获取到锁的时候，就直接返回false了。**

```
RLock lock = redisson.getLock("myLock");
boolean isLocked = lock.tryLock(); // 非阻塞方法，立即返回获取结果
if (isLocked) {
    try {
        // 执行临界区代码
    } finally {
        lock.unlock();
    }
} else {
    // 获取锁失败，处理逻辑
}

```


**lock的原理是以阻塞的方式去获取锁，如果获取锁失败会一直等待，直到获取成功。**

```
RLock lock = redisson.getLock("myLock");
lock.lock(); // 阻塞方法，直到获取到锁
try {
    // 执行代码
} finally {
    lock.unlock();
}

```


**所以，我们可以认为，lock实现的是一个阻塞锁，而tryLock实现的是一个非阻塞锁（在没有指定waitTime的情况下）。**



