# 典型回答
synchronized其实是一种加锁机制，那么既然是锁，天然就具备以下几个缺点：

1、有性能损耗：虽然在JDK 1.6中对synchronized做了很多优化，如适应性自旋、锁消除、锁粗化、轻量级锁和偏向锁等。但是他毕竟还是一种锁。所以，无论是使用同步方法还是同步代码块，在同步操作之前还是要进行加锁，同步操作之后需要进行解锁，这个加锁、解锁的过程是要有性能损耗的。

2、产生阻塞：无论是同步方法还是同步代码块，无论是ACC_SYNCHRONIZED还是monitorenter、monitorexit都是基于Monitor实现的。基于Monitor对象，当多个线程同时访问一段同步代码时，首先会进入Entry Set，当有一个线程获取到对象的锁之后，才能进行The Owner区域，其他线程还会继续在Entry Set等待。并且当某个线程调用了wait方法后，会释放锁并进入Wait Set等待。所以，synchronize实现的锁本质上是一种阻塞锁。

除了前面我们提到的volatile比synchronized性能好以外，volatile其实还有一个很好的附加功能，那就是禁止指令重排。因为volatile借助了内存屏障来帮助其解决可见性和有序性问题，而内存屏障的使用还为其带来了一个禁止指令重排的附加功能，所以在有些场景中是可以避免发生指令重排的问题的。<br />[<br />](https://www.hollischuang.com/archives/3928)
# 扩展知识

## 锁的性能损耗

虽然在JDK 1.6中对synchronized做了很多优化，如如适应性自旋、锁消除、锁粗化、轻量级锁和偏向锁等，但是他毕竟还是一种锁。

以上这几种优化，都是尽量想办法避免对Monitor进行加锁，但是，并不是所有情况都可以优化的，况且就算是经过优化，优化的过程也是有一定的耗时的。

所以，无论是使用同步方法还是同步代码块，在同步操作之前还是要进行加锁，同步操作之后需要进行解锁，这个加锁、解锁的过程是要有性能损耗的。

关于二者的性能对比，由于虚拟机对锁实行的许多消除和优化，使得我们很难量化这两者之间的性能差距，但是我们可以确定的一个基本原则是：volatile变量的读操作的性能和普通变量几乎无差别，但是写操作由于需要插入内存屏障所以会慢一些，即便如此，volatile在大多数场景下也比锁的开销要低。

## 锁产生阻塞

无论是同步方法还是同步代码块，无论是ACC_SYNCHRONIZED还是monitorenter、monitorexit都是基于Monitor实现的。

基于Monitor对象，当多个线程同时访问一段同步代码时，首先会进入Entry Set，当有一个线程获取到对象的锁之后，才能进行The Owner区域，其他线程还会继续在Entry Set等待。并且当某个线程调用了wait方法后，会释放锁并进入Wait Set等待。

所以，synchronize实现的锁本质上是一种阻塞锁，也就是说多个线程要排队访问同一个共享对象。

而volatile是Java虚拟机提供的一种轻量级同步机制，他是基于内存屏障实现的。说到底，他并不是锁，所以他不会有synchronized带来的阻塞和性能损耗的问题。

## volatile的附加功能

除了前面我们提到的volatile比synchronized性能好以外，volatile其实还有一个很好的附加功能，那就是禁止指令重排。

我们先来举一个例子，看一下如果只使用synchronized而不使用volatile会发生什么问题，就拿我们比较熟悉的单例模式来看。

我们通过双重校验锁的方式实现一个单例，这里不使用volatile关键字：

```
 1   public class Singleton {  
 2      private static Singleton singleton;  
 3       private Singleton (){}  
 4       public static Singleton getSingleton() {  
 5       if (singleton == null) {  
 6           synchronized (Singleton.class) {  
 7               if (singleton == null) {  
 8                   singleton = new Singleton();  
 9               }  
 10           }  
 11       }  
 12       return singleton;  
 13       }  
 14   }
```

以上代码，我们通过使用synchronized对Singleton.class进行加锁，可以保证同一时间只有一个线程可以执行到同步代码块中的内容，也就是说singleton = new Singleton()这个操作只会执行一次，这就是实现了一个单例。

但是，当我们在代码中使用上述单例对象的时候有可能发生空指针异常。这是一个比较诡异的情况。

我们假设Thread1 和 Thread2两个线程同时请求Singleton.getSingleton方法的时候：

![image.png](https://cdn.nlark.com/yuque/0/2022/png/5378072/1665897156584-147a00e1-2b52-476e-ba46-e90647459545.png#averageHue=%23fcfbfa&clientId=u46c3b3f7-0e58-4&from=paste&id=u311935cb&originHeight=325&originWidth=877&originalType=url&ratio=1&rotation=0&showTitle=false&size=168070&status=done&style=none&taskId=u84d84808-c58c-4e82-aba9-03e4dc69486&title=)

Step1 ,Thread1执行到第8行，开始进行对象的初始化。 Step2 ,Thread2执行到第5行，判断singleton == null。 Step3 ,Thread2经过判断发现singleton ！= null，所以执行第12行，返回singleton。 Step4 ,Thread2拿到singleton对象之后，开始执行后续的操作，比如调用singleton.call()。

以上过程，看上去并没有什么问题，但是，其实，在Step4，Thread2在调用singleton.call()的时候，是有可能抛出空指针异常的。

之所有会有NPE抛出，是因为在Step3，Thread2拿到的singleton对象并不是一个完整的对象。

我们这里来分析一下，singleton = new Singleton();这行代码到底做了什么事情，大致过程如下：

- 1、虚拟机遇到new指令，到常量池定位到这个类的符号引用。
- 2、检查符号引用代表的类是否被加载、解析、初始化过。 
- 3、虚拟机为对象分配内存。 
- 4、虚拟机将分配到的内存空间都初始化为零值。 
- 5、虚拟机对对象进行必要的设置。
- 6、执行方法，成员变量进行初始化。 
- 7、将对象的引用指向这个内存区域。

我们把这个过程简化一下，简化成3个步骤：

- a、JVM为对象分配一块内存M 
- b、在内存M上为对象进行初始化
- c、将内存M的地址赋值给singleton变量

![image.png](https://cdn.nlark.com/yuque/0/2022/png/5378072/1665897242638-f3e87f94-2c09-42c6-a359-1b12eb9cf459.png#averageHue=%23fbfafa&clientId=u46c3b3f7-0e58-4&from=paste&id=u462dabd1&originHeight=347&originWidth=869&originalType=url&ratio=1&rotation=0&showTitle=false&size=197188&status=done&style=none&taskId=u4cdad04e-b96a-406e-b1bb-c2c2bba1ebd&title=)<br />因为将内存的地址赋值给singleton变量是最后一步，所以Thread1在这一步骤执行之前，Thread2在对singleton==null进行判断一直都是true的，那么他会一直阻塞，直到Thread1将这一步骤执行完。

但是，以上过程并不是一个原子操作，并且编译器可能会进行重排序，如果以上步骤被重排成：

- a、JVM为对象分配一块内存M
- c、将内存的地址复制给singleton变量 
- b、在内存M上为对象进行初始化

![image.png](https://cdn.nlark.com/yuque/0/2022/png/5378072/1665897252028-5d852e16-1954-42fd-8fbf-13bd8d39d41e.png#averageHue=%23fbfafa&clientId=u46c3b3f7-0e58-4&from=paste&id=ub0cd8fb4&originHeight=364&originWidth=863&originalType=url&ratio=1&rotation=0&showTitle=false&size=204356&status=done&style=none&taskId=u6a3a6ae5-7f82-41db-8519-3806637ea08&title=)

这样的话，Thread1会先执行内存分配，在执行变量赋值，最后执行对象的初始化，那么，也就是说，在Thread1还没有为对象进行初始化的时候，Thread2进来判断singleton==null就可能提前得到一个false，则会返回一个不完整的sigleton对象，因为他还未完成初始化操作。

这种情况一旦发生，我们拿到了一个不完整的singleton对象，当尝试使用这个对象的时候就极有可能发生NPE异常。

那么，怎么解决这个问题呢？因为指令重排导致了这个问题，那就避免指令重排就行了。

所以，volatile就派上用场了，因为volatile可以避免指令重排。只要将代码改成以下代码，就可以解决这个问题：

```
 1   public class Singleton {  
 2      private volatile static Singleton singleton;  
 3       private Singleton (){}  
 4       public static Singleton getSingleton() {  
 5       if (singleton == null) {  
 6           synchronized (Singleton.class) {  
 7               if (singleton == null) {  
 8                   singleton = new Singleton();  
 9               }  
 10           }  
 11       }  
 12       return singleton;  
 13       }  
 14   }
```

对singleton使用volatile约束，保证他的初始化过程不会被指令重排。
