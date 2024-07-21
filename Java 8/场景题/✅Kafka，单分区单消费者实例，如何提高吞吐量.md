# 典型回答

在Kafka中，想要提升吞吐量，有效的手段就是增加消费者实例以及增加更多的partition，但是如果不能增加消费者实例，也不能新增更多的partition，那么如何通过技术手段提高吞吐量呢？

主要有以下几个方面可以做：

1、异步消费<br />2、增加消费的线程数<br />4、消息压缩或组合<br />4、调整Kafka参数

### 异步消费

想要提升消费者的消费速度，我们可以快速的处理消息，有一个办法就是在接收到消息之后，先本地落库，然后落库成功之后就直接提交偏移量。

然后在用异步的方式消费这些任务。为了提升速度，我们可以在消费消息的线程中，本地消息保存成功之后，直接起一个异步线程来处理这个消息，如果成功，则直接把消息删除掉，如果失败，那么依赖本地消息重试。

那有人就会问了，这样做，还有消息中间件有意义吗？

当然有了，消息中间件的目的有很多个，比如解耦、异步、削峰填谷。我们在消费者端，接到消息后本地存下来再执行，不影响我们依然做到了解耦、异步和削峰填谷。

### 多线程消费

如果不想用本地落库的方式，那么也可以直接用多线程来执行。

我们可以通过配置，让kafka一次性多拉取一些消息，在多个消息都拉取到之后，通过异步线程池的方式来并发执行。

这种方案可以借助多线程来并发消费一批消息，从而提升并发度，提高吞吐量。但是这个方案存在一个比较大的问题，那就是异步线程多个消息的处理时间不同，可能会导致偏移量的提交并不能按照顺序，那么这个过程可能就会存在消息的丢失和重复消费的情况。

那么，有什么办法可以解决这个问题呢？

**第一种方案**，我们可以把**多个线程编排起来**执行，当所有的线程都消费成功之后，把这其中最大的偏移量提交了就行了。比如我们可以使用CompletableFuture轻松的实现这个功能，CompletableFuture即自带线程池，又支持任务分片，非常适合这种场景，如：

```
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class KafkaConsumerExample {

    private KafkaConsumer<String, String> consumer;
    private List<ConsumerRecord<String, String>> records = new ArrayList<>();
    
    public void consume(int batchSize) {
        // 从 Kafka 中拉取 batchSize 条消息
        records = consumer.poll(batchSize);
        
        // 使用 CompletableFuture 并发执行消息处理
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(records.stream()
        .map(detail -> CompletableFuture.supplyAsync(() -> {
            // 处理消息逻辑
            // ...
						//如果消费失败，直接throw Exception
            return null;
        })).toArray(CompletableFuture[]::new));        

      	// 等待所有任务执行完毕后，提交最大的偏移量
        allFutures.whenComplete((v,e) -> {
        	if(e == null){
          	//未发生异常，则提交最大的偏移量
          	long maxOffset = records.stream().mapToLong(ConsumerRecord::offset).max().orElse(-1L);
            consumer.commitSync(Collections.singletonMap(consumer.assignment().iterator().next(), new OffsetAndMetadata(maxOffset + 1)));
          }else{
          	//有消息消费失败，执行失败处理逻辑
          }
        });
    }
}

```

这样可能存在一个问题，就是如果其中某个消息执行失败了，就会导致没办法正确提交偏移量，导致很多消息会重复消费。很多时候如果我们做好了幂等，重复消费的问题理论上可以忽略，但是如果要解决的话，我们也有一些办法。

比如如果有失败的，那么就想办法把失败的这个消息之前的偏移量都提交了，之后的不提交。可以减少一些重复消费的消息的数量。或者我们干脆把失败的消息落库，通过扫表的方式做重试。

**第二种方案，**因为题目中说了我们只有一个partition和一个消费者，所以正常情况下，偏移量应该是连续的，所以我我们在一次性拉取多个消息之后，可以知道这一批消息的最大偏移量和最小偏移量是多少。这时候可以记录下来，并且在每一个消息消费成功之后，也把他的偏移量记录下来。

可以用数据库或者redis，如果是这种只有单实例的情况，其使用**本地缓存**也可以，既简单，又方便，又不会有分布式一致性问题，唯一的缺点就是机器重启可能会丢，但是这个问题不是很大，大不了重复消费呗，做好幂等就行了。

这样在一定的时间延迟之后，我们就能得到一些已经处理过的消息的偏移量。比如100,101,102,104,105。这时候我们就从小到大，一直找到第一个不连续的偏移量，把他提交了，即102。这种方案需要借助第三方的一个存储。

借助这个方案，我们可以解决消息丢失的问题，因为103这个消息没成功，但是偏移量我们提交的是102的这个，所以这时候103的消息会重发，103,104,105,106.107会在下一次被拉取到。这时候我们还是多线程消费，同时判断104 和 105已经消费过了，就不需要重新消费了，只需要消费另外几个就行了。

### 消息压缩和组合

Kafka支持多种压缩算法，如gzip、snappy、lz4等，我们可以适当的采用合适的算法将消息进行压缩，消息内容小就可以减少网络传输的数据量，从而提升整体的吞吐量。

除了压缩之外，组合也是一个好的办法，如果消息的生产者可以配合我们一起做改造的话，我们可以要求生产者在发送消息的时候，把多条消息组合到一起作为一个大消息发送。然后消费者收到之后再把消息解组之后消费。这样也能降低消息的数量，从而提升系统的吞吐量。

### 调整kafka参数

前面我们提到过的异步消费、多线程消费、压缩等，都是有一些前提的，比如要把offset的提交方式改为手动提交，每次拉取消息的数量、压缩方式等等。

```
enable.auto.commit = false //手动提交

fetch.max.bytes = 104857600 //设置每次拉取请求的最大数据量为100MB
max.poll.records = 50 // 设置每次调用poll()方法最多拉取50条记录

compression.type = gzip //使用gzip进行消息压缩
```

除此以外，还有一些其他的参数调整合适也是可以提升吞吐量的，比较常用的有：

```
num.network.threads = 16 //Broker处理消息的最大线程数，可以设置成CPU数的2倍
num.io.threads = 16 // Broker处理磁盘IO的线程数，可以设置成CPU数的2倍

socket.send.buffer.bytes=65536 //发送缓冲区的大小
socket.receive.buffer.bytes=65536 //接收缓冲区的大小
```

> num.network.threads 指定 Kafka Broker 用于处理网络请求的线程数，它主要负责处理来自客户端的请求、响应请求以及数据传输等。
> num.io.threads 指定 Kafka Broker 用于处理磁盘 I/O 的线程数，它主要负责读写磁盘上的数据。
> socket.send.buffer.bytes 控制发送缓冲区的大小，即客户端发送消息到 Kafka broker 时的缓冲区大小。
> socket.receive.buffer.bytes 控制接收缓冲区的大小，即客户端从 Kafka broker 接收消息时的缓冲区大小。


一般来说，参数的合理设置需要考虑硬件配置、Kafka Broker 的负载情况以及网络吞吐量等多个因素。
