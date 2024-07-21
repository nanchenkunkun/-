# 典型回答

[✅如何保障消息一定能发送到RabbitMQ](https://www.yuque.com/hollis666/fo22bm/inmrfqk0qyvsdgg3?view=doc_embed)

想要保证发送者一定能把消息发送给RabbitMQ，一种是通过confirm机制，另外一种就是通过事务机制。

RabbitMQ的事务机制，允许生产者将一组操作打包成一个原子事务单元，要么全部执行成功，要么全部失败。事务提供了一种确保消息完整性的方法，但需要谨慎使用，因为它们对性能有一定的影响。

RabbitMQ是基于AMQP协议实现的，RabbitMQ中，事务是通过在通道（Channel）上启用的，与事务机制有关的方法有三个：

- txSelect()：将当前channel设置成transaction模式。
- txCommit()：提交事务。
- txRollback()：回滚事务。

我们需要先通过txSelect开启事务，然后就可以发布消息给MQ了，如果txCommit提交成功了，则消息一定到达了RabbitMQ，如果在txCommit执行之前RabbitMQ实例异常崩溃或者抛出异常，那我们就可以捕获这个异常然后执行txRollback进行回滚事务。

所以， 通过事务机制，我们也能保证消息一定可以发送给RabbitMQ。

以下，是一个通过事务发送消息的方法示例：

```
import com.rabbitmq.client.*;

public class RabbitMQTransactionExample {

    public static void main(String[] args) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            // 启用事务
            channel.txSelect();

            String exchangeName = "my_exchange";
            String routingKey = "my_routing_key";

            try {
                // 发送第一条消息
                String message1 = "Transaction Message 1";
                channel.basicPublish(exchangeName, routingKey, null, message1.getBytes());

                // 发送第二条消息
                String message2 = "Transaction Message 2";
                channel.basicPublish(exchangeName, routingKey, null, message2.getBytes());

                // 模拟一个错误
                int x = 1 / 0;

                // 提交事务（如果没有发生错误）
                channel.txCommit();

                System.out.println("Transaction committed.");
            } catch (Exception e) {
                // 发生错误，回滚事务
                channel.txRollback();
                System.err.println("Transaction rolled back.");
            }
        }
    }
}

```



