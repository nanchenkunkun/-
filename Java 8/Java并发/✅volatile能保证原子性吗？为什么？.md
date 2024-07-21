# 典型回答
volatile通常被比喻成”轻量级的synchronized“，也是Java并发编程中比较重要的一个关键字。和synchronized不同，volatile是一个变量修饰符，只能用来修饰变量。无法修饰方法及代码块等。

volatile的用法比较简单，只需要在声明一个可能被多线程同时访问的变量时，使用volatile修饰就可以了。

但是， volatile在线程安全方面，可以保证有序性和可见性，但是是不能保证原子性的 。

synchronized可以保证原子性 ，因为被synchronized修饰的代码片段，在进入之前加了锁，只要他没执行完，其他线程是无法获得锁执行这段代码片段的，就可以保证他内部的代码可以全部被执行。进而保证原子性。

那么，**为什么volatile不能保证原子性呢？因为他不是锁，他没做任何可以保证原子性的处理。当然就不能保证原子性了。**


比如有如下代码：

```java
public class Test {

    volatile int number = 0;

    public void increase() {
        number++;
    }

    public static void main(String[] args) {
        Test volatileAtomDemo = new Test();
        for (int j = 0; j < 10; j++) {
            new Thread(() -> {
                for (int i = 0; i < 1000; i++) {
                    volatileAtomDemo.increase();
                }
            }, String.valueOf(j)).start();
        }
        while (Thread.activeCount() > 2) {
            Thread.yield();
        }

        System.out.println(Thread.currentThread().getName() +
                           " final number result = " + volatileAtomDemo.number);
    }

}
```

> Thread.yield()方法被用来暂停当前正在执行的线程，以允许其他线程获得执行机会，以此来提高并发竞争。


以上程序，正常情况下，输出结果应该是10000，但是真正执行的话就会发现，没办法保证每次执行结果都是10000，这就是因为i++这个操作没办法保证他是原子性的。<br />i++被拆分成3个指令：

- 执行GETFIELD拿到主内存中的原始值number。
- 执行IADD进行加1操作。
- 执行PUTFIELD把工作内存中的值写回主内存中。
- <br />

当多个线程并发执行PUTFIELD指令的时候，会出现写回主内存覆盖问题，所以才会导致最终结果不为 10000，所以 volatile 不能保证原子性。

# 扩展知识

## 如何实现原子性的自增

因为volatile没办法保证原子性，在并发场景中的i++方法会有并发问题，那么有一种解决方案就是使用支持原子性操作的数字类型。如AtomicLong、AtomicInteger等。


[✅LongAdder和AtomicLong的区别？](https://www.yuque.com/hollis666/fo22bm/dhzyrg?view=doc_embed)
