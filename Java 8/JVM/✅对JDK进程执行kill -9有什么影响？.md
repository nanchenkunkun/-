# 典型回答
kill -9 命令会立刻关闭Jvm进程。但是kill -9的语意是强制关闭，会导致在Jvm中执行的服务立刻关闭，来不及收尾。如导致RPC服务没有从注册中心取消注册导致服务不可用，如导致事务执行一半直接终止等等
## kill 命令
我们都知道，想要在Linux中终止一个进程有两种方式，如果是前台进程可以使用Ctrl+C键进行终止；如果是后台进程，那么需要使用kill命令来终止。（其实Ctrl+C也是kill命令）<br />kill命令的格式是：
> kill[参数] [进程号] ，如：
> kill 21121 
> kill -9 21121

其中[参数]是可选的，进程号可以通过jps/ps/pidof/pstree/top等工具获取。<br />kill的命令参数有以下几种：
> -l 信号，如果不加信号的编号参数，则使用“-l”参数会列出全部的信号名称
> -a 当处理当前进程时，不限制命令名和进程号的对应关系
> -p 指定kill 命令只打印相关进程的进程号，而不发送任何信号
> -s 指定发送信号
> -u 指定用户

通常情况下，我们使用的-l(信号)的时候比较多，如我们前文提到的`kill -9`中的9就是信号。<br />信号如果没有指定的话，默认会发出终止信号(15)。常用的信号如下：
> HUP 1 终端断线
> INT 2 中断（同 Ctrl + C）
> QUIT 3 退出（同 Ctrl + \）
> TERM 15 终止
> KILL 9 强制终止
> CONT 18 继续（与STOP相反， fg/bg命令）
> STOP 19 暂停（同 Ctrl + Z）

比较常用的就是强制终止信号：9和终止信号：15。<br />另外，中断信号：2其实就是我们前文提到的Ctrl + C结束前台进程。<br />那么，`kill -9`和`kill -15`到底有什么区别呢？该如何选择呢？
## kill -9 和 kill -15的区别
kill命令默认的信号就是15，首先来说一下这个默认的kill -15信号。<br />当使用kill -15时，系统会发送一个SIGTERM的信号给对应的程序。当程序接收到该信号后，具体要如何处理是自己可以决定的。这时候，应用程序可以选择：

- 立即停止程序
- 释放响应资源后停止程序
- 忽略该信号，继续执行程序

因为kill -15信号只是通知对应的进程要进行”安全、干净的退出”，程序接到信号之后，退出前一般会进行一些”准备工作”，如资源释放、临时文件清理等等，如果准备工作做完了，再进行程序的终止。<br />但是，如果在”准备工作”进行过程中，遇到阻塞或者其他问题导致无法成功，那么应用程序可以选择忽略该终止信号。<br />这也就是为什么我们有的时候使用kill命令是没办法”杀死”应用的原因，因为默认的kill信号是`SIGTERM（15）`，而`SIGTERM（15）`的信号是可以被阻塞和忽略的。<br />和`kill -15`相比，`kill -9`就相对强硬一点，系统会发出SIGKILL信号，他要求接收到该信号的程序应该立即结束运行，不能被阻塞或者忽略。<br />所以，相比于`kill -15`命令，`kill -9`在执行时，应用程序是没有时间进行”准备工作”的，所以**这通常会带来一些副作用，数据丢失或者终端无法恢复到正常状态等**。
# 知识扩展
## Java是如何处理SIGTERM(15)的
我们都知道，在Linux中，Java应用是作为一个独立进程运行的，Java程序的终止运行是基于JVM的关闭实现的，JVM关闭方式分为3种：

1. 正常关闭：当最后一个非守护线程结束或者调用了System.exit或者通过其他特定平台的方法关闭（接收到SIGINT（2）、SIGTERM（15）信号等）
2. 强制关闭：通过调用Runtime.halt方法或者是在操作系统中强制kill（接收到SIGKILL（9）信号)
3. 异常关闭：运行中遇到RuntimeException异常等。

JVM进程在接收到kill -15信号通知的时候，是可以做一些清理动作的，比如删除临时文件等。<br />当然，开发者也是可以自定义做一些额外的事情的，比如让tomcat容器停止，让dubbo服务下线等。<br />而这种自定义JVM清理动作的方式，是通过JDK中提供的shutdown hook实现的。JDK提供了`Java.Runtime.addShutdownHook(Thread hook)`方法，可以注册一个JVM关闭的钩子。<br />例子如下：
```java
public class ShutdownHookTest {

    public static void main(String[] args) {
        boolean flag = true;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("hook execute...");
        }));

        while (flag) {
            // app is runing
        }

        System.out.println("main thread execute end...");
    }
}
```
执行命令：
```shell
➜ jps
6520 ShutdownHookTest
6521 Jps
➜ kill 6520
```
控制台输出内容：<br />`hook execute... Process finished with exit code 143 (interrupted by signal 15: SIGTERM)`<br />可以看到，当我们使用kill（默认kill -15）关闭进程的时候，程序会先执行我注册的shutdownHook，然后再退出，并且会给出一个提示：interrupted by signal 15: SIGTERM<br />如果我们执行命令kill -9：
```shell
➜ kill -9 6520
```
控制台输出内容：<br />`Process finished with exit code 137 (interrupted by signal 9: SIGKILL)`<br />可以看到，当我们使用kill -9 强制关闭进程的时候，程序并没有执行shutdownHook，而是直接退出了，并且会给出一个提示：interrupted by signal 9: SIGKILL
## 正常的重启机器流程是什么样的

1. 先将堆栈的文件dump下来
2. 重启机器，如果重启失败，则采用kill -15命令
3. 事后分析dump文件

> 重启的过程是，先stop、再start。很多时候，start失败的概率要比stop失败的概率低多了。
> 当stop失败的时候，就有进程在，可以对他用kill -15

