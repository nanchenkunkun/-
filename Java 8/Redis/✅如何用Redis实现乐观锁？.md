# 典型回答

所谓乐观锁，其实就是基于CAS的机制，CAS的本质是Compare And Swap，就是需要知道一个key在修改前的值，去进行比较。

在Redis中，想要实现这个功能，我们可以依赖 WATCH 命令。这个命令一旦运行，他会确保只有在 WATCH 监视的键在调用 EXEC 之前没有改变时，后续的事务才会执行。

例如，如果没有 INCRBY，我们可以用下面的方式实现原子的增量操作：

```java
WATCH counter
GET counter
MULTI
SET counter <从 GET 获得的值 + 任何增量>
EXEC
```

1. **WATCH**：使用 WATCH 命令监视一个或多个键。这个命令会监视给定键直到事务开始（即执行 MULTI 命令）。
2. **GET**：在事务开始之前，查询你需要的数据。
3. **MULTI**：使用 MULTI 命令开始事务。
4. **SET**：在事务中添加所有需要执行的命令。
5. **EXEC**：使用 EXEC 命令执行事务。如果自从事务开始以来监视的键被修改过，EXEC 将返回 nil，这表示事务中的命令没有被执行。

通过这种方式，Redis 保证了只有在监视的数据自事务开始以来没有改变的情况下，事务才会执行，从而实现了乐观锁定。

以下，是在Java中，用Jedis实现的代码，和上述流程是一样的：

```java
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

public class RedisOptimisticLock {
    public static void main(String[] args) {
        // 连接到 Redis
        Jedis jedis = new Jedis("localhost");

        try {
            // 监视键
            String key = "myKey";
            jedis.watch(key);

            // 模拟从数据库读取最新值
            String value = jedis.get(key);
            int intValue = Integer.parseInt(value);

            // 开始事务
            Transaction t = jedis.multi();

            // 在事务中执行操作
            t.set(key, String.valueOf(intValue + 1));

            // 尝试执行事务
            if (t.exec() == null) {
                System.out.println("事务执行失败，数据已被其他客户端修改");
            } else {
                System.out.println("事务执行成功");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            jedis.close();
        }
    }
}

```


