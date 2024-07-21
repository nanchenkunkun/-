
在Java并发编程中，ThreadLocal是用来解决线程安全问题的一个很好的工具，它可以为每个线程提供变量的独立副本，从而避免了线程间的数据共享问题。

然而，在使用线程池的场景下，在父子线程间传递线程局部变量是无法实现的，因为ThreadLocal设计上仅支持线程内部的数据隔离，而不支持线程之间的数据传递。

### 背景

在基于Java的应用开发中，尤其是在使用Spring框架、异步处理和微服务架构的系统中，经常需要在不同线程或服务间传递用户会话、数据库事务或其他上下文信息。

比如一个Web服务处理用户请求的过程中，需要记录日志，其中日志需要包含请求的唯一标识（如请求ID）。这个请求ID在进入服务时生成，并在后续的所有处理流程中使用，包括多个子任务可能会并发执行或被分配到线程池中的不同线程上执行。（分布式场景中一般是traceId）

在这种情况下，使用ThreadLocal来存储请求ID会遇到问题：并发执行的子任务无法访问到存储在父线程ThreadLocal中的请求ID，以及使用线程池时，线程的复用会导致请求ID的错误共享或丢失。

### 技术选型

为了解决这个问题，可以使用TransmittableThreadLocal（TTL），它是阿里巴巴开源的一个工具库，设计用来解决在使用线程池等会复用线程的场景下，ThreadLocal无法正确管理线程上下文的问题。

开源地址：[https://github.com/alibaba/transmittable-thread-local](https://github.com/alibaba/transmittable-thread-local) 

TransmittableThreadLocal继承自ThreadLocal，提供了跨线程的数据传递能力，能够确保父线程到子线程的值传递，同时支持线程池等场景下线程间的数据隔离。

另外，还有一个JDK自带的InheritableThreadLocal，他是用于主子线程之间参数传递的，但是，这种方式有一个问题，那就是必须要是在主线程中手动创建的子线程才可以，而在线程池中，InheritableThreadLocal就不行了。

### 具体实现
##### 引入依赖

首先，需要在项目中引入TransmittableThreadLocal的依赖。如果是Maven项目，可以添加如下依赖：

```javascript
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>transmittable-thread-local</artifactId>
    <version><!-- 使用最新版本 --></version> 
</dependency>

```

##### 使用TransmittableThreadLocal存储请求ID

```javascript
public class RequestContext {
    // 使用TransmittableThreadLocal来存储请求ID
    private static final ThreadLocal<String> requestIdTL = new TransmittableThreadLocal<>();

    public static void setRequestId(String requestId) {
        requestIdTL.set(requestId);
    }

    public static String getRequestId() {
        return requestIdTL.get();
    }

    public static void clear() {
        requestIdTL.remove();
    }
}

```

###### **创建一个线程池，并使用TTL提供的工具类确保线程池兼容TransmittableThreadLocal**：

```javascript
import com.alibaba.ttl.threadpool.TtlExecutors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadPoolUtil {
    private static final ExecutorService pool = Executors.newFixedThreadPool(10);

    // 使用TtlExecutors工具类包装原始的线程池，使其兼容TransmittableThreadLocal
    public static final ExecutorService ttlExecutorService = TtlExecutors.getTtlExecutorService(pool);

    public static ExecutorService getExecutorService() {
        return ttlExecutorService;
    }
}

```

TtlExecutors是TransmittableThreadLocal（TTL）库中的一个工具类，它提供了一种机制来包装Java标准库中的ExecutorService，ScheduledExecutorService等线程池接口的实例。

这种包装允许在使用线程池时，能够正确地传递TransmittableThreadLocal中存储的上下文数据，即使任务是在不同的线程中执行的。这对于解决在使用线程池时ThreadLocal变量值传递问题至关重要。

###### 执行并行任务，并在任务中使用RequestContext来访问请求ID：

```javascript
import java.util.stream.IntStream;

public class Application {
    public static void main(String[] args) {
        // 模拟Web应用中为每个请求设置唯一的请求ID
        String requestId = "REQ-" + System.nanoTime();
        RequestContext.setRequestId(requestId);

        try {
            ExecutorService executorService = ThreadPoolUtil.getExecutorService();

            IntStream.range(0, 5).forEach(i -> 
                executorService.submit(() -> {
                    // 在子线程中获取并打印请求ID
                    System.out.println("Task " + i + " running in thread " + Thread.currentThread().getName() + " with Request ID: " + RequestContext.getRequestId());
                })
            );
        } finally {
            // 清理资源
            RequestContext.clear();
            ThreadPoolUtil.getExecutorService().shutdown();
        }
    }
}

```


### 学习资料

[✅ThreadLocal的应用场景有哪些？](https://www.yuque.com/hollis666/fo22bm/bpm9cgs19qwlgc1k?view=doc_embed)

[✅ThreadLocal为什么会导致内存泄漏？如何解决的？](https://www.yuque.com/hollis666/fo22bm/bueq7weva8ha9f1p?view=doc_embed)

[✅父子线程之间怎么共享数据？](https://www.yuque.com/hollis666/fo22bm/adgan2125uzrsbte?view=doc_embed)

[✅有了InheritableThreadLocal为啥还需要TransmittableThreadLocal？](https://www.yuque.com/hollis666/fo22bm/fucuuyqoqv8rdkpr?view=doc_embed)
