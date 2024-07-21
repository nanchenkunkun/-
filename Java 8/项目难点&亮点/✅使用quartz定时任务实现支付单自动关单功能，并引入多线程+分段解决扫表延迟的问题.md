### 背景

我负责的订单系统模块，有一个功能就是需要实现订单的到期自动关闭，这功能以前其实是有的，但是后来我发现经常有一些订单，明明已经到期了，但是还是没有正常被关闭，就导致已超时的订单后来有支付成功的情况。

后来经过排查，是因为之前的实现方式比较简单，是基于JDK自带的delayQueue实现的，大致的代码如下：

```
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

class Order implements Delayed {
    private String orderId;
    private long createTime;
    private long closeTime;

    public Order(String orderId, long delayInMinutes) {
        this.orderId = orderId;
        this.createTime = System.currentTimeMillis();
        this.closeTime = this.createTime + TimeUnit.MINUTES.toMillis(delayInMinutes);
    }

    public String getOrderId() {
        return orderId;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        long delay = closeTime - System.currentTimeMillis();
        return unit.convert(delay, TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed other) {
        if (this == other) return 0;
        long diff = getDelay(TimeUnit.MILLISECONDS) - other.getDelay(TimeUnit.MILLISECONDS);
        return (int) (diff);
    }
}

public class OrderAutoCloser {
    public static void main(String[] args) {
        DelayQueue<Order> delayQueue = new DelayQueue<>();

        // 创建订单并将其添加到DelayQueue中
        Order order1 = new Order("Order1", 30); // 30分钟后自动关闭
        Order order2 = new Order("Order2", 15); // 15分钟后自动关闭
        delayQueue.offer(order1);
        delayQueue.offer(order2);

        // 启动后台线程来处理订单关闭
        Thread closerThread = new Thread(() -> {
            while (true) {
                try {
                    Order order = delayQueue.take();
                    System.out.println("Closing order: " + order.getOrderId());
                    // 在这里执行订单关闭的逻辑
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        closerThread.start();
    }
}

```

在创建订单的时候，就指定好自动关闭的时间，并且把订单放入delayQueue中，借助delayQueue来实现到期关闭的功能。

但是后来发现一个比较大的问题，那就是delayQueue是依托于JVM的，当应用崩溃了，或者正常的发布重启过程时，delayQueue就都没有了，这时候里面的订单就无法被关闭了。

### 技术选型

关于这个问题，其实就是实现一个订单的到期自动关闭的功能，实现的方式有很多，包括了自己扫表、依赖延迟消息、依赖Redis等。

各个方案都有自己的优缺点，但是因为我们这个订单量一开始其实并不大，所以就选择了一个最简单的方案就是自己用定时任务扫表的方式来实现。

这里用了Quartz，这是一个功能强大的调度框架，可以用于执行定时任务。


### 具体实现

```
@Component
public class OrderAutoCloseQuartz extends QuartzJobBean{

    @Autowired 
    private OrderMapper orderMapper;

    @Override
    @Transactional 
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        //扫描订单数据
      	//执行关单操作
    }
}
```

```
@Configuration
public class OrderQuartzConfig {
    
    @Bean
    public JobDetail orderjobDetail() {
        return JobBuilder
                .newJob(OrderAutoCloseQuartz.class) 
                .withIdentity("orderAutoCloseQuartz") 
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger orderTrigger() {
        CronScheduleBuilder scheduleBuilder 
            = CronScheduleBuilder.cronSchedule("0 0/1 * * * ?");
       //调度器时钟,时间表达式，每隔一分钟执行一次
			return TriggerBuilder
                .newTrigger()
                .forJob(orderjobDetail())
                .withIdentity("orderAutoCloseQuartz")
                .withSchedule(scheduleBuilder).build();
    }
}
```


以上是使用Quartz实现定时任务扫表关单的部分代码，这个方案对于数据量少的订单关单是非常简单高效的。

但是如果随着业务量增大，那么一次扫表可能处理不完，那么就需要引入多线程，并行的执行。那么多线程在扫描的时候怎么避免扫描到重复的数据呢？有一个好的办法，就是可以通过分段的思想进行数据隔离。举个例子：

```java
Long minId = messageService.getMinInitId();


for(int i=1;i<= threadPool.size();i++){
    Long maxId = minId + segmentSize()*i;

    List<Message> messages = messageService.scanInitMessages(minId,maxId);

    proccee(messages);
    minId = maxId + 1;
}
```

那么就可以把以上这段代码，结合到定时任务执行的OrderAutoCloseQuartz中，在这里面进行多线程的扫表和处理。可以大大提升效率。


### 学习资料

[✅订单到期关闭如何实现](https://www.yuque.com/hollis666/fo22bm/tg0ehg?view=doc_embed)

[✅定时任务扫表的方案有什么缺点？](https://www.yuque.com/hollis666/fo22bm/bgr91vskph8odcsr?view=doc_embed)


