# 典型回答

InheritableThreadLocal是用于主子线程之间参数传递的，但是，这种方式有一个问题，那就是必须要是在主线程中手动创建的子线程才可以，而现在池化技术非常普遍了，很多时候线程都是通过线程池进行创建和复用的，这时候InheritableThreadLocal就不行了。

TransmittableThreadLocal是阿里开源的一个方案 （开源地址：[https://github.com/alibaba/transmittable-thread-local](https://github.com/alibaba/transmittable-thread-local) ） ，这个类继承并加强InheritableThreadLocal类。用来实现线程之间的参数传递，一经常被用在以下场景中：

1. 分布式跟踪系统 或 全链路压测（即链路打标）
2. 日志收集记录系统上下文
3. Session级Cache
4. 应用容器或上层框架跨应用代码给下层SDK传递信息

# 扩展知识
## 使用方式

先需要导入依赖：

```
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>transmittable-thread-local</artifactId>
    <version>2.14.2</version>
</dependency>
```

对于简单的父子线程之间参数传递，可以用以下方式：

```
TransmittableThreadLocal<String> context = new TransmittableThreadLocal<>();

// 在父线程中设置
context.set("value-set-in-parent");

// 在子线程中可以读取，值是"value-set-in-parent"
String value = context.get();
```

如果在线程池中，可以用如下方式使用：

```
TransmittableThreadLocal<String> context = new TransmittableThreadLocal<>();

// 在父线程中设置
context.set("value-set-in-parent");

Runnable task = new RunnableTask();
// 额外的处理，生成修饰了的对象ttlRunnable
Runnable ttlRunnable = TtlRunnable.get(task);
executorService.submit(ttlRunnable);


// Task中可以读取，值是"value-set-in-parent"
String value = context.get();
```

除了Runnable，Callable也支持：

```
TransmittableThreadLocal<String> context = new TransmittableThreadLocal<>();


// 在父线程中设置
context.set("value-set-in-parent");

Callable call = new CallableTask();
// 额外的处理，生成修饰了的对象ttlCallable
Callable ttlCallable = TtlCallable.get(call);
executorService.submit(ttlCallable);


// Call中可以读取，值是"value-set-in-parent"
```

也可以直接用在线程池上，而不是Runnable和Callable上：

```
ExecutorService executorService = ...
// 额外的处理，生成修饰了的对象executorService
executorService = TtlExecutors.getTtlExecutorService(executorService);

TransmittableThreadLocal<String> context = new TransmittableThreadLocal<>();


// 在父线程中设置
context.set("value-set-in-parent");

Runnable task = new RunnableTask();
Callable call = new CallableTask();
executorService.submit(task);
executorService.submit(call);


// Task或是Call中可以读取，值是"value-set-in-parent"
String value = context.get();
```
