# 典型回答

想要保证多线程情况下，i++的正确性，需要考虑可见性、原子性及有序性。

在并发编程中，我们能用到的并发工具无非就是synchronized，volatile，reentrantLock以及并发工具类如AtomicInteger等。

这里面，除了volatile不可以以外（因为他没办法保证原子性），其他几种方式都可以。

[✅volatile能保证原子性吗？为什么？](https://www.yuque.com/hollis666/fo22bm/aylaul?view=doc_embed)

使用 AtomicInteger 类：

```java
private static AtomicInteger i = new AtomicInteger(0);

public static void increment() {
    i.incrementAndGet();
}
```

使用synchronized：

```java
public class HollisTest{
    private static int i = 0;

    public void increment() {
        synchronized (HollisTest.class) {
            i++;
        }
    }
}

```

使用reentrantLock：

```java
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class HollisTest {
    private int i = 0;
    private Lock lock = new ReentrantLock();

    public void increment() {
        lock.lock();
        try {
            i++;
        } finally {
            lock.unlock();
        }
    }
}

```
