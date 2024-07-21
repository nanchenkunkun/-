# 典型回答

Feign是Spring Cloud中的一个声明式的HTTP客户端库，用于简化编写基于HTTP的服务调用代码。但是从Spring Cloud 2020版本开始，官方宣布Feign将不再维护和支持，推荐使用OpenFeign作为替代方案。

但是，随着SpringCloud 2022的发布，官方宣布OpenFeign将被视为功能完整。这意味着Spring Cloud团队将不再向模块添加新特性。只会修复bug和安全问题。

但是，在Spring 6.0 发布之后，Spring内置了一个HTTP客户端——@HttpExchange （[https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/service/annotation/HttpExchange.html](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/service/annotation/HttpExchange.html) ），建议大家使用这个客户端进行HTTP调用。

# 扩展知识

## @HttpExchange使用

想要使用这个新的HTTP客户端，需要Spring升级到6.0，或者SpringBoot升级到3.0版本，然后再POM中依赖spring-web。以下两个二选一：

```java
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

在 Spring 6.0中，可以让HTTP 服务接口带有@HttpExchange。那么这个接口方法就会被视为 HTTP 端点，目前支持的注解有以下几个：

- @GetExchange  HTTP GET 请求
- @PostExchange  HTTP POST 请求
- @PutExchange  HTTP PUT 请求
- @PatchExchange  HTTP PATCH 请求
- @DeleteExchange  HTTP DELETE 请求

首先，我们自己定义一个HTTP接口，定义一个实体类：

```java
public class User implements Serializable {

    private String name;
    private Integer age;
    // Constructor、Getter、Setter
    @Override
    public String toString() {
        return name + ":" + age;
    }
}

```

然后定义一个Controller
```java
@GetMapping("/users")
public List<User> list() {
    return IntStream.rangeClosed(20, 25)
            .mapToObj(i -> new User("Hollis",  i))
            .collect(Collectors.toList());
}
```

以上，服务在启动后，通过`http://localhost:8080/users`地址访问后会得到10个我生成的用户信息。

有了一个HTTP接口之后，用@HttpExchange 调用方式如下：

```java
public interface UserApiService {
    @GetExchange("/users")
    List<User> getUsers();
}
```

还需要定义一个用于HTTP调用的client：

```java
@Configuration
public class WebConfiguration {

    @Bean
    public WebClient  webClient() {
        return WebClient.builder()
                                .baseUrl("https://localhost:8080")
                                .build();
    }

    @Bean
    UserApiService userApiService(){
        HttpServiceProxyFactory httpServiceProxyFactory =
                HttpServiceProxyFactory.builder(WebClientAdapter.forClient(webClient()))
                        .build();
        return httpServiceProxyFactory.createClient(UserApiService.class);
    }
    	
}
```

然后就可以调用了，如：

```java
@SpringBootTest
class UsersTests {

    @Autowired
    private UserApiService userApiService;

    @Test
    public void testGetUsers(){
       List<User> users = userApiService.getUsers();
       Assert.assertEquals(users.size(),10);
    }
    
}
```

