# 典型回答

Spring的Bean是否线程安全，这个要取决于他的作用域。Spring的Bean有多种作用域，其中用的比较多的就是Singleton和Prototype。

默认情况下，Spring Bean 是单例的（Singleton）。这意味着在整个 Spring 容器中只存在一个 Bean 实例。如果将 Bean 的作用域设置为原型的（Prototype） ，那么每次从容器中获取 Bean 时都会创建一个新的实例。

对于Prototype这种作用域的Bean，他的Bean 实例不会被多个线程共享，所以不存在线程安全的问题。

但是对于Singleton的Bean，就可能存在线程安全问题了，但是也不绝对，要看这个Bean中是否有共享变量。

[✅能不能谈谈你对线程安全的理解？](https://www.yuque.com/hollis666/fo22bm/bnddbd?view=doc_embed&inner=Dg2zO)

如以下Bean：

```
@Service
public class CounterService {
    private int count = 0;

    public int increment() {
        return ++count;
    }
}
```

默认情况下，Spring Bean 是单例的，count字段是一个共享变量，那么如果多个线程同时调用 increment 方法，可能导致计数器的值不正确。那么这段代码就不是线程安全的。

我们通常把上面这种Bean叫做有状态的Bean，有状态的Bean就是非线程安全的，我们需要自己来考虑他的线程安全性问题。

那如果一个Singleton的Bean中是无状态的，即没有成员变量，或者成员变量只读不写，那么他就是个线程安全的。

```
@Service
public class CounterService {
    
    public int increment(int a) {
        return ++a;
    }
}

```

所以，总结一下就是：

**Prototype的Bean是线程安全的，无状态的Singleton的Bean是线程安全的。有状态的Singleton的Bean是非线程安全的。**

# 扩展知识

## 有状态的Bean如何解决线程安全问题

想要让一个有状态的Bean变得线程安全，有以下几个做法：

1、修改作用域为Prototype，这样的Bean就可以避免线程安全问题。

```
@Scope("prototype")
@Service
public class CounterService {

  	private int count = 0;
    // ...
}
```

但是需要注意，Prototype的bean，每次从容器中请求一个 Prototype Bean 时，都会创建一个新的实例。这可能导致性能开销，特别是在需要频繁创建对象的情况下。 而且，每个 Prototype Bean 的实例都需要占用一定的内存，可能会导致内存资源的消耗较大。

2、加锁

想要实现线程安全，有一个有效的办法就是加锁，在并发修改共享变量的地方加锁：

```
@Service
public class CounterService {
    private int count = 0;

    public synchronized int increment() {
        return ++count;
    }
}
```

但是加锁的话会影响并发，降低系统的吞吐量，所以使用的时候需要谨慎，不建议用这个方案。

3、使用并发工具类

可以使用并发包中提供的工具类，如原子类，线程安全的集合等。

```
import java.util.concurrent.atomic.AtomicInteger;

public class CounterService {
    private AtomicInteger count = new AtomicInteger(0);

    public int increment() {
        return count.incrementAndGet();
    }
}

```

建议使用这种，既能保证线程安全，又有比较好的性能。
