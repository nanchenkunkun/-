# 典型回答

Ribbon是Netflix推出的负载均衡组件，**但是从2020年开始，Netflix推出的很多SpringCloud组件都宣布不再更新维护了**，所以，SpringCloud就自己推出了Spring Cloud  LoadBalancer 来代替Ribbon，所以，在Spring Cloud 2020.0版本中，Ribbon已经被标记为过时，官方推荐使用Spring Cloud LoadBalancer。

LoadBalancer是Spring Cloud官方提供的负载均衡组件，可用于替代Ribbon。其使用方式与Ribbon基本兼容，可以从Ribbon进行平滑过渡。

其实LoadBalancer就是Ribbon的替代品，二者在功能和使用上没什么太大的差别，如果一定要说一说他们之间的差别的话，主要有这几个：

1. 实现方式：Ribbon是一个独立的第三方的库，需要单独引入。而Spring Cloud LoadBalancer是Spring Cloud的一个组件，集成在Spring Cloud中，可以直接使用。
2. 易用性：Ribbon是一个较为底层的负载均衡器，需要开发人员手动配置负载均衡策略和服务发现机制。而Spring Cloud LoadBalancer提供了一个更高层次的抽象，将负载均衡策略和服务发现机制的实现进行了封装，使开发人员更容易使用。
3. 生态完整：Spring Cloud LoadBalancer与Spring Cloud的其他组件紧密集成，具有更好的易用性和稳定性，而且与Ribbon相比，更加轻量级，性能更好。

# 扩展知识

## 使用示例

在项目的pom.xml文件中添加以下依赖：

```
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-loadbalancer</artifactId>
</dependency>
```


创建一个RestTemplate对象，并使用@LoadBalanced注解进行标记：

```
@Configuration
public class AppConfig {
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

实现服务调用：

```
@Service
public class MyService {
    @Autowired
    private RestTemplate restTemplate;

    public String callService() {
        String url = "http://my-service/my-api";
        return restTemplate.getForObject(url, String.class);
    }
}
```

在上面的示例中，在实际调用时，会从提供"my-service"的服务提供者中，自动选择一个可用的实例进行服务调用。

Spring Cloud LoadBalancer默认提供了一些常见的负载均衡算法，例如轮询、随机等。如果需要选择其他复杂的负载均衡算法，可以通过在应用程序配置文件中指定配置属性来进行设置：

```
spring.cloud.loadbalancer.configurations.my-service=random
```

或者使用注解@LoadBalancerClient 

```
@Configuration
@LoadBalancerClient(name = "my-service", configuration = RandomLoadBalancerConfig.class)
public class AppConfig {
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```
