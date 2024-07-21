# 典型回答

在Web应用开发中，确保应用可以平稳可靠的关闭是至关重要的。在我们常用的Spring Boot中其实提供了内置功能来优雅地处理应用程序的关闭的能力。

先说一下啥是优雅停机，其实他指的是以受控方式终止应用程序的过程，允许它完成任何正在进行的任务，释放资源，并确保数据的完整性。与突然终止应用程序不同，优雅停机确保所有进程都得到优雅停止，以防止潜在的数据损坏或丢失。

从Spring Boot 2.3开始，SpringBoot内置了优雅停机的功能。想要启用优雅停机也非常简单，你只需在你的application.properties文件中添加一行代码：

```yaml
server.shutdown=graceful
```

通过这个设置，当你停止服务器时，它将不再接受新的请求。并且服务器也不会立即关闭，而是等待正在进行的请求处理完。

这个等待的时间我们是可以自定义的：

```yaml
spring.lifecycle.timeout-per-shutdown-phase=2m
```

默认的等待时长是30秒，我们通过以上配置可以将这个等待时长延长直2分钟。

# 扩展知识

## Spring Boot Actuator Shutdown Endpoint

想要在Spring Boot Actuator中启用优雅停机，需要做如下配置。

首先增加Maven依赖：

```yaml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

然后增加配置项，

```yaml
management.endpoints.web.exposure.include=*
management.endpoint.shutdown.enabled=true
```

要优雅停机应用，可以使用HTTP POST请求来调用关闭端点。例如，可以使用curl命令或工具来发送POST请求：

```yaml
curl -X POST http://localhost:8080/actuator/shutdown
```

当你发送POST请求到/actuator/shutdown时，应用将接收到关闭命令并开始进行优雅停机。应用会等待一段时间以完成正在进行的请求处理，然后关闭。
