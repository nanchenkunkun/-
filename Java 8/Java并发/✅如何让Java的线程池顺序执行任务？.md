# 典型回答

Java中的线程池本身并不提供内置的方式来保证任务的顺序执行的，因为线程池的设计目的是为了提高并发性能和效率，如果顺序执行的话，那就和单线程没区别了。

但是如果被问到想要实现这个功能该怎么做，有以下两种方式。

1、使用单线程线程池

我们可以使用SingleThreadExecutor这种线程池来执行任务，因为这个线程池中只有一个线程，所以他可以保证任务可以按照提交任务被顺序执行。

```java
ExecutorService executor = Executors.newSingleThreadExecutor();
executor.submit(task1);
executor.submit(task2);
executor.submit(task3);
// 任务按照提交的顺序逐个执行
executor.shutdown();

```


2、使用有依赖关系的任务调度方式

可以使用ScheduledThreadPoolExecutor结合ScheduledFuture来实现任务的顺序执行。将任务按照顺序提交给线程池，每个任务的执行时间通过ScheduledFuture的get()方法等待前一个任务完成。

```java
ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
ScheduledFuture<?> future1 = executor.schedule(task1, 0, TimeUnit.MILLISECONDS);
ScheduledFuture<?> future2 = executor.schedule(task2, future1.get(), TimeUnit.MILLISECONDS);
ScheduledFuture<?> future3 = executor.schedule(task3, future2.get(), TimeUnit.MILLISECONDS);
// 任务会按照依赖关系和前一个任务的执行时间逐个执行
executor.shutdown();
```
