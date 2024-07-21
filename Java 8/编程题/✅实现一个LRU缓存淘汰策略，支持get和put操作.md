# 典型回答

LRU算法的思想是：如果一个数据在最近一段时间没有被访问到，那么可以认为在将来它被访问的可能性也很小。因此，当空间满时，最久没有访问的数据最先被淘汰。

一般实现有两种方式，首先是**通过继承LinkedHashMap可以实现这个功能**。

LinkedHashMap内部维护了一个双向链表，用于存储元素的顺序信息。当accessOrder参数为true时，LinkedHashMap会按照访问顺序来维护元素，即最近访问的元素会被移到链表尾部，而最久未使用的元素会被移到链表头部。当accessOrder参数为false时，LinkedHashMap会按照插入顺序来维护元素。


LinkedHashMap和HashMap一样提供了put、get等方法，实现细节稍有不同（以下特点为当accessOrder为true时）：

- put方法：
   - 如果指定的键已经存在，则更新对应的值，并将该元素移动到链表末尾
   - 如果指定的键不存在，则将新元素插入到哈希表中，并将其插入到链表末尾
- get方法：
   - 如果指定的键不存在，则返回null；
   - 如果指定的键存在，则返回对应的值，并将该元素移动到链表末尾

但是，需要注意的是，LinkedHashMap默认情况下不会移除元素的，不过，LinkedHashMap中预留了方法afterNodeInsertion，在插入元素之后这个方法会被回调，这个方法的默认实现如下：

```
void afterNodeInsertion(boolean evict) { 
   LinkedHashMap.Entry<K,V> first;
   if (evict && (first = head) != null && removeEldestEntry(first)) {
       K key = first.key;
       removeNode(hash(key), key, null, false, true);
   }
}

protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
   return false;
}
```

可以看到，如果我们可以实现removeEldestEntry方法， 让他返回true的话，就可以执行删除节点的动作。所以，一个基于**LinkedHashMap的LRU实现如下：**

```
import java.util.*;

public class LRUCache<K, V> extends LinkedHashMap<K, V> {
    private final int capacity;

    public LRUCache(int capacity) {
        // 调用LinkedHashMap构造函数，设置初始容量和负载因子
        super(capacity, 0.75f, true);
        this.capacity = capacity;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        // 重写LinkedHashMap的removeEldestEntry方法，实现LRU缓存淘汰策略
        // 当缓存容量超出设定值时，自动移除最久未使用的元素
        return size() > capacity;
    }
}
```

以上，就是一个最简单的LRU 缓存的实现方式了。

除此之外，还有一些其他的方式也可以实现，比如**基于LinkedList+HashMap也可以简单的实现**：

```
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class LRUCache<K, V> {

    private final int capacity;            // 缓存容量
    private final Map<K, V> cache;         // 缓存
    private final LinkedList<K> keyList;   // 缓存key列表，用于记录key的访问顺序

    public LRUCache(int capacity) {
        this.capacity = capacity;
        this.cache = new HashMap<>(capacity);
        this.keyList = new LinkedList<>();
    }

    // put操作：向缓存中存入一个key-value
    public synchronized void put(K key, V value) {
        // 如果缓存中已经存在该key，则需要将其从缓存中移除，因为它将被更新
        if (cache.containsKey(key)) {
            cache.remove(key);
        }
        // 如果缓存已满，则需要删除最久未使用的key-value，即keyList的第一个元素
        while (cache.size() >= capacity) {
            K oldestKey = keyList.removeFirst();
            cache.remove(oldestKey);
        }
        // 将新的key-value存入缓存中，并将该key添加到keyList的末尾，表示最近被访问
        cache.put(key, value);
        keyList.addLast(key);
    }

    // get操作：根据key获取对应的value
    public synchronized V get(K key) {
        // 如果缓存中存在该key，则将其从keyList中移除，并添加到末尾表示最近被访问
        if (cache.containsKey(key)) {
            keyList.remove(key);
            keyList.addLast(key);
            return cache.get(key);
        }
        // 如果缓存中不存在该key，则返回null
        return null;
    }
}

```

借助LinkedList来保存key的访问情况，将新的key或者刚刚被访问的key放在末尾，这样在移除的时候，可以从队头开始移除元素。
