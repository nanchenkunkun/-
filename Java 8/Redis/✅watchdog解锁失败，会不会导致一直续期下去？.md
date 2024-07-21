# 典型回答

**不会的，因为在解锁过程中，不管是解锁失败了，还是解锁时抛了异常，都还是会把本地的续期任务停止，避免下次续期。**

具体实现如下：

```java
@Override
public void unlock() {
    try {
        get(unlockAsync(Thread.currentThread().getId()));
    } catch (RedisException e) {
        if (e.getCause() instanceof IllegalMonitorStateException) {
            throw (IllegalMonitorStateException) e.getCause();
        } else {
            throw e;
        }
    }
}
```
这是redisson中解锁方法的入口，这里调用了unlockAsync方法，传入了当前线程的ID

```java
@Override
public RFuture<Void> unlockAsync(long threadId) {
    return getServiceManager().execute(() -> unlockAsync0(threadId));
}

private RFuture<Void> unlockAsync0(long threadId) {
    CompletionStage<Boolean> future = unlockInnerAsync(threadId);
    CompletionStage<Void> f = future.handle((opStatus, e) -> {
        cancelExpirationRenewal(threadId);

        if (e != null) {
            if (e instanceof CompletionException) {
                throw (CompletionException) e;
            }
            throw new CompletionException(e);
        }
        if (opStatus == null) {
            IllegalMonitorStateException cause = new IllegalMonitorStateException("attempt to unlock lock, not locked by current thread by node id: "
                    + id + " thread-id: " + threadId);
            throw new CompletionException(cause);
        }

        return null;
    });

    return new CompletableFutureWrapper<>(f);
}
```

这里就是unlock的核心逻辑了。主要看两个关键步骤：

```java
CompletionStage<Boolean> future = unlockInnerAsync(threadId);
CompletionStage<Void> f = future.handle((opStatus, e) -> {
});
```

第一步是执行解锁的动作，第二部是执行解锁后的操作。注意看，这里用到了一个CompletionStage，并且通过handle方法进行了后续的操作。

> CompletionStage 是 Java 8 引入的一个接口，位于 java.util.concurrent 包中。它代表了一个异步操作的阶段，这个阶段在某个任务的计算完成时会执行。CompletionStage 提供了一种非阻塞的方式来处理一系列的异步操作步骤。每一个操作步骤都可以以 CompletionStage 的形式表示，这些步骤可以串行执行，也可以并行执行或者应用某种组合。通过这种方式，CompletionStage 提供了强大的异步编程模型，允许开发者以链式调用的方式来组织复杂的异步逻辑。（他其实是CompletableFuture的父类）


CompletionStage的handle 方法提供了一种机制来处理前一个阶段的结果或异常，无论该阶段是正常完成还是异常完成。他的方法签名如下：

```java
<T> CompletionStage<T> handle(BiFunction<? super T, Throwable, ? extends T> fn);
```

handle 方法接收一个 BiFunction，这个函数有两个参数：计算的结果（如果计算成功完成）和抛出的异常（如果计算失败）。这使得 handle 方法可以在一个地方同时处理操作的成功和失败情况。

- 如果前一个阶段成功完成，handle 方法中的函数将被调用，其中的异常参数（Throwable）将为 null，而结果参数将携带操作的结果。
- 如果前一个阶段失败或抛出异常，handle 方法同样会被调用，但这次结果参数将为 null，而异常参数将携带相应的异常信息。

那么也就是说，**CompletionStage 的 handle 方法允许你在前一个操作无论是成功完成、失败，还是抛出异常的情况下，都能够执行 handle 方法中定义的逻辑。**

**所以，不管上面的unlockInnerAsync过程中，解锁是否成功，是否因为网络原因等出现了异常，后续的代码都能正常执行。**那后续的代码是什么呢？

```java
CompletionStage<Void> f = future.handle((opStatus, e) -> {
    cancelExpirationRenewal(threadId);

    if (e != null) {
        if (e instanceof CompletionException) {
            throw (CompletionException) e;
        }
        throw new CompletionException(e);
    }
    if (opStatus == null) {
        IllegalMonitorStateException cause = new IllegalMonitorStateException("attempt to unlock lock, not locked by current thread by node id: "
                + id + " thread-id: " + threadId);
        throw new CompletionException(cause);
    }

    return null;
});

```

这段代码**一上来就调用了cancelExpirationRenewal**：

```java
protected void cancelExpirationRenewal(Long threadId) {
    ExpirationEntry task = EXPIRATION_RENEWAL_MAP.get(getEntryName());
    if (task == null) {
        return;
    }
    
    if (threadId != null) {
        task.removeThreadId(threadId);
    }

    if (threadId == null || task.hasNoThreads()) {
        Timeout timeout = task.getTimeout();
        if (timeout != null) {
            timeout.cancel();
        }
        EXPIRATION_RENEWAL_MAP.remove(getEntryName());
    }
}

```

逻辑挺简单的，就是从EXPIRATION_RENEWAL_MAP中把当前线程移除掉。

那么通过 [https://www.yuque.com/hollis666/fo22bm/fg0f0wh41g8eu5ik](https://www.yuque.com/hollis666/fo22bm/fg0f0wh41g8eu5ik) 我们知道，续期是需要依赖EXPIRATION_RENEWAL_MAP的，如果某个线程不在EXPIRATION_RENEWAL_MAP里面了，就不会再被续期了。

所以，如果解锁过程中失败了，redisson也能保证不会再被续期了。除非移除EXPIRATION_RENEWAL_MAP的这个动作也失败了，但是从本地的map中移除一个key失败的概率还是极低的。
