# 典型回答
在Spring框架中，Shutdown Hook（关闭钩子）是一种机制，用于在应用程序关闭时执行一些清理操作。

Spring会向JVM注册一个shutdown hook，在接收到关闭通知的时候，进行bean的销毁，容器的销毁处理等操作。

在Spring框架中，可以使用使用AbstractApplicationContext类或其子类来注册Shutdown Hook。这些类提供了一个registerShutdownHook()方法，用于将Shutdown Hook与应用程序上下文关联起来。

很多中间件的优雅上下线的功能（优雅停机），都是基于Spring的shutdown hook的机制实现的，比如Dubbo的优雅下线。

[✅什么是Dubbo的优雅停机，怎么实现的？](https://www.yuque.com/hollis666/fo22bm/gxda8y?view=doc_embed)

还有我们经常在Spring中使用的以下两种方式，其实都是基于shutdown hook实现的。如：

1、实现DisposableBean接口，实现destroy方法：

```java
@Slf4j
@Component
public class HollisShutdownHook implements DisposableBean {

    @Override
    public void destroy() throws Exception {
        // 清理资源
    }

}
```

2、使用@PreDestroy注解

```java
@Service
public class HollisBean {
    
    @PreDestroy
    public void cleanup() {
        // 执行清理逻辑
        System.out.println("Performing cleanup before bean destruction...");
        
        // 关闭资源、释放连接等
        // ...
    }
}
```


当然，我们也可以借助Spring的事件机制，来自己注册一个hook，如下：

```java
@Component
public class HollisListener implements ApplicationListener<ContextClosedEvent> {

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        // 做容器关闭之前的清理工作
    }

}
```

可以实现ApplicationListener接口，监听 Spring 容器的关闭事件（ContextClosedEvent），来做一些特殊的处理。
