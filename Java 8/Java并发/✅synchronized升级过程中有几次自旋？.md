# 典型回答

基于Open JDK 8中源码显示，synchronized升级过程中有2次自旋。

**第一次自旋**<br />第一次自旋发生在 synchronized 获取轻量级锁时，即当一个线程尝试获取一个被其他线程持有的轻量级锁时，它会自旋等待锁的持有者释放锁。

在OpenJDK 8中，轻量级锁的自旋默认是开启的，最多自旋15次，每次自旋的时间逐渐延长。如果15次自旋后仍然没有获取到锁，就会升级为重量级锁。

代码的具体实现在 ObjectSynchronizer::fast_enter() 函数中，有如下代码：

```
// try to obtain the lock by a fast-path CAS
if (Atomic::cmpxchg_ptr(lock, obj->mark_addr(), mark) == mark) {
  TEVENT (fast_enter: release stacklock) ;
  return;
}

```

如果 CAS 操作成功，则表示获取锁成功，函数直接返回。否则，进入自旋。

**第二次自旋**<br />第二次自旋发生在 synchronized 轻量级锁升级到重量级锁的过程中。即当一个线程尝试获取一个被其他线程持有的重量级锁时，它会自旋等待锁的持有者释放锁。

在OpenJDK 8中，默认情况下不会开启重量级锁自旋。如果线程在尝试获取重量级锁时，发现该锁已经被其他线程占用，那么线程会直接阻塞，等待锁被释放。如果锁被持有时间很短，可以考虑开启重量级锁自旋，避免线程挂起和恢复带来的性能损失。

第二次自旋的代码实现在 ObjectSynchronizer::slow_enter() 函数中，有如下代码：

```
// Anticipate successful CAS -- the ST of the displaced mark must
// be visible <= the ST performed by the CAS.
lock->set_displaced_header(mark);
if (mark == (markOop) Atomic::cmpxchg_ptr(lock, obj->mark_addr(), mark)) {
  TEVENT (slow_enter: release stacklock) ;
  return ;
}

```

**自适应自旋**<br />在JDK6中之后的版本中，JVM引入了**自适应的自旋**机制。该机制通过监控轻量级锁自旋等待的情况，动态调整自旋等待的时间。

如果自旋等待的时间很短，说明锁的竞争不激烈，当前线程可以自旋等待一段时间，避免线程挂起和恢复带来的性能损失。如果自旋等待的时间较长，说明锁的竞争比较激烈，当前线程应该及时释放CPU资源，让其他线程有机会执行。

自适应的自旋实现在ObjectSynchronizer::FastHashCode()函数中。该函数会根据轻量级锁自旋等待的情况，调整自旋等待的时间。

# 扩展知识

## 自旋的实现

在OpenJDK 8的源码中，synchronized的升级过程中涉及到了多次自旋操作，其中包括：

1. 第一次自旋：在尝试获取轻量级锁失败后，线程会进行自旋，使用CAS操作去尝试获取锁。这里的自旋并没有使用while循环，而是使用了C++的inline函数，如ObjectSynchronizer::FastLock()。

2. 第二次自旋：在尝试获取重量级锁失败后，线程会进行自旋，等待拥有锁的线程释放锁。这里的自旋同样使用了C++的inline函数，如ObjectSynchronizer::FastUnlock()。

3. 自适应自旋：在尝试获取轻量级锁时，线程会进行自旋，等待拥有锁的线程释放锁。但这里的自旋不是固定次数的，而是根据前一次自旋的时间和成功获取锁的概率进行自适应调整。这里的自旋实现在C++的Thread.inline.hpp中，如Thread::SpinPause()。

需要注意的是，虽然这些自旋操作并没有使用while循环实现，但其本质上都是在不断尝试获取锁或等待锁的过程中循环执行的。这些循环操作使用的是各种内建函数、指令集和C++的语法特性实现，能够更高效地执行自旋操作，从而提升锁的性能。
