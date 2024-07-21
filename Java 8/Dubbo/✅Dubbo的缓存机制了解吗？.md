# 典型回答

Dubbo提供了缓存机制，其主要作用是缓存服务调用的响应结果，减少重复调用服务的次数，提高调用性能。

Dubbo支持了服务端结果缓存和客户端结果缓存。

**服务端缓存**是指将服务端方法的返回结果缓存到内存中，以便下次请求时可以直接从缓存中获取结果，而不必再调用服务方法。服务端缓存可以提高响应速度和系统吞吐量。Dubbo提供了三种服务端缓存的实现方式：

- LRU Cache: 使用基于LRU(最近最少使用)算法的缓存，当缓存空间满时，会将最近最少使用的缓存清除掉。
- Thread Local Cache: 使用线程本地缓存，即每个线程都拥有一个缓存实例，缓存结果只对当前线程可见。
- Concurrent Map Cache: 使用基于ConcurrentMap的缓存，支持并发读写，相对LRU Cache和Thread Local Cache来说，缓存效率更高。

**客户端缓存**是指客户端将调用远程服务方法的返回结果缓存到内存中，以便下次请求时可以直接从缓存中获取结果，而不必再调用远程服务方法。消费端缓存可以提高系统的响应速度和降低系统的负载。Dubbo提供了两种消费端缓存的实现方式：

- LRU Cache: 使用基于LRU算法的缓存，当缓存空间满时，会将最近最少使用的缓存清除掉。
- Thread Local Cache: 使用线程本地缓存，即每个线程都拥有一个缓存实例，缓存结果只对当前线程可见。

需要注意的是，缓存虽好用，使用需谨慎，过度依赖缓存可能会出现数据不一致的问题。
# 扩展知识

## 服务端缓存

接口维度的服务端缓存配置方式支持XML和注解两种：<br />XML配置：
```
<bean id="demoService" class="org.apache.dubbo.demo.provider.DemoServiceImpl"/>
<dubbo:service interface="com.foo.DemoService" ref="demoService" cache="lru" /> 
```
注解配置方式：
```
@DubboService(cache = "lru")
public class DemoServiceImpl implements DemoService {

    private static final Logger logger = LoggerFactory.getLogger(DemoServiceImpl.class);
    @Override
    public String sayHello(String name) {
        logger.info("Hello " + name + ", request from consumer: " + RpcContext.getContext().getRemoteAddress());
        return "Hello " + name;

    }

}
```


还支持方法维度的缓存配置，同样支持XML和注解两种：

```
<bean id="demoService" class="org.apache.dubbo.demo.provider.DemoServiceImpl"/>
<dubbo:service interface="com.foo.DemoService" ref="demoService" cache="lru" />
    <dubbo:method name="sayHello" cache="lru" />
</dubbo:service>
```

```
@DubboService(methods = {@Method(name="sayHello",cache = "lru")})
public class DemoServiceImpl implements DemoService {

    private static final Logger logger = LoggerFactory.getLogger(DemoServiceImpl.class);
    @Override
    public String sayHello(String name) {
        logger.info("Hello " + name + ", request from consumer: " + RpcContext.getContext().getRemoteAddress());
        return "Hello " + name;

    }

}
```

## 客户端缓存

接口维度：

```
<dubbo:reference interface="com.foo.DemoService" cache="lru" />
```
或者：

```
@DubboReference(cache = "lru")
private DemoService demoService;
```

方法维度：

```
<dubbo:reference interface="com.foo.DemoService">
    <dubbo:method name="sayHello" cache="lru" />
</dubbo:reference>
```

或者：
```
@DubboReference(methods = {@Method(name="sayHello",cache = "lru")})
private DemoService demoService;
```
