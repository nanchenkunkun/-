# 典型回答

在Redis中遍历所有的key，有两种办法，分别使用KEYS命令和SCAN命令。

KEYS命令用于查找所有符合给定模式的键，例如KEYS *会返回所有键。它在小数据库中使用时非常快，但在包含大量键的数据库中使用可能会阻塞服务器，因为它一次性检索并返回所有匹配的键。

如使用Jedis的是实现方式如下：

```java
import redis.clients.jedis.Jedis;

public class RedisKeysExample {
    public static void main(String[] args) {
        Jedis jedis = new Jedis("localhost");
        Set<String> keys = jedis.keys("*"); // 使用KEYS命令获取所有键
        for(String key : keys) {
            System.out.println(key);
        }
        jedis.close();
    }
}
```

**SCAN命令提供了一种更安全的遍历键的方式，它以游标为基础分批次迭代键集合，每次调用返回一部分匹配的键。**SCAN命令不会一次性加载所有匹配的键，因此不会像KEYS命令那样阻塞服务器，更适合用于生产环境中遍历键集合。

如使用Jedis的是实现方式如下：

```java
import redis.clients.jedis.Jedis;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

public class RedisScanExample {
    public static void main(String[] args) {
        Jedis jedis = new Jedis("localhost");
        String cursor = ScanParams.SCAN_POINTER_START;
        ScanParams scanParams = new ScanParams().count(10);
        do {
            ScanResult<String> scanResult = jedis.scan(cursor, scanParams);
            cursor = scanResult.getCursor();
            scanResult.getResult().forEach(System.out::println);
        } while (!cursor.equals("0"));
        jedis.close();
    }
}

```

当遍历结束时，cursor 的值会变为 0，我们可以通过判断 cursor 的值来终止迭代。
