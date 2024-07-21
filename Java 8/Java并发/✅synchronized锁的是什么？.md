# 典型回答

[✅synchronized是怎么实现的？](https://www.yuque.com/hollis666/fo22bm/gxq5p0?view=doc_embed)

上面的题目中介绍过synchronized的实现机制，但是很多人还是不明白synchronized到底锁的是什么。因为synchronized在使用的时候，可以锁定对象，也可以锁定当前类，所以很多人会产生误会：

```java
//同步方法，对象锁  
public synchronized void doSth(){
    System.out.println("Hello World");
}

//同步方法，类锁  
public synchronized static void doSth(){
    System.out.println("Hello World");
}


//同步代码块，类锁
public void doSth1(){
    synchronized (Demo.class){
        System.out.println("Hello World");
    }
}

//同步代码块，对象锁
public void doSth1(){
    synchronized (this){
        System.out.println("Hello World");
    }
}
```

但是，不管是上面哪种用法，** synchronized最终锁的都是对象！**Java中一切皆对象，类最终也是通过对象的方式呈现的。所以，**synchronized的不同用法，只不过锁的对象不同而已。**

并且，我们都知道synchronized加锁的过程需要再对象头上修改锁标记的，而这个对象头你就知道要依赖对象了。

### 同步方法
synchronized修饰的同步方法有两种，一种是普通方法，一种是静态方法。

以下，是一个普通的同步方法：
```java
public class ThreadTest {

    public static void main(String[] args) {

        for (int i = 0; i < 10; i++) {
            MyThread myThread = new MyThread();
            Thread thread = new Thread(myThread);
            thread.start();

        }
    }
}

class MyThread implements Runnable {

    @Override
    public void run() {
        print();
    }

    public synchronized void print() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(Thread.currentThread().getName() + " : " + System.currentTimeMillis());
    }
}
```

这个代码在执行时，输出结果：

```java
Thread-3 : 1692845687094
Thread-1 : 1692845687094
Thread-5 : 1692845687094
Thread-6 : 1692845687094
Thread-7 : 1692845687094
Thread-0 : 1692845687094
Thread-4 : 1692845687094
Thread-2 : 1692845687094
Thread-8 : 1692845687094
Thread-9 : 1692845687094
```

也就是说，多个线程之间是没有互相被锁阻塞而影响的，他们打印出来的时间戳都是一样的。

而，如果是同步静态方法就不一样了：
```java
public class ThreadTest {

    public static void main(String[] args) {

        for (int i = 0; i < 10; i++) {
            MyThread myThread = new MyThread();
            Thread thread = new Thread(myThread);
            thread.start();

        }
    }
}

class MyThread implements Runnable {

    @Override
    public void run() {
        print();
    }

    public static synchronized void print() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(Thread.currentThread().getName() + " : " + System.currentTimeMillis());
    }
}
```

以上代码在执行时，每一行内容是顺序并且按照一定的延迟打印出来的：

```java
Thread-0 : 1692845747897
Thread-9 : 1692845748904
Thread-8 : 1692845749905
Thread-7 : 1692845750909
Thread-6 : 1692845751913
Thread-5 : 1692845752915
Thread-4 : 1692845753920
Thread-3 : 1692845754923
Thread-2 : 1692845755925
Thread-1 : 1692845756931
```


可以看到，每个线程打印出来的时间戳都不一样，是有时间间隔的，这是因为他们互相之间有锁阻塞的影响。

**总结一下，synchronized的普通方法，其实锁的是具体调用这个方法的实例对象，而synchronized的静态方法，其实锁的是这个方法锁属于的类对象。**

> 一个类只有一个类对象，但是有很多个实例对象。


### 同步代码块

除了同步方法以外，synchronized还有同步代码块，同步代码块有两种用法，分别是：

```java
synchronized (this){
}

和

synchronized (ThreadTest.class){
}
```

以下是两个示例：

```java
public class ThreadTest {

    public static void main(String[] args) {

        for (int i = 0; i < 10; i++) {
            MyThread myThread = new MyThread();
            Thread thread = new Thread(myThread);
            thread.start();

        }
    }
}

class MyThread implements Runnable {

    @Override
    public void run() {
        synchronized (this){
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(Thread.currentThread().getName() + " : " + System.currentTimeMillis());
        }
    }
}
```

这个代码在执行时，输出结果：

```java
Thread-1 : 1692845948150
Thread-2 : 1692845948150
Thread-9 : 1692845948150
Thread-0 : 1692845948150
Thread-3 : 1692845948150
Thread-6 : 1692845948150
Thread-4 : 1692845948150
Thread-5 : 1692845948150
Thread-7 : 1692845948150
Thread-8 : 1692845948150
```

也就是说，多个线程之间是没有互相被锁阻塞而影响的，他们打印出来的时间戳都是一样的。

而下面这个例子：

```java
public class ThreadTest {

    public static void main(String[] args) {

        for (int i = 0; i < 10; i++) {
            MyThread myThread = new MyThread();
            Thread thread = new Thread(myThread);
            thread.start();

        }
    }
}

class MyThread implements Runnable {

    @Override
    public void run() {
        synchronized (MyThread.class){
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(Thread.currentThread().getName() + " : " + System.currentTimeMillis());
        }
    }
}
```

输出结果：

```java
Thread-0 : 1692845997813
Thread-9 : 1692845998820
Thread-8 : 1692845999825
Thread-7 : 1692846000831
Thread-6 : 1692846001836
Thread-5 : 1692846002841
Thread-4 : 1692846003848
Thread-3 : 1692846004852
Thread-2 : 1692846005856
Thread-1 : 1692846006859
```


可以看到，每个线程打印出来的时间戳都不一样，是有时间间隔的，这是因为他们互相之间有锁阻塞的影响。


**总结一下，synchronized(this)，其实锁的是this这个实例对象，而synchronized(Xxx.Class)，其实锁的是这个类对象。**

