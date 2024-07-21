# 典型回答

如果不能使用synchronized和lock的话，想要实现单例可以通过饿汉模式、枚举、以及静态内部类的方式实现。

**饿汉**，其实都是通过定义静态的成员变量，以保证instance可以在类初始化的时候被实例化。

**静态内部类**，这种方式和饿汉方式只有细微差别，只是做法上稍微优雅一点。这种方式是Singleton类被装载了，instance不一定被初始化。因为SingletonHolder类没有被主动使用，只有显示通过调用getInstance方法时，才会显示装载SingletonHolder类，从而实例化instance。。。但是，原理和饿汉一样。

**枚举**，其实，如果把枚举类进行反编译，你会发现他也是使用了static final来修饰每一个枚举项。

其实，上面三种方式，都是依赖静态数据在类初始化的过程中被实例化这一机制的。但是，如果真要较真的话，ClassLoader的loadClass方法在加载类的时候使用了synchronized关键字。也正是因为这样， 除非被重写，这个方法默认在整个装载过程中都是同步的（线程安全的）。

那么，除了上面这三种，还有一种无锁的实现方式，那就是CAS。

# 扩展知识

## CAS实现线程安全的单例

```
public class Singleton {
    private static final AtomicReference<Singleton> INSTANCE = new AtomicReference<Singleton>(); 

    private Singleton() {}

    public static Singleton getInstance() {
        for (;;) {
            Singleton singleton = INSTANCE.get();
            if (null != singleton) {
                return singleton;
            }

            singleton = new Singleton();
            if (INSTANCE.compareAndSet(null, singleton)) {
                return singleton;
            }
        }
    }
}
```

用CAS的好处在于不需要使用传统的锁机制来保证线程安全。

但是我们的实现方式中，用了一个for循环一直在进行重试，所以，这种方式有一个比较大的缺点在于，如果忙等待一直执行不成功(一直在死循环中)，会对CPU造成较大的执行开销。

