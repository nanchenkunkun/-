# 典型回答

### 程序正常运行完

当Java应用程序中的所有非守护线程（即用户线程）都完成执行且没有其他活动线程时，JVM会正常退出。这是最常见的退出方式。

[✅什么是守护线程，和普通线程有什么区别？](https://www.yuque.com/hollis666/fo22bm/dlg6vw?view=doc_embed)

### System.exit() 被调用
当代码中的任何位置调用 System.exit(int status) 方法时，JVM会立即开始终止过程。这个方法可以接受一个状态码，通常用0表示正常退出，非0表示异常退出。

### Runtime.getRuntime().halt() 被调用
与 System.exit() 不同，Runtime.getRuntime().halt(int status) 方法用于强制终止当前运行的Java虚拟机，而不会执行任何关闭钩子或者终止已注册的未捕获异常处理器。

### 遇到无法恢复的错误
当JVM遇到一个无法恢复的系统错误，如操作系统信号或内部错误，它可能会立即退出。比如JVM自身的bug或者本地方法库存在一些问题等。

但是需要注意的是，我们常见的一些ERROR，如OOM，并不会导致JVM立即退出：

[✅Java发生了OOM一定会导致JVM 退出吗？](https://www.yuque.com/hollis666/fo22bm/fsnk2a6xdyhqfvf7?view=doc_embed)

但是，在在一些极端情况下，比如元空间（Metaspace）耗尽或JVM本身的资源不足，JVM可能会处于无法恢复的状态，从而导致整个JVM进程终止。

还有就是，有些JVM参数配置可能会在遇到OOM时导致JVM终止。例如，-XX:OnOutOfMemoryError="<cmd args>; <cmd args>" 参数允许用户指定在遇到OOM错误时要执行的命令，这些命令可以包括终止JVM的命令。

如：`XX:OnOutOfMemoryError="kill -9 %p"`

### 接收到终止信号
在类Unix系统中，JVM进程可能会因为接收到某些类型的操作系统信号（如SIGKILL或SIGTERM）而立即退出。某些信号会导致JVM进行优雅地关闭，如执行关闭钩子，而某些信号则会导致JVM立即终止。

> SIGTERM:kill -15 
> SIGKILL:kill -9 


[✅对JDK进程执行kill -9有什么影响？](https://www.yuque.com/hollis666/fo22bm/kmlq81?view=doc_embed)
### 

