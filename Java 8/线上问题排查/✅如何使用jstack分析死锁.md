[✅什么是死锁，如何解决？](https://www.yuque.com/hollis666/fo22bm/mtdxsd?view=doc_embed)

先来写一段死锁的程序：

```
package javaCommand;
/**
 * @author hollis
 */
public class JStackDemo {
    public static void main(String[] args) {
        Thread t1 = new Thread(new DeadLockclass(true));//建立一个线程
        Thread t2 = new Thread(new DeadLockclass(false));//建立另一个线程
        t1.start();//启动一个线程
        t2.start();//启动另一个线程
    }
}
class DeadLockclass implements Runnable {
    public boolean falg;// 控制线程
    DeadLockclass(boolean falg) {
        this.falg = falg;
    }
    public void run() {
        /**
         * 如果falg的值为true则调用t1线程
         */
        if (falg) {
            while (true) {
                synchronized (Suo.o1) {
                    System.out.println("o1 " + Thread.currentThread().getName());
                    synchronized (Suo.o2) {
                        System.out.println("o2 " + Thread.currentThread().getName());
                    }
                }
            }
        }
        /**
         * 如果falg的值为false则调用t2线程
         */
        else {
            while (true) {
                synchronized (Suo.o2) {
                    System.out.println("o2 " + Thread.currentThread().getName());
                    synchronized (Suo.o1) {
                        System.out.println("o1 " + Thread.currentThread().getName());
                    }
                }
            }
        }
    }
}

class Suo {
    static Object o1 = new Object();
    static Object o2 = new Object();
}
```

当我启动该程序时，我们看一下控制台：

![QQ20160118-0 (1).png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1694948245009-c9d0bf3a-db0a-4c78-8ff0-afd4e114c2fe.png#averageHue=%23393b3f&clientId=uf115ccac-63fe-4&from=ui&id=u380a0445&originHeight=98&originWidth=375&originalType=binary&ratio=1&rotation=0&showTitle=false&size=14668&status=done&style=none&taskId=u62ee147f-fa3d-4285-9c53-83b19c4e0c4&title=)

我们发现，程序只输出了两行内容，然后程序就不再打印其它的东西了，但是程序并没有停止。

这样就产生了死锁。 当线程1使用synchronized锁住了o1的同时，线程2也是用synchronized锁住了o2。当两个线程都执行完第一个打印任务的时候，线程1想锁住o2，线程2想锁住o1。但是，线程1当前锁着o1，线程2锁着o2。所以两个想成都无法继续执行下去，就造成了死锁。

然后，我们使用jstack来看一下线程堆栈信息：

```
Found one Java-level deadlock:
=============================
"Thread-1":
  waiting to lock monitor 0x00007f0134003ae8 (object 0x00000007d6aa2c98, a java.lang.Object),
  which is held by "Thread-0"
"Thread-0":
  waiting to lock monitor 0x00007f0134006168 (object 0x00000007d6aa2ca8, a java.lang.Object),
  which is held by "Thread-1"

Java stack information for the threads listed above:
===================================================
"Thread-1":
    at javaCommand.DeadLockclass.run(JStackDemo.java:40)
    - waiting to lock <0x00000007d6aa2c98> (a java.lang.Object)
    - locked <0x00000007d6aa2ca8> (a java.lang.Object)
    at java.lang.Thread.run(Thread.java:745)
"Thread-0":
    at javaCommand.DeadLockclass.run(JStackDemo.java:27)
    - waiting to lock <0x00000007d6aa2ca8> (a java.lang.Object)
    - locked <0x00000007d6aa2c98> (a java.lang.Object)
    at java.lang.Thread.run(Thread.java:745)

Found 1 deadlock.
```

哈哈，堆栈写的很明显，它告诉我们 Found one Java-level deadlock，然后指出造成死锁的两个线程的内容。然后，又通过 Java stack information for the threads listed above来显示更详细的死锁的信息。 他说

> Thread-1在想要执行第40行的时候，当前锁住了资源<0x00000007d6aa2ca8>,但是他在等待资源<0x00000007d6aa2c98> Thread-0在想要执行第27行的时候，当前锁住了资源<0x00000007d6aa2c98>,但是他在等待资源<0x00000007d6aa2ca8> 由于这两个线程都持有资源，并且都需要对方的资源，所以造成了死锁。 原因我们找到了，就可以具体问题具体分析，解决这个死锁了。

