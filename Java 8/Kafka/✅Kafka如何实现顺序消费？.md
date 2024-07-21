# 典型回答

Kafka的消息是存储在指定的topic中的某个partition中的。并且一个topic是可以有多个partition的。同一个partition中的消息是有序的，但是跨partition，或者跨topic的消息就是无序的了。

**为什么同一个partition的消息是有序的？**

因为当生产者向某个partition发送消息时，消息会被追加到该partition的日志文件（log）中，并且被分配一个唯一的 offset，文件的读写是有顺序的。而消费者在从该分区消费消息时，会从该分区的最早 offset 开始逐个读取消息，保证了消息的顺序性。

**基于此，想要实现消息的顺序消费，可以有以下几个办法：**

1、在一个topic中，只创建一个partition，这样这个topic下的消息都会按照顺序保存在同一个partition中，这就保证了消息的顺序消费。

2、发送消息的时候指定partition，如果一个topic下有多个partition，那么我们可以把需要保证顺序的消息都发送到同一个partition中，这样也能做到顺序消费。

# 扩展知识

## 如何发到同一个partition

当我们发送消息的时候，如果key为null，那么Kafka 默认采用 Round-robin 策略，也就是轮转，实现类是 DefaultPartitioner。那么如果想要指定他发送到某个partition的话，有以下三个方式：

### 指定partition

我们可以在发送消息的时候，可以直接在ProducerRecord中指定partition

```java
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;

public class KafkaProducerExample {
    public static void main(String[] args) {
        
        // 创建Kafka生产者
        Producer<String, String> producer = new KafkaProducer<>(getProperties());

        String topic = "hollis_topic"; // 指定要发送消息的主题
        String message = "Hello World!"; // 要发送的消息内容
        int partition = 0; // 要发送消息的分区

        // 创建包含分区信息的ProducerRecord
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, partition, null, message);

        // 发送消息
        producer.send(record);

        // 关闭Kafka生产者
        producer.close();
    }
}

```
### 指定key

在没有指定 Partition(null 值) 时, 如果有 Key, Kafka 将依据 Key 做hash来计算出一个 Partition 编号来。如果key相同，那么也能分到同一个partition中：

```java
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;

public class KafkaProducerExample {
    public static void main(String[] args) {
        
        // 创建Kafka生产者
        Producer<String, String> producer = new KafkaProducer<>(getProperties());

        String topic = "hollis_topic"; // 指定要发送消息的主题
        String message = "Hello World!"; // 要发送的消息内容
        String key = "Hollis_key"; // 要发送消息的key

       // 创建ProducerRecord，指定主题、键和消息内容
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, null, key, message);

        // 发送消息
        producer.send(record);

        // 关闭Kafka生产者
        producer.close();
    }
}

```

### 自定义Partitioner

除了以上两种方式，我们还可以实现自己的分区器（Partitioner）来指定消息发送到特定的分区。

我们需要创建一个类实现Partitioner接口，并且重写partition()方法。

```java
import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.record.InvalidRecordException;
import org.apache.kafka.common.utils.Utils;

import java.util.List;
import java.util.Map;

public class CustomPartitioner implements Partitioner {

    @Override
    public void configure(Map<String, ?> configs) {
        // 可以在这里处理和获取分区器的配置参数
    }

    @Override
    public int partition(String topic, Object key, byte[] keyBytes, Object value, byte[] valueBytes, Cluster cluster) {
        List<PartitionInfo> partitions = cluster.partitionsForTopic(topic);
        int numPartitions = partitions.size();

        if (keyBytes == null || !(key instanceof String)) {
            throw new InvalidRecordException("键不能为空且必须是字符串类型");
        }

        // 根据自定义的逻辑，确定消息应该发送到哪个分区
        String keyValue = (String) key;
        int partition = Math.abs(keyValue.hashCode()) % numPartitions;

        // 返回分区编号
        return partition;
    }

    @Override
    public void close() {
        // 可以在这里进行一些清理操作
    }
}

```

在partition()方法中，我们使用了一个简单的逻辑，根据键的哈希值将消息发送到相应的分区。为了在Kafka生产者中使用自定义的分区器，你需要在生产者的配置中指定分区器类：

```java
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;

public class KafkaProducerExample {
    public static void main(String[] args) {
        // 设置Kafka生产者的配置属性
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("partitioner.class", "com.hollis.CustomPartitioner"); // 指定自定义分区器类

        // 创建Kafka生产者
        Producer<String, String> producer = new org.apache.kafka.clients.producer.KafkaProducer<>(props);

        String topic = "hollis_topic"; // 指定要发送消息的主题
        String message = "Hello World!"; // 要发送的消息内容
        String key = "Hollis_key"; // 要发送消息的key

        // 创建ProducerRecord，指定主题、键和消息内容
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, message);

        // 发送消息
        producer.send(record);

        // 关闭Kafka生产者
        producer.close();
    }
}

```
