# 典型回答

Spring Cloud通常被用来构建分布式架构，而分布式中，就需要解决多个微服务之间的通信问题。多个应用分别部署在不同的机器上，那么想要让他们之间互相通信，有以下几种常见的办法（这里只介绍下有哪些以及主要的区别，各自原理后续会单独章节展开。）

### RESTful 调用

最常见的方式是使用RESTful API进行服务间的通信。Spring Cloud利用Spring MVC提供了强大的REST客户端和服务端支持。

比如我们的一个服务通过Spring MVC暴露出一个HTTP接口出来，然后另一个服务通过**RestTemplate**或**WebClient**来进行HTTP调用。

```java
@Autowired
private RestTemplate restTemplate;

public MyObject callOtherService() {
    return restTemplate.getForObject("http://other-service/api/data", MyObject.class);
}
```

### 使用Feign客户端（常用）

Feign是一个声明式的Web服务客户端，使得编写Web服务客户端更加容易。

通过创建一个接口并用注解来配置请求的细节，Feign可以自动处理请求的发送和结果的映射。Spring Cloud对Feign进行了集成，使其成为在微服务架构中实现服务间调用的一个流行选择。

```java
@FeignClient("other-service")
public interface OtherServiceClient {
    @GetMapping("/api/data")
    MyObject getData();
}
```

### Spring Cloud Gateway（常用+推荐）

Spring Cloud Gateway是基于请求路由的API网关。它允许你根据请求的不同特征（如路径、头部、请求参数等）将请求路由到不同的后端服务上。

```java
spring:
  cloud:
    gateway:
      routes:
        - id: auth-service
          uri: lb://auth-service
          predicates:
            - Path=/auth/**
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/user/**

```

如以上配置，当我们访问这个gateway网关应用时，如果路径是/user/**，那么他就会把请求路由给我们的user-service应用来处理。

[✅为什么需要SpringCloud Gateway，他起到了什么作用？](https://www.yuque.com/hollis666/fo22bm/ow7cnpaa2du8zvv5?view=doc_embed)

通常我们也会在网关中配合Spring Cloud LoadBalancer来实现客户端负载均衡的功能。

### RPC调用（常用+推荐）

在SpringCloud中，也可以集成很多RPC框架进行服务间的调用，这也是非常常见的，比如gRPC，Dubbo等。

```java
package com.example.consumer;

import com.example.api.GreetingService;
import org.apache.dubbo.config.annotation.Reference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GreetingController {

    @Reference
    private GreetingService greetingService;

    @GetMapping("/greet")
    public String greet(@RequestParam String name) {
        return greetingService.sayHello(name);
    }
}
```
<br />
### Spring Cloud Stream（常用+推荐）

服务间通信，除了互相调用以外，还可以基于消息的通信，Spring Cloud Stream提供了一个统一的方式来发送和接收消息。它抽象了底层消息中间件（如RabbitMQ、Kafka等），允许开发者使用统一的API来处理消息。



