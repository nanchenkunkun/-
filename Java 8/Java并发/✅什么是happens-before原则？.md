# 典型回答
在关于JMM的介绍中，我们知道，JMM 是一种规范，它提供了一系列的机制来保证跨线程的内存可见性、有序性和原子性。

我们之前介绍过很多保证可见性的关键字，如volatile和synchronized等，其实，volatile和synchronized为啥可以保证可见性，也正是因为他们遵守了一个重要的happens-before原则（后文会介绍，Monitor Lock 和Volatile Variable 是happens - before中重要的两个原则）。

我们之前还介绍过一个原则，叫做as-if-serial，他意思指：不管怎么重排序，单线程程序的执行结果都不能被改变。编译器和处理器无论如何优化，都必须遵守as-if-serial语义。

[✅synchronized是如何保证原子性、可见性、有序性的？](https://www.yuque.com/hollis666/fo22bm/qw9x0lgisg4q18t6?view=doc_embed)

这个as-if-serial语义是针对单线程的，但是如果在多线程情况下呢？有没有什么原则可以保证有序性呢？这就需要我们的happens-before原则了。

happens-before原则是一种用于描述多线程程序中操作执行顺序的规则。它是Java内存模型（Java Memory Model，JMM）的一部分：**如果一个操作 A “happen-before” 另一个操作 B，那么 A 的结果对 B 是可见的。这个概念是理解线程间内存可见性的关键。**

**举一个例子，如以下代码：**

```

public class ThreadStartExample {
    private int startValue = 10;

    public void startNewThread() {
        startValue +=1；
        new Thread(() -> {
            int localValue = startValue;  
        }).start();
    }
}
```

有两个线程，一个写startValue，一个读startValue，但是我们并没有用synchronized加锁，也没有用volatile修饰，那么，JVM是如何保证，在主线程中修改startValue的操作在子线程中是可见的呢？

这其实就是happens-before原则发挥的作用了。但是，happens-before原则也不是没有任何限制，任何场景都能happens-before的，还是有一些规则要求的。我们接下来介绍下每个规则以及附上一些代码演示

### happends-before规则

以下十几个happends-before原则的适用场景（节选自《深入理解Java虚拟机》，并做了一些描述上的修改，增加了代码示例，方便大家理解）

- 程序次序规则（Program Order Rule）：在单个线程内，按照程序代码的顺序，前一个操作 happens-before 后一个操作。

```
public class ProgramOrderExample {
    private int a = 0;
    private int b = 0;

    public void method() {
        a = 1;  // 操作1
        b = 2;  // 操作2，
        // 操作1 `happens-before` 操作2
    }
}

```

- 管程锁定规则（Monitor Lock Rule）：对一个锁的解锁 happens-before 随后对这个锁的加锁。即在 synchronized 代码块或方法中，释放锁之前的所有操作对于下一个获取这个锁的线程是可见的。

```
public class MonitorLockExample {
    private int value = 0;
    private final Object lock = new Object();

    public void increment() {
        synchronized(lock) {
            value++;  // 操作在锁内
        }
    }

    public int getValue() {
        synchronized(lock) {
            return value;  // 此操作 `happens-before` increment() 中的操作
        }
    }
}
```

increment 方法中对 value 的修改，在 getValue 方法获取锁之后是可见的。

- volatile 变量规则（Volatile Variable Rule）：对一个 volatile 字段的写操作 happens-before 任意后续对这个字段的读操作。即确保 volatile 变量的写操作对其他线程立即可见。

```
public class VolatileExample {
    private volatile boolean flag = false;

    public void writeFlag() {
        flag = true;  // volatile 写操作
    }

    public boolean checkFlag() {
        return flag;  // 这里读取到的 flag 值 `happens-before` 写操作
    }
}
```

当一个线程调用 writeFlag()，另一个线程随后调用 checkFlag() 将看到 flag 为 true。

- 线程启动规则（Thread Start Rule）：对线程的 start() 方法的调用 happens-before 该线程的每个动作。确保线程启动时，主线程中对共享变量的写操作对于新线程是可见的。

```
public class ThreadStartExample {
    private int startValue = 10;

    public void startNewThread() {
        startValue +=1；
        new Thread(() -> {
            int localValue = startValue;  // startValue 的值 `happens-before` 这里的读操作
            // 处理 localValue
        }).start();
    }
}

```

线程启动时，将看到 startValue 的值为 11。

- 线程终止规则（Thread Termination Rule）：一个线程的所有操作 happens-before 对这个线程的 join() 方法的成功返回。确保线程终止时，该线程中的所有操作对于调用 join() 方法的线程是可见的。

```
public class ThreadJoinExample {
    private int counter = 0;

    public void incrementInThread() throws InterruptedException {
        Thread thread = new Thread(() -> {
            counter++;
        });
        thread.start();
        thread.join();  // `happens-before` counter 的读操作
        int value = counter;  // 这里能够看到线程中对 counter 的修改
    }
}

```

主线程中可以看到子线程对counter的修改

- 线程中断规则（Thread Interruption Rule）： 对线程的 interrupt() 方法的调用 happens-before 被中断线程检测到中断事件的发生。即线程的中断操作在被该线程检测到之前已经发生。

- 对象终结规则（Finalizer Rule）： 一个对象的初始化完成（构造函数执行结束）happens-before 它的 finalize() 方法的开始。即在对象被回收前，其构造过程已经完全结束。

```
public class FinalizerExample {
    private int value;

    public FinalizerExample() {
        value = 10;  // 构造函数中的操作
    }

    @Override
    protected void finalize() {
        // 此方法 `happens-before` 构造函数中的操作
        if (value != 10) {
            // 这里不应该发生
        }
    }
}

```

- 传递性（Transitivity）：如果操作 A 先行发生于操作 B，操作 B 先行发生于操作 C，那就可以得出操作 A 先行发生于操作 C 的结论。


```
public class TransitivityExample {
    private volatile boolean ready;
    private int number;

    public void writer() {
        number = 42;       // 操作 A
        ready = true;      // 操作 B
    }

    public void reader() {
        if (ready) {       // 操作 C（由于 ready 是 volatile，它 `happens-before` 这里的操作）
            assert number == 42;  // 因为 A `happens-before` B，B `happens-before` C，所以 A `happens-before` C
        }
    }
}

```

由于 ready 是一个 volatile 变量，写入 ready（操作 B）发生在读取 ready 之前，同样，写入 number（操作 A）发生在写入 ready 之前。根据传递性规则，写入 number 发生在读取 ready 之前。


