# 典型回答

Hystrix熔断器是用来防止级联失败的，并允许系统快速失败和快速恢复。他的原理是通过模拟电路中的断路器（熔断器）来实现的，当某个部分发生故障时，断路器会切断电流，防止故障扩散。

在Hystrix中，这种机制用于管理对依赖服务的调用，特别是在这些服务表现不稳定或响应延迟时。

Hystrix断路器主要有三种状态：

- **关闭 **
   - 熔断器在默认情况下下是呈现关闭的状态，而熔断器本身带有计数功能，每当错误发生一次，计数器也就会进行“累加”的动作，到了一定的错误发生次数断路器就会被“开启”，这个时候亦会在内部启用一个计时器，一旦时间到了就会切换成半开启的状态。
- **开启 **
   - 在开启的状态下任何请求都会“直接”被拒绝并且抛出异常讯息。
- **半开启 **
   - 在此状态下断路器会允许部分的请求，如果这些请求都能成功通过，那么就意味着错误已经不存在，则会被切换回关闭状态并重置计数。倘若请求中有“任一”的错误发生，则会恢复到“开启”状态，并且重新计时，给予系统一段休息时间。

![image.png](https://cdn.nlark.com/yuque/0/2024/png/5378072/1714544112919-0df7501a-9190-49f7-be5d-89088ec85eb5.png#averageHue=%23fcfbf5&clientId=uf3ba2905-ce60-4&from=paste&height=1442&id=u3b53d6e9&originHeight=1442&originWidth=2138&originalType=binary&ratio=1&rotation=0&showTitle=false&size=265544&status=done&style=none&taskId=uaa710ec8-6c9d-4a05-8924-8eed621369e&title=&width=2138)

Hystrix通过一系列指标来确定是否需要开启断路器，主要指标包括：

- **请求的数量**：在一定时间窗口内，只有请求数量超过了一定阈值，断路器的状态才会评估是否需要开启。这防止了在请求量很低时因一两个请求失败就触发断路器。
- **错误百分比**：计算在过去的一段时间内，失败请求占总请求的百分比。如果这一比例超过设定的阈值，断路器将开启。

在代码中，开发者可以使用Hystrix命令来包装对下游服务的调用。这些命令封装了服务调用的细节，包括超时、失败、回退机制等。当断路器开启时，这些命令会自动执行配置的回退逻辑，而不是执行实际的服务调用。

如果在微服务系统的调用过程中，引入熔断器，那么整个系统将天然具备以下能力：

- **快速失败**：当因为调用远程服务失败次数过多，熔断器开启时，上游服务对于下游服务的调用就会快速失败，这样可以避免上游服务被拖垮。
- **无缝恢复**：因为熔断器可以定期检查下游系统是否恢复，一旦恢复就可以重新回到关闭状态，所有请求便可以正常请求到下游服务。使得系统不需要人为干预。

# 扩展知识

以下是一个使用Hystrix的Java代码示例，演示如何创建一个Hystrix命令来包装对某个依赖服务的调用，并提供回退逻辑以应对服务调用失败的情况。

假设我们有一个服务方法 `getUserById`，用于从远程用户服务获取用户详情。我们将使用Hystrix来包装这个服务调用，以便在远程服务失败时提供回退逻辑。
### 1: 添加依赖

首先，确保在你的项目中添加了Hystrix依赖。如果你使用Maven，可以在`pom.xml`文件中添加以下依赖：

```xml
<dependencies>
    <dependency>
        <groupId>com.netflix.hystrix</groupId>
        <artifactId>hystrix-core</artifactId>
        <version>1.5.18</version> <!-- 使用适当的版本 -->
    </dependency>
</dependencies>
```

### 2: 创建Hystrix命令

我们将创建一个继承自 `HystrixCommand` 的类，用于封装对远程服务的调用。

```java
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;

public class GetUserCommand extends HystrixCommand<String> {
    private final int userId;

    public GetUserCommand(int userId) {
        super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("UserServiceGroup"))
                    .andCommandPropertiesDefaults(
                        HystrixCommandProperties.Setter()
                            .withExecutionTimeoutInMilliseconds(1000)
                            .withCircuitBreakerErrorThresholdPercentage(50)
                            .withCircuitBreakerRequestVolumeThreshold(20)
                            .withCircuitBreakerSleepWindowInMilliseconds(5000)
                    ));
        this.userId = userId;
    }

    @Override
    protected String run() throws Exception {
        // 模拟远程服务调用
        return remoteCallToGetUser(userId);
    }

    @Override
    protected String getFallback() {
        return "Hollis 的八股文用户";
    }

    private String remoteCallToGetUser(int userId) {
        // 这里应该是实际调用远程服务的代码，例如使用HTTP客户端等
    }
}
```

- 在`GetUserCommand`构造函数中，我们设置了Hystrix命令的几个关键属性，如执行超时时间、断路器的错误百分比阈值、请求量阈值和“sleep window”。
- `run()`方法包含实际调用远程服务的逻辑。
- 当调用失败或由于断路器开启而阻断调用时，`getFallback()`方法会被调用，返回一个默认值或其他回退逻辑。
### 3: 使用Hystrix命令

你可以在应用中如下使用`GetUserCommand`来获取用户信息：

```java
public class UserService {
    public String getUserById(int userId) {
        GetUserCommand command = new GetUserCommand(userId);
        return command.execute(); // 同步执行
        // 或者使用 command.queue() 来异步执行
    }
}
```

通过这种方式，Hystrix帮助我们管理依赖服务的不确定性和失败，从而增强系统的稳定性和弹性。
## <br />
