# 典型回答

CountDownLatch、CyclicBarrier、Semaphore都是Java并发库中的**同步辅助类**，它们**都可以用来协调多个线程之间的执行**。

但是，它们三者之间还是有一些区别的：

**CountDownLatch是一个计数器**，它允许一个或多个线程等待其他线程完成操作。**它通常用来实现一个线程等待其他多个线程完成操作之后再继续执行的操作。**

**CyclicBarrier是一个同步屏障**，它允许多个线程相互等待，直到到达某个公共屏障点，才能继续执行。**它通常用来实现多个线程在同一个屏障处等待，然后再一起继续执行的操作。**

**Semaphore是一个计数信号量**，它允许多个线程同时访问共享资源，并通过计数器来控制访问数量。**它通常用来实现一个线程需要等待获取一个许可证才能访问共享资源，或者需要释放一个许可证才能完成操作的操作。**

CountDownLatch适用于一个线程等待多个线程完成操作的情况<br />CyclicBarrier适用于多个线程在同一个屏障处等待<br />Semaphore适用于一个线程需要等待获取许可证才能访问共享


使用CountDownLatch、CyclicBarrier、Semaphore实现线程协调：

[✅有三个线程T1,T2,T3如何保证顺序执行？](https://www.yuque.com/hollis666/fo22bm/wwqs6n658n4ip0ed?view=doc_embed&inner=zITf4)

