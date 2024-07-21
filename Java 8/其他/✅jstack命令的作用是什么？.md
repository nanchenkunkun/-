# 典型回答

jstack用于生成java虚拟机当前时刻的线程快照。

> 线程快照是当前java虚拟机内每一条线程正在执行的方法堆栈的集合


生成线程快照的主要目的是定位线程出现长时间停顿的原因，如线程间死锁、死循环、请求外部资源导致的长时间等待等。 

线程出现停顿的时候通过jstack来查看各个线程的调用堆栈，就可以知道没有响应的线程到底在后台做什么事情，或者等待什么资源。 

# 扩展知识

## 使用

想要学习一个命令，先来看看帮助，使用jstack -help查看帮助：
```
hollis@hos:~$ jstack -help
Usage:
    jstack [-l] <pid>
        (to connect to running process)
    jstack -F [-m] [-l] <pid>
        (to connect to a hung process)
    jstack [-m] [-l] <executable> <core>
        (to connect to a core file)
    jstack [-m] [-l] [server_id@]<remote server IP or hostname>
        (to connect to a remote debug server)

Options:
    -F  to force a thread dump. Use when jstack <pid> does not respond (process is hung)
    -m  to print both java and native frames (mixed mode)
    -l  long listing. Prints additional information about locks
    -h or -help to print this help message
```

-F当’jstack [-l] pid’没有响应的时候强制打印栈信息 -l长列表. 打印关于锁的附加信息,例如属于java.util.concurrent的ownable synchronizers列表. -m打印java和native c/c++框架的所有栈信息. -h | -help打印帮助信息 pid 需要被打印配置信息的java进程id,可以用jps查询.

首先，我们分析这么一段程序的线程情况：
```
/**
 * @author hollis
 */
public class JStackDemo1 {
    public static void main(String[] args) {
        while (true) {
            //Do Nothing
        }
    }
}
```

先是用jps查看进程号：
```
hollis@hos:~$ jps
29788 JStackDemo1
29834 Jps
22385 org.eclipse.equinox.launcher_1.3.0.v20130327-1440.jar
```

然后使用jstack 查看堆栈信息：
```
hollis@hos:~$ jstack 29788
2015-04-17 23:47:31
...此处省略若干内容...
"main" prio=10 tid=0x00007f197800a000 nid=0x7462 runnable [0x00007f197f7e1000]
   java.lang.Thread.State: RUNNABLE
    at javaCommand.JStackDemo1.main(JStackDemo1.java:7)
```

我们可以从这段堆栈信息中看出什么来呢？我们可以看到，当前一共有一条用户级别线程,线程处于runnable状态，执行到JStackDemo1.java的第七行。 看下面代码：

```
/**
 * @author hollis
 */
public class JStackDemo1 {
    public static void main(String[] args) {
        Thread thread = new Thread(new Thread1());
        thread.start();
    }
}
class Thread1 implements Runnable{
    @Override
    public void run() {
        while(true){
            System.out.println(1);
        }
    }
}
```
线程堆栈信息如下：

```
"Reference Handler" daemon prio=10 tid=0x00007fbbcc06e000 nid=0x286c in Object.wait() [0x00007fbbc8dfc000]
   java.lang.Thread.State: WAITING (on object monitor)
    at java.lang.Object.wait(Native Method)
    - waiting on <0x0000000783e066e0> (a java.lang.ref.Reference$Lock)
    at java.lang.Object.wait(Object.java:503)
    at java.lang.ref.Reference$ReferenceHandler.run(Reference.java:133)
    - locked <0x0000000783e066e0> (a java.lang.ref.Reference$Lock)
```
我们能看到：

> 线程的状态： WAITING 线程的调用栈 线程的当前锁住的资源： <0x0000000783e066e0> 线程当前等待的资源：<0x0000000783e066e0>


为什么同时锁住的等待同一个资源：

> 线程的执行中，先获得了这个对象的 Monitor（对应于 locked <0x0000000783e066e0>）。当执行到 obj.wait(), 线程即放弃了 Monitor的所有权，进入 “wait set”队列（对应于 waiting on <0x0000000783e066e0> ）。



