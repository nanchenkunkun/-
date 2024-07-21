# 典型回答

[✅如何用SETNX实现分布式锁？](https://www.yuque.com/hollis666/fo22bm/feovxr7gr8ois5yt?view=doc_embed)

在使用SETNX实现的分布式锁中，因为存在锁无法续期导致并发冲突的问题，所以在真实的生产环境中用的并不是很多，其实，真正在使用Redis时，用的比较多的是基于Redisson实现分布式锁。

> Redisson是一个基于Redis的Java客户端，它提供了丰富的功能，包括分布式锁的支持。 [https://redisson.org/](https://redisson.org/)
> 关于Redisson实现分布式锁可以查看：[https://github.com/redisson/redisson/wiki/8.-Distributed-locks-and-synchronizers](https://github.com/redisson/redisson/wiki/8.-Distributed-locks-and-synchronizers)


为了避免锁超时，Redisson中引入了看门狗的机制，他可以帮助我们在Redisson实例被关闭前，不断的延长锁的有效期。

> 默认情况下，看门狗的检查锁的超时时间是30秒钟，也可以通过修改Config.lockWatchdogTimeout来另行指定。


### 可重入锁

基于Redisson可以非常简单的就获取一个可重入的分布式锁。基本步骤如下：

引入依赖：
```java
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson</artifactId>
    <version>最新版</version> 
</dependency>
```

定义一个Redisson客户端：
```java
 /**
 * @author Hollis
 */
@Configuration
public class RedissonConfig {
    
    @Bean(destroyMethod="shutdown")
    public RedissonClient redisson() throws IOException {
        Config config = new Config();
		config.useSingleServer().setAddress("redis://127.0.0.1:6379");
		RedissonClient redisson = Redisson.create(config);
        return redisson;
    }
}

```

接下来，在想要使用分布式锁的地方做如下调用即可：

```java
@Service
public class LockTestService{
    @Autowired
    RedissonClient redisson;
    
    public void testLock(){
        RLock lock = redisson.getLock("myLock");
        try {
            lock.lock();
            // 执行需要加锁的代码
        } finally {
            lock.unlock();
        }
    }
}

```

当然，也可以设置超时时间：

```java
// 设置锁的超时时间为30秒
lock.lock(30, TimeUnit.SECONDS);
try {
    // 执行需要保护的代码
} finally {
    lock.unlock();
}

```

以上方式，实现的是一个可重入的分布式锁，也就是说，获取到锁的线程可以再次尝试获取锁，并且这个锁也只能被这个线程解锁。

除了可重入锁以外，Redisson还支持公平锁（FairLock）以及联锁（MultiLock）的使用。

公平锁：
```java
RLock fairLock = redisson.getFairLock("anyLock");
fairLock.lock();
```

[✅公平锁和非公平锁的区别？](https://www.yuque.com/hollis666/fo22bm/bnt978?view=doc_embed)

联锁：
```java
RLock lock1 = redissonInstance1.getLock("lock1");
RLock lock2 = redissonInstance2.getLock("lock2");
RLock lock3 = redissonInstance3.getLock("lock3");

RedissonMultiLock lock = new RedissonMultiLock(lock1, lock2, lock3);
// 同时加锁：lock1 lock2 lock3
// 所有的锁都上锁成功才算成功。
lock.lock();
...
lock.unlock();
```

### RedLock

[✅什么是RedLock，他解决了什么问题？](https://www.yuque.com/hollis666/fo22bm/lxzg0ubs2xpvenxw?view=doc_embed)

为了解决Redis单点问题，引入了红锁的算法，Redisson中支持红锁的使用。示例在上面的文档中已经有了，这里就不再重复介绍了。

### 读写锁

Redisson中支持分布式可重入读写锁，这种锁允许同时有多个读锁和一个写锁对同一个资源进行加锁。

```java
RReadWriteLock rwlock = redisson.getReadWriteLock("anyRWLock");
// 最常见的使用方法
rwlock.readLock().lock();
// 或
rwlock.writeLock().lock();
```

