# 典型回答

什么是消费端限流，这是一种保护消费者的手段，假如说，现在是业务高峰期了，消息有大量堆积，导致MQ消费者需要不断地进行消息消费，很容易被打挂，甚至重启之后还是会被大量消息涌入，继续被打挂。

为了解决这个问题，RabbitMQ提供了basicQos的方式来实现消费端限流。我们可以在消费者端指定最大的未确认消息数，当达到这个限制时，RabbitMQ将不再推送新的消息给消费者，直到有一些消息得到确认。

想要实现这个功能，首先需要把自动提交关闭。

```
channel.basicConsume(queueName, false, consumer);
```

接着进行限流配置：
```
/**
  * 限流设置:  
	*	prefetchSize：每条消息大小的设置，0是无限制
  * prefetchCount:标识每次推送多少条消息
  * global:false标识channel级别的  true:标识消费者级别的
  */
 channel.basicQos(0,10,false);
```

如以上配置，可以实现消费者在处理完一条消息后，才会获取下一条消息。

然后再在消费者处理完一条消息之后，手动发送确认消息给到RabbitMQ，这样就可以拉取下一条消息了：

```
channel.basicAck(deliveryTag, false); // 发送确认
```

完整代码如下：

```
import com.rabbitmq.client.*;

public class ConsumerWithFlowControl {
    private static final String QUEUE_NAME = "my_queue";
    private static final String HOST = "localhost";

    public static void main(String[] args) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(HOST);

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            // 声明队列
            channel.queueDeclare(QUEUE_NAME, false, false, false, null);

            // 设置消费者限流，每次只获取一条消息
            int prefetchCount = 1;
            channel.basicQos(prefetchCount);

            // 创建消费者
            DefaultConsumer consumer = new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    String message = new String(body, "UTF-8");
                    System.out.println("Received: " + message);

                    // 模拟消息处理耗时
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }

                    // 发送消息确认
                    channel.basicAck(envelope.getDeliveryTag(), false);
                }
            };

            // 指定队列，并关闭自动确认
            channel.basicConsume(QUEUE_NAME, false, consumer);

           
        }
    }
}

```
