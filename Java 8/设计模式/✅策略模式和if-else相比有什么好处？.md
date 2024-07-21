# 典型回答

策略模式是一种行为设计模式，它允许在运行时根据不同情况选择算法的不同实现。它将算法和其相应的行为封装在一个独立的类中，使得它们可以相互替换，而不会影响客户端的使用。这种模式支持开闭原则，即在不修改现有客户端代码的情况下，可以动态地添加、删除或替换算法。

策略模式相较于if-else语句，有以下几个优势：

1. 易于扩展：使用策略模式，可以方便地增加、删除或更换算法，而不需要修改原有的代码，只需要添加新的策略类即可。
2. 更好的可读性：策略模式可以将复杂的条件语句分散到不同的策略类中，使得代码更加清晰、易于理解和维护。
3. 避免大量的条件判断：在if-else语句中，可能需要写很多的条件判断，当条件越来越多时，代码变得复杂、难以维护。而使用策略模式，可以将条件判断分散到不同的策略类中，每个策略类只需要关注自己的逻辑，使得代码更加简洁。
4. 提高代码复用性：策略模式可以将一些常用的算法封装在策略类中，可以被多个客户端共享使用，从而提高代码的复用性。

一般在实际应用中，策略模式会结合工厂模式、模板方法模式一起使用。


# 扩展知识

## 示例

我们结合策略+工厂+模板方法模式，看一下如何在Spring中使用。

假设有一个订单处理系统，处理订单的流程包括如下步骤：

1. 根据订单类型选择不同的处理策略（如普通订单、团购订单、秒杀订单等）；
2. 每个订单类型的处理策略可能有所不同，但是都需要经过一些公共的处理流程，比如记录日志、验证订单信息等；
3. 处理完订单后，需要将处理结果返回给调用方。

首先，定义订单处理策略的接口：

```
public interface OrderProcessStrategy {
    void process(Order order);
}
```

然后定义一个公共的基础实现类，其中包含了订单的前置处理和后置处理：

```
public abstract class BaseOrderProcessStrategy implements OrderProcessStrategy {
   public void process(Order order){
    	//前置处理
    	checkOrder(order);

    	doProcess(order);

    	//后置处理
      //doLog(order);
  	
    }

  	public abstract void doProcess(Order order);
    public void checkOrder(Order order){
    	//订单检查业务逻辑
    }

	 public void doLog(Order order){
    	//记录日志相关代码
    }
}
```

接下来，定义不同类型订单的处理策略实现类，集成BaseOrderProcessStrategy这个抽象类，并且实现其中的doProcess方法。

```
@Component("normalOrderProcessStrategy")
public class NormalOrderProcessStrategy extends BaseOrderProcessStrategy {
    @Override
    public void doProcess(Order order) {
        // 普通订单处理逻辑
    }
}

@Component("groupOrderProcessStrategy")
public class GroupOrderProcessStrategy extends BaseOrderProcessStrategy {
    @Override
    public void doProcess(Order order) {
        // 团购订单处理逻辑
    }
}

@Component("seckillOrderProcessStrategy")
public class SeckillOrderProcessStrategy extends BaseOrderProcessStrategy {
    @Override
    public void doProcess(Order order) {
        // 秒杀订单处理逻辑
    }
}

```

然后，定义一个工厂类来创建订单处理策略实例：

```
@Component
public class OrderProcessStrategyFactory {
    @Autowired
    private ApplicationContext applicationContext;

    public OrderProcessStrategy getStrategy(String type) {
        return applicationContext.getBean(type + "OrderProcessStrategy", OrderProcessStrategy.class);
    }
}
```

或者：
```

public static class OrderProcessStrategyFactory {
    @Autowired
    private ConcurrentHashMap<String,OrderProcessStrategy> orderProcessStrategyMaps;

    public OrderProcessStrategy getStrategy(String type) {
        return orderProcessStrategyMaps.get(type + "OrderProcessStrategy");
    }
}
```

最后，定义一个订单处理服务类，使用策略模式来处理订单：

```
@Service
public class OrderProcessService {
    @Autowired
    private OrderProcessStrategyFactory strategyFactory;

    public void processOrder(Order order) {
        // 选择处理策略
        OrderProcessStrategy strategy = strategyFactory.getStrategy(order.getType());

        // 执行处理流程
        strategy.process(order);
    }
}
```

上述代码中，通过使用工厂模式来创建不同类型的订单处理策略实例，并使用策略模式来处理订单，避免了复杂的 if-else 语句，代码更加简洁易读，也更加易于维护。同时，使用模板方法模式来封装公共的处理流程，避免了代码重复。
