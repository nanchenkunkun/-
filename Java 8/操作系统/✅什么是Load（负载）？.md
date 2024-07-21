# 典型回答
负载(load)是Linux机器的一个重要指标，直观了反应了机器当前的状态。

在Linux系统中，系统负载是对当前CPU工作量的度量，被定义为特定时间间隔内运行队列中的平均线程数。load average 表示机器一段时间内的平均load。这个值越低越好。负载过高会导致机器无法处理其他请求及操作，甚至导致死机。

Linux的负载高，主要是由于CPU使用、内存使用、IO消耗三部分构成。任意一项使用过多，都将导致服务器负载的急剧攀升。

# 扩展知识

## 查看机器负载

在Linux机器上，有多个命令都可以查看机器的负载信息。其中包括`uptime`、`top`、`w`等。

### `uptime`命令

`uptime`命令能够打印系统总共运行了多长时间和系统的平均负载。uptime命令可以显示的信息显示依次为：现在时间、系统已经运行了多长时间、目前有多少登陆用户、系统在过去的1分钟、5分钟和15分钟内的平均负载。

```
➜  ~ uptime
13:29  up 23:41, 3 users, load averages: 1.74 1.87 1.97
```

这行信息的后半部分，显示"load average"，它的意思是"系统的平均负荷"，里面有三个数字，我们可以从中判断系统负荷是大还是小。

`1.74 1.87 1.97` 这三个数字的意思分别是1分钟、5分钟、15分钟内系统的平均负荷。我们一般表示为load1、load5、load15。

### `w`命令

w命令的主要功能其实是显示目前登入系统的用户信息。但是与who不同的是，w命令功能更加强大，w命令还可以显示：当前时间，系统启动到现在的时间，登录用户的数目，系统在最**近1分钟、5分钟和15分钟的平均负载**。然后是每个用户的各项数据，项目显示顺序如下：登录帐号、终端名称、远 程主机名、登录时间、空闲时间、JCPU、PCPU、当前正在运行进程的命令行。

```
➜  ~ w
14:08  up 23:41, 3 users, load averages: 1.74 1.87 1.97
USER     TTY      FROM              LOGIN@  IDLE WHAT
hollis   console  -                六14   23:40 -
hollis   s000     -                六14   20:24 -zsh
hollis   s001     -                六15       - w
```

从上面的`w`命令的结果可以看到，当前系统时间是14:08，系统启动到现在经历了23小时41分钟，共有3个用户登录。系统在近1分钟、5分钟和15分钟的平均负载分别是`1.74 1.87 1.97`。这和`uptime`得到的结果相同。 下面还打印了一些登录的用户的各项数据，不详细介绍了。

### `top`命令

top命令是Linux下常用的性能分析工具，能够实时显示系统中各个进程的资源占用状况，类似于Windows的任务管理器。

```
➜  ~ top
Processes: 244 total, 3 running, 9 stuck, 232 sleeping, 1484 threads                                                                                                                               14:16:01
Load Avg: 1.74, 1.87, 1.97  CPU usage: 8.0% user, 6.79% sys, 85.19% idle   SharedLibs: 116M resident, 16M data, 14M linkedit. MemRegions: 66523 total, 2152M resident, 50M private, 930M shared.
PhysMem: 7819M used (1692M wired), 370M unused. VM: 682G vsize, 533M framework vsize, 6402060(0) swapins, 7234356(0) swapouts. Networks: packets: 383006/251M in, 334448/60M out.
Disks: 1057821/38G read, 350852/40G written.

PID    COMMAND      %CPU TIME     #TH   #WQ  #PORT MEM    PURG   CMPRS  PGRP  PPID  STATE    BOOSTS          %CPU_ME %CPU_OTHRS UID  FAULTS    COW    MSGSENT   MSGRECV   SYSBSD    SYSMACH   CSW
30845  top          3.0  00:00.49 1/1   0    21    3632K  0B     0B     30845 1394  running  *0[1]           0.00000 0.00000    0    3283+     112    203556+   101770+   8212+     119901+   823+
30842  Google Chrom 0.0  00:47.39 17    0    155   130M   0B     0B     1146  1146  sleeping *0[1]           0.00000 0.00000    501  173746    2697   117678    37821     364228    444830    310043
```

上面的输出结果中，Load Avg: 1.74, 1.87, 1.97显示的就是负载信息。

## 机器正常负载范围

对于机器的Load到底多少算正常的问题，一直都是很有争议的，不同人有着不同的理解。对于单个CPU，有人认为如果Load超过0.7就算是超出正常范围了。也有人认为只要不超过1都没问题。也有人认为，单个CPU的负载在2以下都可以接受。

为什么会有这么多不同的理解呢，是因为不同的机器除了CPU影响之外还有其他因素的影响，运行的程序、机器内存、甚至是机房温度等都有可能有区别。

比如，有些机器用于定时执行大量的跑批任务，这个时间段内，Load可能会飙的比较高。而其他时间可能会比较低。那么这段飙高时间我们要不要去排查问题呢？

我的建议是，最好根据自己机器的实际情况，建立一个指标的基线（如近一个月的平均值），只要日常的load在基线上下范围内不太大都可以接收，如果差距太多可能就要人为介入检查了。

但是，总要有个建议的阈值吧，关于这个值。阮一峰在自己的博客中有过以下建议：

> 当系统负荷持续大于0.7，你必须开始调查了，问题出在哪里，防止情况恶化。
>  
> 当系统负荷持续大于1.0，你必须动手寻找解决办法，把这个值降下来。
>  
> 当系统负荷达到5.0，就表明你的系统有很严重的问题，长时间没有响应，或者接近死机了。你不应该让系统达到这个值。


以上指标都是基于单CPU的，但是现在很多电脑都是多核的。所以，对一般的系统来说，是根据cpu数量去判断系统是否已经过载（Over Load）的。如果我们认为0.7算是单核机器负载的安全线的话，那么四核机器的负载最好保持在3(4*0.7 = 2.8)以下。

还有一点需要提一下，在Load Avg的指标中，有三个值，1分钟系统负荷、5分钟系统负荷，15分钟系统负荷。我们在排查问题的时候也是可以参考这三个值的。

一般情况下，1分钟系统负荷表示最近的暂时现象。15分钟系统负荷表示是持续现象，并非暂时问题。如果load15较高，而load1较低，可以认为情况有所好转。反之，情况可能在恶化。

## 如何降低负载

导致负载高的原因可能很复杂，有可能是硬件问题也可能是软件问题。

如果是硬件问题，那么说明机器性能确实就不行了，那么解决起来很简单，直接换机器就可以了。

前面我们提过，CPU使用、内存使用、IO消耗都可能导致负载高。如果是软件问题，有可能由于Java中的某些线程被长时间占用、大量内存持续占用等导致。建议从以下几个方面排查代码问题：

1、是否有内存泄露导致频繁GC <br />2、是否有死锁发生 <br />3、是否有大字段的读写 <br />4、会不会是数据库操作导致的，排查SQL语句问题。

这里还有个建议，如果发现线上机器Load飙高，可以考虑先把堆栈内存dump下来后，进行重启，暂时解决问题，然后再考虑回滚和排查问题。

## Java Web应用Load飙高排查思路

1、使用uptime查看当前load，发现load飙高。

```
➜  ~ uptime
13:29  up 23:41, 3 users, load averages: 10 10 10
```

2、使用top命令，查看占用CPU较高的进程ID。

```
➜  ~ top

PID USER      PR  NI  VIRT  RES  SHR S %CPU %MEM    TIME+  COMMAND
1893 admin     20   0 7127m 2.6g  38m S 181.7 32.6  10:20.26 java
```

发现PID为1893的进程占用CPU 181%。而且是一个Java进程，基本断定是软件问题。

3、使用 `top`命令，查看具体是哪个线程占用率较高

```
➜  ~ top -Hp 1893
PID USER      PR  NI  VIRT  RES  SHR S %CPU %MEM    TIME+  COMMAND
4519 admin     20   0 7127m 2.6g  38m R 18.6 32.6   0:40.11 java
```

4、使用`printf`命令查看这个线程的16进制

```
➜  ~ printf %x 4519
11a7
```

5、使用`jstack`命令查看当前线程正在执行的方法。

```
➜  ~ jstack 1893 |grep -A 200 11a7
"thread-5" #500 daemon prio=10 os_prio=0 tid=0x00007f632314a800 nid=0x11a2 runnable [0x000000005442a000]
java.lang.Thread.State: RUNNABLE
at sun.misc.URLClassPath$Loader.findResource(URLClassPath.java:684)
at sun.misc.URLClassPath.findResource(URLClassPath.java:188)
at java.net.URLClassLoader$2.run(URLClassLoader.java:569)
at java.net.URLClassLoader$2.run(URLClassLoader.java:567)
at java.security.AccessController.doPrivileged(Native Method)
at java.net.URLClassLoader.findResource(URLClassLoader.java:566)
at org.hibernate.validator.internal.xml.ValidationXmlParser.getInputStreamForPath(ValidationXmlParser.java:248)
at com.hollis.test.util.BeanValidator.validate(BeanValidator.java:30)
```

从上面的线程的栈日志中，可以发现，当前占用CPU较高的线程正在执行我代码的com.hollis.test.util.BeanValidator.validate(BeanValidator.java:30)类。那么就可以去排查这个类是否用法有问题了。

6、还可以使用jstat来查看GC情况，看看是否有频繁FGC，然后再使用jmap来dump内存，查看是否存在内存泄露。

## 真实排查过程

[✅Load飙高问题排查过程](https://www.yuque.com/hollis666/fo22bm/uq7bul?view=doc_embed)
