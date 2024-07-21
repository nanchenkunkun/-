# 典型回答

[✅什么是SPI，和API有啥区别](https://www.yuque.com/hollis666/fo22bm/eltpur?view=doc_embed)

在Java中，SPI（Service Provider Interface）是一个为软件设计的扩展机制，允许第三方为某些接口提供实现。不同的框架和库可能会有不同的SPI机制，如JDK、Dubbo都支持。

他的的主要区别如下：

### 懒加载 vs 预加载

- JDK 的 `ServiceLoader` 会在使用 `ServiceLoader.load()` 方法时加载所有可用的服务实现，这是一种预加载方式。
- Dubbo 的 SPI 支持懒加载，即只有当实际需要使用服务时，才会加载服务的实现。这可以提高应用启动速度，并减少资源消耗。

### 扩展点自动激活

- JDK 的 SPI 不支持根据条件自动选择和激活具体的实现。
- Dubbo 的 SPI 支持扩展点自动激活功能。可以通过键值对的方式在配置文件中指定条件，当满足条件时自动选择对应的实现。

[Dubbo的SPI扩展机制之自动激活](https://cn.dubbo.apache.org/zh-cn/blog/2022/08/07/7-dubbo%e7%9a%84spi%e6%89%a9%e5%b1%95%e6%9c%ba%e5%88%b6%e4%b9%8b%e8%87%aa%e5%8a%a8%e6%bf%80%e6%b4%bb%e6%89%a9%e5%b1%95activate%e6%ba%90%e7%a0%81%e8%a7%a3%e6%9e%90/)

### 扩展点自动装配

- JDK 的 SPI 不支持依赖注入，服务提供者需要自己管理依赖或使用其他方式注入依赖。
- Dubbo 的 SPI 支持依赖注入。Dubbo 可以自动注入扩展点所需的其他扩展点，使得开发者可以很方便地在一个扩展实现中使用其他扩展服务。

[扩展点加载](https://cn.dubbo.apache.org/zh-cn/docsv2.7/dev/spi/#%E6%89%A9%E5%B1%95%E7%82%B9%E8%87%AA%E5%8A%A8%E8%A3%85%E9%85%8D)

### 配置文件的位置和格式

- 在 JDK 中，服务提供者的配置文件放置在 `META-INF/services` 目录下，文件名是完整的接口名称，文件内容是实现类的全限定名列表。
- 在 Dubbo 中，配置文件通常放在 `META-INF/dubbo` （也可以是其他目录，如 `META-INF/dubbo/internal`），文件名仍然是接口全名。不过，文件内容可以包含键值对，提供更丰富的配置选项和描述。

### AOP 支持

- JDK 的 SPI 没有内建的方法来支持面向切面编程（AOP）。
- Dubbo 的 SPI 机制支持通过 `wrapper` 类包装扩展点，从而支持 AOP 风格的服务增强。这可以用于日志记录、事务管理等。

### 扩展名

- Dubbo 允许开发者为服务实现指定一个易于理解的名称（即扩展名），使得配置更加直观和简洁。

所以，相比之下，Dubbo 的 SPI 机制因为支持懒加载，所以性能会更好一些，并且他提供了更加丰富的扩展性和灵活性。并且内置了一些更加强大的功能。

# 扩展知识
## JDK 的 SPI 用法

JDK 的 SPI 主要用于允许服务提供者提供服务接口的多种实现。常见的使用场景包括数据库驱动加载、日志框架等。JDK 的 SPI 机制主要涉及三个部分：

- Service Interface：这是被服务提供者实现的接口。
- Service Provider Registration：实现该接口的服务提供者必须在 META-INF/services 目录下创建一个名字为接口全限定名的文件，文件内部列出实现该接口的具体实现类的全限定名。
- Service Loading：通过 ServiceLoader 类加载服务。它会查找并加载服务接口的实现，通常是在运行时动态查找。

```java
public interface MyService {
    void serviceMethod();
}

public class MyServiceImpl implements MyService {
    @Override
    public void serviceMethod() {
        System.out.println("Service Method Implemented");
    }
}

// 在 META-INF/services 目录下创建文件，名为 com.example.MyService
// 文件内容：
// com.example.MyServiceImpl

```

```java
import java.util.ServiceLoader;

public class TestSPI {
    public static void main(String[] args) {
        ServiceLoader<MyService> services = ServiceLoader.load(MyService.class);
        for (MyService service : services) {
            service.serviceMethod();
        }
    }
}

```

## Dubbo 的 SPI 用法

Dubbo 的 SPI 机制是对 JDK 的 SPI 的增强，提供了更加灵活的动态扩展能力。Dubbo SPI 支持按需加载、自动注入等功能。在 Dubbo 中，SPI 配置文件通常位于 META-INF/dubbo 目录下。

创建接口和实现类，然后在 META-INF/dubbo 中配置服务：

```java
public interface PrintService {
    void print(String message);
}

public class SimplePrintService implements PrintService {
    @Override
    public void print(String message) {
        System.out.println("Message: " + message);
    }
}

// 在 META-INF/dubbo/com.example.PrintService 文件
// 内容：
// simplePrintService=com.example.SimplePrintService
```

使用 Dubbo 的 ExtensionLoader 来加载实现：

```java
PrintService printService = ExtensionLoader.getExtensionLoader(PrintService.class).getExtension("simplePrintService");
printService.print("Hello Dubbo SPI");
```
