# 典型回答

Zuul是一个开源的网关服务，主要用于将客户端请求路由到相应的微服务实例。它是Netflix公司开发的，现在成为了Spring Cloud生态系统中的一部分。

网关就相当于是一个前置的入口，所有的请求都要经过他，然后通过它进行服务转发和路由。

![](https://cdn.nlark.com/yuque/0/2023/png/5378072/1681548771739-ceb1ed3e-47f0-49bb-9dc0-3903123033db.png#averageHue=%233efb42&clientId=u92f3890f-303c-4&from=paste&id=ucb50631f&originHeight=840&originWidth=1646&originalType=url&ratio=1&rotation=0&showTitle=false&status=done&style=none&taskId=ub01be8ec-3bf9-480b-8f02-e0b72b0f7fc&title=)


网关能带来几个好处，首先比较容易想到的就是他承担了统一的入口和出口的角色，所有的请求都需要经过他，那么他就可以很方便的管理这些服务。

其次，因为所有请求都要经过他， 所以他可以做**负载均衡**以及**动态路由**。根据不同的负载均衡算法，或者根据请求的参数内容，进行决策选择一个合适的后端服务进行调用。

其他他还有把很多公共的服务集成进来，比如做统一的鉴权、统一的异常处理等，还可以做各种粒度的限流、降级和熔断的操作。

# 扩展知识

## 示例

以一个简单的示例为例，假设我们有两个服务，一个是 User Service，一个是 Order Service，它们的访问地址分别是：

-  http://localhost:8081/user
-  http://localhost:8082/order

我们可以在注册中心注册一个 Zuul 服务，然后在 Zuul 服务中配置路由规则，将不同的请求路由给不同的服务：

```
# application.yml

spring:
  application:
    name: api-gateway

zuul:
  routes:
    user-service:
      path: /user/**
      url: http://localhost:8081/user
    order-service:
      path: /order/**
      url: http://localhost:8082/order

```

这样，我们就可以通过访问 http://localhost:8080/user/xxx 来访问 User Service，通过访问 http://localhost:8080/order/xxx 来访问 Order Service 。

至于前面说的一些鉴权， 限流等，可以通过ZuulFilter来实现，这个是Zuul中的一个组件，用于实现请求拦截和过滤。

比如使用RateLimiter实现一个简单的限流：

```
@Component
public class RateLimitFilter extends ZuulFilter {

    private static final RateLimiter RATE_LIMITER = RateLimiter.create(10);

    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
        return 0;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() throws ZuulException {
        if (!RATE_LIMITER.tryAcquire()) {
            throw new ZuulException("Too many requests", HttpStatus.TOO_MANY_REQUESTS.value(), "Rate limit exceeded");
        }
        return null;
    }
}

```


