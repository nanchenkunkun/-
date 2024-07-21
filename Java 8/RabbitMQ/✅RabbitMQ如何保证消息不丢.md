# 典型回答

[✅如何保障消息一定能发送到RabbitMQ](https://www.yuque.com/hollis666/fo22bm/inmrfqk0qyvsdgg3?view=doc_embed)

上文介绍了如何确保RabbitMQ的发送者把消息能够投递给RabbitMQ的Exchange和Queue，那么，Queue又是如何保证消息能不丢的呢？

RabbitMQ在接收到消息后，默认并不会立即进行持久化，而是先把消息暂存在内存中，这时候如果MQ挂了，那么消息就会丢失。所以需要通过持久化机制来保证消息可以被持久化下来。

### 队列和交换机的持久化


在声明队列时，可以通过设置`durable`参数为true来创建一个持久化队列。持久化队列会在RabbitMQ服务器重启后保留，确保队列的元数据不会丢失。

在声明交换机时，也可以通过设置`durable`参数为true来创建一个持久化交换机。持久化交换机会在RabbitMQ服务器重启后保留，以确保交换机的元数据不会丢失。

绑定关系通常与队列和交换机相关联。当创建绑定关系时，还是可以设置`durable`参数为true，以创建一个持久化绑定。持久化绑定关系会在服务器重启后保留，以确保绑定关系不会丢失。

```
@Bean
public Queue TestQueue() {
    // 第二个参数durable:是否持久化,默认是false
    return new Queue("queue-name",true,true,false);
}


@Bean
public DirectExchange mainExchange() {
  	//第二个参数durable:是否持久化,默认是false
    return new DirectExchange("main-exchange",true,false);
}
```


### 持久化消息

生产者发送的消息可以通过设置消息的`deliveryMode`为2来创建持久化消息。持久化消息在发送到持久化队列后，将在服务器重启后保留，以确保消息不会丢失。

> deliveryMode是一项用于设置消息传递模式的属性，用于指定消息的持久性级别。deliveryMode可以具有两个值：
> 1. 1（非持久化）：这是默认的传递模式。如果消息被设置为非持久化，RabbitMQ将尽力将消息传递给消费者，但不会将其写入磁盘，这意味着如果RabbitMQ服务器在消息传递之前崩溃或重启，消息可能会丢失。
> 2. 2（持久化）：如果消息被设置为持久化，RabbitMQ会将消息写入磁盘，以确保即使在RabbitMQ服务器重启时，消息也不会丢失。持久化消息对于重要的消息非常有用，以确保它们不会在传递过程中丢失。


```
Message message = MessageBuilder.withBody("hello, spring".getBytes(StandardCharsets.UTF_8)) //kp 消息体，字符集
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT) 
                .build();

rabbitTemplate.convertAndSend("simple.queue", message);
```

通过设置`deliveryMode`类实现消息的持久化。但是需要注意，将消息设置为持久化会增加磁盘I/O开销。

### 消费者确认机制


有了持久化机制后，那么怎么保证消息在持久化下来之后一定能被消费者消费呢？这里就涉及到消息的消费确认机制。

在RabbitMQ中，消费者处理消息成功后可以向MQ发送ack回执，MQ收到ack回执后才会删除该消息，这样才能确保消息不会丢失。如果消费者在处理消息中出现了异常，那么就会返回nack回执，MQ收到回执之后就会重新投递一次消息，如果消费者一直都没有返回ACK/NACK的话，那么他也会在尝试重新投递。

### 无法做到100%不丢


虽然我们通过发送者端进行异步回调、MQ进行持久化、消费者做确认机制，但是也没办法保证100%不丢，因为MQ的持久化过程其实是异步的。即使我们开了持久化，也有可能在内存暂存成功后，异步持久化之前宕机了，那么这个消息就会丢失。

如果想要做到100%不丢失，就需要引入本地消息表，来通过轮询的方式来进行消息重投。

[✅如何基于本地消息表实现分布式事务？](https://www.yuque.com/hollis666/fo22bm/xm675quxo1bc5qm8?view=doc_embed)
