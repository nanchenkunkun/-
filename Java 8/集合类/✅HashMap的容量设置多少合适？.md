# 典型回答

HashMap有扩容机制，就是当达到扩容条件时会进行扩容。HashMap的扩容条件就是当HashMap中的元素个数（size）超过临界值（threshold）时就会自动扩容。在HashMap中，threshold = loadFactor * capacity。

[✅HashMap是如何扩容的？](https://www.yuque.com/hollis666/fo22bm/co1ul8?view=doc_embed)

所以，如果我们没有设置初始容量大小，随着元素的不断增加，HashMap会发生多次扩容，而HashMap中的扩容机制决定了每次扩容都需要重建hash表，是非常影响性能的。

所以，首先可以明确的是，我们建议开发者在创建HashMap的时候指定初始化容量。并且《阿里巴巴开发手册》中也是这么建议的：

![](https://cdn.nlark.com/yuque/0/2024/jpeg/5378072/1705732262005-310d6437-232e-47dd-9470-d9583d5da95f.jpeg#averageHue=%23e9e7e0&clientId=u46564c76-1729-4&from=paste&id=ue6772eb9&originHeight=86&originWidth=673&originalType=url&ratio=1&rotation=0&showTitle=false&status=done&style=none&taskId=u5aa9804a-3c5d-43f2-9ffa-f465d9a7a83&title=)

 那么，既然建议我们集合初始化的时候，要指定初始值大小，那么我们创建HashMap的时候，到底指定多少合适呢？

有些人会自然想到，我准备塞多少个元素我就设置成多少呗。比如我准备塞7个元素，那就new HashMap(7)。

**但是，这么做不仅不对，而且以上方式创建出来的Map的容量也不是7。**

因为，当我们使用HashMap(int initialCapacity)来初始化容量的时候，HashMap并不会使用我们传进来的initialCapacity直接作为初识容量。

**JDK会默认帮我们计算一个相对合理的值当做初始容量。所谓合理值，其实是找到第一个比用户传入的值大的2的幂。**

也就是说，当我们new HashMap(7)创建HashMap的时候，JDK会通过计算，帮我们创建一个容量为8的Map；当我们new HashMap(9)创建HashMap的时候，JDK会通过计算，帮我们创建一个容量为16的Map。

**但是，这个值看似合理，实际上并不尽然。因为HashMap在根据用户传入的capacity计算得到的默认容量，并没有考虑到loadFactor这个因素，只是简单机械的计算出第一个大于这个数字的2的幂。**

loadFactor是负载因子，当HashMap中的元素个数（size）超过 threshold = loadFactor * capacity时，就会进行扩容。

也就是说，如果我们设置的默认值是7，经过JDK处理之后，HashMap的容量会被设置成8，但是，这个HashMap在元素个数达到 8*0.75 = 6的时候就会进行一次扩容，这明显是我们不希望见到的。

那么，到底设置成什么值比较合理呢？

这里我们可以参考JDK8中putAll方法中的实现的，这个实现在guava（21.0版本）也被采用。并且在阿里巴巴Java开发手册中也有这样的规定

![image.png](https://cdn.nlark.com/yuque/0/2022/png/719664/1668845250399-62fa1924-5491-4e83-9643-c92179ff3c6f.png#averageHue=%23c2b3a3&clientId=uf896ad2c-a51f-4&from=paste&height=39&id=ua0839155&originHeight=78&originWidth=900&originalType=binary&ratio=1&rotation=0&showTitle=false&size=57727&status=done&style=none&taskId=u79b127f4-4884-49e0-b4f8-f1503fd0939&title=&width=450)

即：`**return (int) ((float) expectedSize / 0.75F + 1.0F);**`

比如我们计划向HashMap中放入7个元素的时候，我们通过expectedSize / 0.75F + 1.0F计算，7/0.75 + 1 = 10 ,10经过JDK处理之后，会被设置成16，这就大大的减少了扩容的几率。

> 当HashMap内部维护的哈希表的容量达到75%时（默认情况下），会触发rehash，而rehash的过程是比较耗费时间的。所以初始化容量要设置成expectedSize/0.75 + 1的话，可以有效的减少冲突也可以减小误差。（大家结合这个公式，好好理解下这句话）



**所以，我们可以认为，当我们明确知道HashMap中元素的个数的时候，把默认容量设置成expectedSize / 0.75F + 1.0F 是一个在性能上相对好的选择，但是，同时也会牺牲些内存。**

这个算法在guava中有实现，开发的时候，可以直接通过Maps类创建一个HashMap：

```java
Map<String, String> map = Maps.newHashMapWithExpectedSize(7);
```

其代码实现如下：

```java
public static <K, V> HashMap<K, V> newHashMapWithExpectedSize(int expectedSize) {
    return new HashMap(capacity(expectedSize));
}

static int capacity(int expectedSize) {
    if (expectedSize < 3) {
        CollectPreconditions.checkNonnegative(expectedSize, "expectedSize");
        return expectedSize + 1;
    } else {
        return expectedSize < 1073741824 ? (int)((float)expectedSize / 0.75F + 1.0F) : 2147483647;
    }
}
```

但是，以上的操作是一种用内存换性能的做法，真正使用的时候，要考虑到内存的影响。但是，大多数情况下，我们还是认为内存是一种比较富裕的资源。

但是话又说回来了，有些时候，我们到底要不要设置HashMap的初识值，这个值又设置成多少，真的有那么大影响吗？其实也不见得！

可是，大的性能优化，不就是一个一个的优化细节堆叠出来的吗？

再不济，以后你写代码的时候，使用Maps.newHashMapWithExpectedSize(7);的写法，也可以让同事和老板眼前一亮。

