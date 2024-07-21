# 典型回答

Dubbo是通过JDK的shutdown hook来完成优雅停机（下线）的。

对于服务提供方来说，应用下线时，先标记为不接受新请求，请求过来时直接报错，让客户端重试。然后，检测线程池中的线程是否正在运行，如果有，那就等线程执行完。

对于服务消费方来说，应用下线时，不再发起新的调用请求，所有的新调用在客户端侧直接报错。然后，检测还有没有远程请求没有得到返回的，如果有，则等待返回结果。

# 扩展知识
## 
## 优雅上下线

关于"优雅上下线"这个词，我没找到官方的解释，我尝试解释一下这是什么。

首先，上线、下线大家一定都很清楚，比如我们一次应用发布过程中，就需要先将应用服务停掉，然后再把服务启动起来。这个过程就包含了一次下线和一次上线。

那么，"优雅"怎么理解呢？

先说什么情况我们认为不优雅：

1、服务停止时，没有关闭对应的监控，导致应用停止后发生大量报警。

2、应用停止时，没有通知外部调用方，很多请求还会过来，导致很多调用失败。

3、应用停止时，有线程正在执行中，执行了一半，JVM进程就被干掉了。

4、应用启动时，服务还没准备好，就开始对外提供服务，导致很多失败调用。

5、应用启动时，没有检查应用的健康状态，就开始对外提供服务，导致很多失败调用。

以上，都是我们认为的不优雅的情况，那么，反过来，优雅上下线就是一种避免上述情况发生的手段。

一个应用的优雅上下线涉及到的内容其实有很多，从底层的操作系统、容器层面，到编程语言、框架层面，再到应用架构层面，涉及到的知识很广泛。

其实，优雅上下线中，最重要的还是优雅下线。因为如果下线过程不优雅的话，就会发生很多调用失败了、服务找不到等问题。所以很多时候，大家也会提优雅停机这样的概念。

本文后面介绍的优雅上下线也重点关注优雅停机的过程。

## 操作系统&容器的优雅上下线

我们知道，`kill -9`之所以不建议使用，是因为`kill -9`特别强硬，系统会发出SIGKILL信号，他要求接收到该信号的程序应该立即结束运行，不能被阻塞或者忽略。

这个过程显然是不优雅的，因为应用立刻停止的话，就没办法做收尾动作。而更优雅的方式是`kill -15`。

当使用`kill -15`时，系统会发送一个SIGTERM的信号给对应的程序。当程序接收到该信号后，具体要如何处理是自己可以决定的。

`kill -15`会通知到应用程序，这就是操作系统对于优雅上下线的最基本的支持。

以前，在操作系统之上就是应用程序了，但是，自从容器化技术推出之后，在操作系统和应用程序之间，多了一个容器层，而Docker、k8s等容器其实也是支持优雅上下线的。

如Docker中同样提供了两个命令， `docker stop` 和 `docker kill`

`docker stop`就像`kill -15`一样，他会向容器内的进程发送SIGTERM信号，在10S之后（可通过参数指定）再发送SIGKILL信号。

而`docker kill`就像`kill -9`，直接发送SIGKILL信号。

## JVM的优雅上下线

在操作系统、容器等对优雅上下线有了基本的支持之后，在接收到`docker stop`、`kill -15`等命令后，会通知应用进程进行进程关闭。

而Java应用在运行时就是一个独立运行的进程，这个进程是如何关闭的呢？

Java程序的终止运行是基于JVM的关闭实现的，JVM关闭方式分为正常关闭、强制关闭和异常关闭3种。

这其中，正常关闭就是支持优雅上下线的。正常关闭过程中，JVM可以做一些清理动作，比如删除临时文件。

当然，开发者也是可以自定义做一些额外的事情的，比如通知应用框架优雅上下线操作。

而这种机制**是通过JDK中提供的shutdown hook实现的。JDK提供了Java.Runtime.addShutdownHook(Thread hook)方法，可以注册一个JVM关闭的钩子。**

例子如下：

```
package com.hollis;

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

```
➜ jps
6520 ShutdownHookTest
6521 Jps
➜ kill 6520
```

控制台输出内容：

```
hook execute...
Process finished with exit code 143 (interrupted by signal 15: SIGTERM)
```

可以看到，当我们使用kill（默认kill -15）关闭进程的时候，程序会先执行我注册的shutdownHook，然后再退出，并且会给出一个提示：`interrupted by signal 15: SIGTERM`

## Spring的优雅上下线

有了JVM提供的shutdown hook之后，很多框架都可以通过这个机制来做优雅下线的支持。

比如Spring，他就会向JVM注册一个shutdown hook，在接收到关闭通知的时候，进行bean的销毁，容器的销毁处理等操作。

同时，作为一个成熟的框架，Spring也提供了事件机制，可以借助这个机制实现更多的优雅上下线功能。

ApplicationListener是Spring事件机制的一部分，与抽象类ApplicationEvent类配合来完成ApplicationContext的事件机制。

开发者可以实现ApplicationListener接口，监听到 Spring 容器的关闭事件（ContextClosedEvent），来做一些特殊的处理：

```
@Component
public class MyListener implements ApplicationListener<ContextClosedEvent> {

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        // 做容器关闭之前的清理工作
    }

}
```

## Dubbo的优雅上下线

因为Spring中提供了ApplicationListener接口，帮助我们来监听容器关闭事件，那么，很多web容器、框架等就可以借助这个机制来做自己的优雅上下线操作。

如tomcat、dubbo等都是这么做的。

应用在停机时，接收到关闭通知时，会先把自己标记为不接受（发起）新请求，然后再等待10s（默认是10秒）的时候，等执行中的线程执行完。

那么，之所以他能做这些事，是因为从操作系统、到JVM、到Spring等都对优雅停机做了很好的支持。

关于Dubbo各个版本中具体是如何借助JVM的shutdown hook机制、或者说Spring的事件机制做的优雅停机，我的一位同事的一篇文章介绍的很清晰，大家可以看下：

[https://www.cnkirito.moe/dubbo-gracefully-shutdown/](https://www.cnkirito.moe/dubbo-gracefully-shutdown/)

在从Dubbo 2.5 到 Dubbo 2.7介绍了历史版本中，Dubbo为了解决优雅上下线问题所遇到的问题和方案。

目前，Dubbo中实现方式如下，同样是用到了Spring的事件机制：

```
public class SpringExtensionFactory implements ExtensionFactory {
    public static void addApplicationContext(ApplicationContext context) {
        CONTEXTS.add(context);
        if (context instanceof ConfigurableApplicationContext) {
            ((ConfigurableApplicationContext) context).registerShutdownHook();
            DubboShutdownHook.getDubboShutdownHook().unregister();
        }
        BeanFactoryUtils.addApplicationListener(context, SHUTDOWN_HOOK_LISTENER);
    }
}
```

