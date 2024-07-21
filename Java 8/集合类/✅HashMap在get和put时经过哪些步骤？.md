# 典型回答
对于HashMap来说，底层是基于散列算法实现，散列算法分为散列再探测和拉链式。HashMap 则使用了拉链式的散列算法，即采用数组+链表/红黑树来解决hash冲突，数组是HashMap的主体，链表主要用来解决哈希冲突。这个数组是Entry类型，它是HashMap的内部类，每一个Entry包含一个key-value键值对
## get方法
下面是JDK 1.8中HashMap的get方法的简要实现过程：

1. 首先，需要计算键的哈希值，并通过哈希值计算出在数组中的索引位置。
2. 如果该位置上的元素为空，说明没有找到对应的键值对，直接返回null。
3. 如果该位置上的元素不为空，遍历该位置上的元素，如果找到了与当前键相等的键值对，那么返回该键值对的值，否则返回null。

```
public V get(Object key) {
    Node<K, V> e;
    return (e = getNode(hash(key), key)) == null ? null : e.value;
}
```

get 方法看起来很简单，就是通过同样的 hash 得到 key 的hash 值。重点看下 getNode方法：

```
final Node<K, V> getNode(int hash, Object key) {
        //当前HashMap的散列表的引用
        Node<K, V>[] tab;
        //first：桶头元素
        //e：用于存放临时元素
        Node<K, V> first, e;
        //n：table 数组的长度
        int n;
        //元素中的 k
        K k;
        // 将 table 赋值为 tab，不等于null 说明有数据，(n = tab.length) > 0 同理说明 table 中有数据
        //同时将 该位置的元素 赋值为 first
        if ((tab = table) != null && (n = tab.length) > 0 && (first = tab[(n - 1) & hash]) != null) {
            //定位到了桶的到的位置的元素就是想要获取的 key 对应的，直接返回该元素
            if (first.hash == hash && ((k = first.key) == key || (key != null && key.equals(k)))) {
                return first;
            }
            //到这一步说明定位到的元素不是想要的，且该位置不仅仅有一个元素，需要判断是链表还是树
            if ((e = first.next) != null) {
                //是否已经树化
                if (first instanceof TreeNode) {
                    return ((TreeNode<K, V>) first).getTreeNode(hash, key);
                }
                //处理链表的情况
                do {
                    //如果遍历到了就直接返回该元素
                    if (e.hash == hash && ((k = e.key) == key || (key != null && key.equals(k)))) {
                        return e;
                    }
                } while ((e = e.next) != null);
            }
        }
        //遍历不到返回null
        return null;
    }

```
  
## put方法
下面是JDK 1.8中HashMap的put方法的简要实现过程：

1. 首先，put方法会计算键的哈希值(通过调用hash方法)，并通过哈希值计算出在数组中的索引位置。
2. 如果该位置上的元素为空，那么直接将键值对存储在该位置上。
3. 如果该位置上的元素不为空，那么遍历该位置上的元素，如果找到了与当前键相等的键值对，那么将该键值对的值更新为当前值，并返回旧值。
4. 如果该位置上的元素不为空，但没有与当前键相等的键值对，那么将键值对插入到链表或红黑树中（如果该位置上的元素数量超过了一个阈值，就会将链表转化为红黑树来提高效率）。
5. 如果插入成功，返回被替换的值；如果插入失败，返回null。
6. 插入成功后，如果需要扩容，那么就进行一次扩容操作。

put方法的代码很简单，就一行代码：
```
public V put(K key, V value) {
return putVal(hash(key), key, value, false, true);
}
```

核心其实是通过 `putValue`方法实现的，在传给`putValue`的参数中，先调用`hash`获取了一下hashCode。

[✅HashMap的hash方法是如何实现的？](https://www.yuque.com/hollis666/fo22bm/sz24zwwrdg92qizg?view=doc_embed)

**putVal 方法主要实现如下，给大家增加了注释：**

```
 /**
     * Implements Map.put and related methods.
     *
     * @param hash         key 的 hash 值
     * @param key          key 值
     * @param value        value 值
     * @param onlyIfAbsent true：如果某个 key 已经存在那么就不插了；false 存在则替换，没有则新增。这里为 false
     * @param evict        不用管了，我也不认识
     * @return previous value, or null if none
     */
    final V putVal(int hash, K key, V value, boolean onlyIfAbsent,
                   boolean evict) {
        // tab 表示当前 hash 散列表的引用
        Node<K, V>[] tab;
        // 表示具体的散列表中的元素
        Node<K, V> p;
        // n：表示散列表数组的长度
        // i：表示路由寻址的结果
        int n, i;
        // 将 table 赋值发给 tab ；如果 tab == null，说明 table 还没有被初始化。则此时是需要去创建 table 的
        // 为什么这个时候才去创建散列表？因为可能创建了 HashMap 时候可能并没有存放数据，如果在初始化 HashMap 的时候就创建散列表，势必会造成空间的浪费
        // 这里也就是延迟初始化的逻辑
        if ((tab = table) == null || (n = tab.length) == 0) {
            n = (tab = resize()).length;
        }
        // 如果 p == null，说明寻址到的桶的位置没有元素。那么就将 key-value 封装到 Node 中，并放到寻址到的下标为 i 的位置
        if ((p = tab[i = (n - 1) & hash]) == null) {
            tab[i] = newNode(hash, key, value, null);
        }
        // 到这里说明 该位置已经有数据了，且此时可能是链表结构，也可能是树结构
        else {
            // e 表示找到了一个与当前要插入的key value 一致的元素
            Node<K, V> e;
            // 临时的 key
            K k;
            // p 的值就是上一步 if 中的结果即：此时的 (p = tab[i = (n - 1) & hash]) 不等于 null
            // p 是原来的已经在 i 位置的元素，且新插入的 key 是等于 p中的key
            //说明找到了和当前需要插入的元素相同的元素（其实就是需要替换而已）
            if (p.hash == hash && ((k = p.key) == key || (key != null && key.equals(k))))
                //将 p 的值赋值给 e
                e = p;
                //说明已经树化，红黑树会有单独的文章介绍，本文不再赘述
            else if (p instanceof TreeNode) {
                e = ((TreeNode<K, V>) p).putTreeVal(this, tab, hash, key, value);
            } else {
                //到这里说明不是树结构，也不相等，那说明不是同一个元素，那就是链表了
                for (int binCount = 0; ; ++binCount) {
                    //如果 p.next == null 说明 p 是最后一个元素，说明，该元素在链表中也没有重复的，那么就需要添加到链表的尾部
                    if ((e = p.next) == null) {
                        //直接将 key-value 封装到 Node 中并且添加到 p的后面
                        p.next = newNode(hash, key, value, null);
                        // 当元素已经是 7了，再来一个就是 8 个了，那么就需要进行树化
                        if (binCount >= TREEIFY_THRESHOLD - 1) {
                            treeifyBin(tab, hash);
                        }
                        break;
                    }
                    //在链表中找到了某个和当前元素一样的元素，即需要做替换操作了。
                    if (e.hash == hash && ((k = e.key) == key || (key != null && key.equals(k)))) {
                        break;
                    }
                    //将e(即p.next)赋值为e，这就是为了继续遍历链表的下一个元素（没啥好说的）下面有张图帮助大家理解。
                    p = e;
                }
            }
            //如果条件成立，说明找到了需要替换的数据，
            if (e != null) {
                //这里不就是使用新的值赋值为旧的值嘛
                V oldValue = e.value;
                if (!onlyIfAbsent || oldValue == null) {
                    e.value = value;
                }
                //这个方法没用，里面啥也没有
                afterNodeAccess(e);
                //HashMap put 方法的返回值是原来位置的元素值
                return oldValue;
            }
        }
        // 上面说过，对于散列表的 结构修改次数，那么就修改 modCount 的次数
        ++modCount;
        //size 即散列表中的元素的个数，添加后需要自增，如果自增后的值大于扩容的阈值，那么就触发扩容操作
        if (++size > threshold) {
            resize();
        }
        //啥也没干
        afterNodeInsertion(evict);
        //原来位置没有值，那么就返回 null 呗
        return null;
    }
```

# 知识扩展
## HashMap如何定位key
先通过 `(table.length - 1) & (key.hashCode ^ (key.hashCode >>> 16))`定位到key位于哪个table中，然后再通过`key.equals(rowKey)`来判断两个key是否相同，综上，是先通过hashCode和equals来定位KEY的。<br />源码如下：
```java
static final int hash(Object key) {
    int h;
    return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
}
final V putVal(int hash, K key, V value, boolean onlyIfAbsent, boolean evict) {
    // ...省略
    if ((p = tab[i = (n - 1) & hash]) == null)
            tab[i] = newNode(hash, key, value, null);
    else {
        Node<K,V> e; K k;
        // 这里会通过equals判断
        if (p.hash == hash && ((k = p.key) == key || (key != null && key.equals(k))))
            e = p;
        else if (p instanceof TreeNode)
            e = ((TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value);
    // ...省略
    return null;
}
```
所以，在使用HashMap的时候，尽量用String和Enum等已经实现过hashCode和equals方法的官方库类，如果一定要自己的类，就一定要实现hashCode和equals方法
### HashMap定位tableIndex的骚操作
通过源码发现，hashMap定位tableIndex的时候，是通过`(table.length - 1) & (key.hashCode ^ (key.hashCode >> 16))`，而不是常规的`key.hashCode % (table.length)`呢？

1. 为什么是用&而不是用%：因为&是基于内存的二进制直接运算，比转成十进制的取模快的多。以下运算等价：`X % 2^n = X & (2^n – 1)`。这也是hashMap每次扩容都要到2^n的原因之一
2. 为什么用key.hash ^ (key.hash >> 16)而不是用key.hash：这是因为增加了扰动计算，使得hash分布的尽可能均匀。因为hashCode是int类型，虽然能映射40亿左右的空间，但是，HashMap的table.length毕竟不可能有那么大，所以为了使hash%table.length之后，分布的尽可能均匀，就需要对实例的hashCode的值进行扰动，说白了，就是将hashCode的高16和低16位，进行异或，使得hashCode的值更加分散一点。
## HashMap的key为null时，没有hashCode是如何存储的？
HashMap对key=null的case做了特殊的处理，key值为null的kv对，总是会放在数组的第一个元素中，如下源码所示：
```java
private V putForNullKey(V value) {
    for (Entry<K,V> e = table[0]; e != null; e = e.next) {
        if (e.key == null) {
            V oldValue = e.value;
            e.value = value;
            e.recordAccess(this);
            return oldValue;
        }
    }
    modCount++;
    addEntry(0, null, value, 0);
    return null;
}


private V getForNullKey() {
    for (Entry<K,V> e = table[0]; e != null; e = e.next) {
        if (e.key == null)
            return e.value;
    }
    return null;
}
```
## HashMap的value可以为null吗？有什么优缺点？
HashMap的key和value都可以为null，优点很明显，不会因为调用者的粗心操作就抛出NPE这种RuntimeException，但是缺点也很隐蔽，就像下面的代码一样：
```java
// 调用远程RPC方法，获取map
Map<String, Object> map = remoteMethod.queryMap();
// 如果包含对应key，则进行业务处理
if(map.contains(KEY)) {
    String value = (String)map.get(KEY);
    System.out.println(value);
}
```
虽然`map.contains(key)`，但是`map.get(key)==null`，就会导致后面的业务逻辑出现NPE问题

