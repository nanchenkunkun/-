# 典型回答

Java线程出现异常，如果这里的异常是我们认知中的Exception的话，JVM进程其实是不会退出的。

因为Java本身就是支持多线程的，每个Java线程都是相对独立的执行单元，每个线程是独立的执行上下文，异常只会影响抛出异常的线程。所以当一个线程抛出异常时，只会影响到该线程本身。其他线程将继续执行，不受异常的影响。

而且，在Java中，我们是可以自己主动的通过异常处理机制来捕获和处理异常的。如果在线程的代码中使用try-catch块来捕获异常，并在catch块中处理异常，那么异常不会传播到线程的外部，也不会影响整个进程的执行。

即使有的异常我们并没有捕获，Java也认为这些异常并不是特别严重（因为严重的话就不是异常，而是ERROR了），所以JVM并不会因为一个线程的异常就直接把JVM进程直接退出。

# 扩展知识

## 多线程异常处理

[✅为什么不能在try-catch中捕获子线程的异常？](https://www.yuque.com/hollis666/fo22bm/dtci5npzb1cidzxk?view=doc_embed)

## OOM与JVM退出

[✅Java发生了OOM一定会导致JVM 退出吗？](https://www.yuque.com/hollis666/fo22bm/fsnk2a6xdyhqfvf7?view=doc_embed)

