# 典型回答

可重入锁是一种多线程同步机制，允许**同一线程**多次获取同一个锁而不会导致死锁。

[✅什么是可重入锁，怎么实现可重入锁？](https://www.yuque.com/hollis666/fo22bm/zvx2w5h9sr7trle7?view=doc_embed)

在Redis中，最简单的方式就是使用setnx来实现一个分布式锁了，但是如果我想要实现一个具有重入功能的锁，那么用setnx如何实现呢？

首先，我们需要有一个标识来识别出一个线程，这里可以是线程ID，分布式的traceId（[https://www.yuque.com/hollis666/fo22bm/nnl88aqknhx2v76c](https://www.yuque.com/hollis666/fo22bm/nnl88aqknhx2v76c) ），或者是一个唯一的业务ID都可以。

有了这个唯一标识之后，我们加锁的时候，就可以用这个标识来判断当前持有锁的线程是不是自己，如果是的话，就可以直接重入。否则就无法重入。

为了保证重入几次之后，需要同时解锁几次，那么我们也需要维护一个重入次数的字段。因为每一次重入其实就是一个加锁动作，避免出现加锁2次，但是1次解锁动作就把锁给解了的情况。

有了以上基础之后，加锁和解锁的逻辑如下：

**加锁的逻辑**：

   - 当线程尝试获取锁时，它首先检查锁是否已经存在。
   - 如果锁不存在（即 SETNX 返回成功），线程设置锁，存储自己的标识符和计数器（初始化为1）。
   - 如果锁已存在，线程检查锁中的标识符是否与自己的相同。
      - 如果是，线程已经持有锁，只需增加计数器的值。
      - 如果不是，获取锁失败，因为锁已被其他线程持有。

**解锁的逻辑**：

   - 当线程释放锁时，它会减少计数器的值。
   - 如果计数器降至0，这意味着线程已完成对锁的所有获取请求，可以完全释放锁。
   - 如果计数器大于0，锁仍被视为被该线程持有。

代码实现如下：

```java
import redis.clients.jedis.Jedis;

public class ReentrantRedisLock {

    public synchronized boolean tryLock(Jedis jedis,String lockKey) {
        String currentThreadId = String.valueOf(Thread.currentThread().getId());

        // 尝试获取锁
        String lockValue = jedis.get(lockKey);
        if (lockValue == null) {
            // 锁不存在，尝试设置锁
            jedis.set(lockKey, currentThreadId + ":1", "NX", "EX", 30);
            return true;
        }

        // 锁存在，检查是否由当前线程持有
        String[] parts = lockValue.split(":");

        //加锁线程是当前线程，则增加次数，进行重入加锁
        if (parts.length == 2 && parts[0].equals(currentThreadId)) {
            int count = Integer.parseInt(parts[1]) + 1;
            jedis.set(lockKey, currentThreadId + ":" + count, "XX", "EX", 30);
            return true;
        }

        //加锁失败
        return false;
    }

    public synchronized void unlock(Jedis jedis,String lockKey) {
        String currentThreadId = String.valueOf(Thread.currentThread().getId());

        String lockValue = jedis.get(lockKey);
        if (lockValue != null) {
            String[] parts = lockValue.split(":");
            if (parts.length == 2 && parts[0].equals(currentThreadId)) {
                int count = Integer.parseInt(parts[1]);
                //减少重入次数
                if (count > 1) {
                    jedis.set(lockKey, currentThreadId + ":" + (count - 1), "XX", "EX", 30);
                } else {
                    //解锁
                    jedis.del(lockKey);
                }
            }
        }
    }
}

```

在这个实现中，锁的值是一个由线程 ID 和锁的获取次数组成的字符串，格式为 `线程ID:次数`。当一个线程尝试获取锁时，它会检查当前的锁值。

如果锁由相同的线程持有，则增加计数器；否则，尝试设置新的锁。释放锁时，它会递减计数器，当计数器为零时，锁被完全释放。

# 扩展知识

## lua优化

以上方式用synchronized来解决的并发，其实这里性能并不好，可以直接借助lua脚本的原子性来实现这个可重入的功能。

[✅为什么Lua脚本可以保证原子性？](https://www.yuque.com/hollis666/fo22bm/rwdgnu?view=doc_embed)

以下是lua脚本部分的代码实现：

```java
-- tryLock.lua
-- 尝试获取锁的Lua脚本
-- KEYS[1] 是锁的key
-- ARGV[1] 是当前线程的标识
-- ARGV[2] 是锁的超时时间
local lockValue = redis.call('get', KEYS[1])
if lockValue == false then
    -- 锁不存在，创建锁并设置超时
    redis.call('setex', KEYS[1], ARGV[2], ARGV[1] .. ':1')
    return true
else
    local parts = {}
    local index = 0
    for match in (lockValue .. ":"):gmatch("(.-)" .. ":") do
        parts[index] = match
        index = index + 1
    end
    if parts[0] == ARGV[1] then
        -- 锁已经被当前线程持有，重入次数加1
        local count = tonumber(parts[1]) + 1
        redis.call('setex', KEYS[1], ARGV[2], ARGV[1] .. ':' .. count)
        return true
    end
end
return false

```

> `..` 是Lua中的字符串连接操作符，用于连接两个字符串。
> `gmatch` 是Lua的一个字符串操作函数，用于在给定字符串中全局匹配指定的模式，并返回一个迭代器，每次调用这个迭代器都会返回下一个匹配的字符串。
> 模式 `"(.-):"` 是一个模式表达式,功能是匹配任意数量的字符直到遇到第一个 ":"


```java
-- unlock.lua
-- 释放锁的Lua脚本
-- KEYS[1] 是锁的key
-- ARGV[1] 是当前线程的标识
local lockValue = redis.call('get', KEYS[1])
if lockValue ~= false then
    local parts = {}
    local index = 0
    for match in (lockValue .. ":"):gmatch("(.-)" .. ":") do
        parts[index] = match
        index = index + 1
    end
    if parts[0] == ARGV[1] then
        local count = tonumber(parts[1])
        if count > 1 then
            -- 减少重入次数
            count = count - 1
            redis.call('set', KEYS[1], ARGV[1] .. ':' .. count)
        else
            -- 重入次数为0，删除锁
            redis.call('del', KEYS[1])
        end
        return true
    end
end
return false

```

有了以上脚本之后，使用jedis就可以直接调用lua脚本了：

```java
// 尝试获取锁
String tryLockScript = "..."; // Lua脚本字符串
Object result = jedis.eval(tryLockScript, Collections.singletonList(lockKey), Arrays.asList(currentThreadId, "30"));

// 释放锁
String unlockScript = "..."; // Lua脚本字符串
jedis.eval(unlockScript, Collections.singletonList(lockKey), Collections.singletonList(currentThreadId));
```
