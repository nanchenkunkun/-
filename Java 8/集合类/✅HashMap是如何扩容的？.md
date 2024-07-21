# 典型回答

为什么需要扩容？假设现在散列表中的元素已经很多了，但是现在散列表的链化已经比较严重了，哪怕是树化了，时间复杂度也没有O(1)好，所以需要扩容来降低Hash冲突的概率，以此来提高性能。

我们知道，当`++size > threshold `之后(详见java.util.HashMap#putVal方法)，HashMap就会初始化新的新的桶数组，该桶数组的size为原来的两倍，在扩大桶数组的过程中，会涉及三个部分：

1. 如果某桶节点没有形成链表，则直接rehash到其他桶中
2. 如果桶中形成链表，则将链表重新链接
3. 如果桶中的链表已经形成红黑树，但是链表中的元素个数小于6，则进行取消树化的操作
## 桶元素重新映射
如果桶中只有一个元素，没有形成链表，则将原来的桶引用置为null，同时，将该元素进行rehash即可，如下代码所示：
```java
if (e.next == null) {
    newTab[e.hash & (newCap - 1)] = e;
}
```
## 链表重新链接
假设有4个key，分别为a，b，c，d，且假定他们的hash值如下：
```java
hash(a) = 3; hash(a) & 7 = 3;   hash(a) & 8 = 0; 
hash(b) = 11; hash(b) & 7 = 3;  hash(b) & 8 = 8; 
hash(c) = 27; hash(c) & 7 = 3;  hash(c) & 8 = 8;  
hash(d) = 59; hash(d) & 7 = 3;  hash(d) & 8 = 8; 
```
假如此时HashMap的cap为8，某个桶中已经形成链表，则可得到：table[3]=a->b->c->d<br />如果此时扩容，将newCap设为16，我们可以看到如下结果：
```java
hash(a) = 3; hash(a) & 15 = 3; 
hash(b) = 11; hash(b) & 15 = 11
hash(c) = 27; hash(c) & 15 = 11
hash(d) = 59; hash(d) & 15 = 11
```

**我们会发现，当hash(k) & oldCap = 0（即hash(a) = 3;的这个记录）时，这些链表的节点还是在原来的节点中（扩容后他的结果还是3），同时如果hash(k) & oldCap != 0时（11 27 59这几条记录），这些链表的节点会到桶中的其他的位置中（从3变成了11）。**

所以，对于链表来说，我们就不用逐个节点重新映射，而是直接通过hash(k) & oldCap进行分类，之后统一移动他们的位置即可。源码如下：
```java
Node<K,V> loHead = null, loTail = null;
Node<K,V> hiHead = null, hiTail = null;
Node<K,V> next;
do {
    next = e.next;
    if ((e.hash & oldCap) == 0) {
        if (loTail == null)
            loHead = e;
        else
            loTail.next = e;
        loTail = e;
    }
    else {
        if (hiTail == null)
            hiHead = e;
        else
            hiTail.next = e;
        hiTail = e;
    }
} while ((e = next) != null);
if (loTail != null) {
    loTail.next = null;
    newTab[j] = loHead;
}
if (hiTail != null) {
    hiTail.next = null;
    newTab[j + oldCap] = hiHead;
}
```
## 取消树化
有了上面链表重新连接的经验，我们会发现，其实树化后的节点，也可以使用该操作来降低红黑树每个节点rehash时的时间复杂度，所以红黑树的TreeNode继承了链表的Node类，有了next字段，这样就可以像链表一样重新链接，源码如下：
```java
TreeNode<K,V> loHead = null, loTail = null;
TreeNode<K,V> hiHead = null, hiTail = null;
for (TreeNode<K,V> e = b, next; e != null; e = next) {
    next = (TreeNode<K,V>)e.next;
    e.next = null;
    if ((e.hash & bit) == 0) {
        if ((e.prev = loTail) == null)
            loHead = e;
        else
            loTail.next = e;
        loTail = e;
        ++lc;
    }
    else {
        if ((e.prev = hiTail) == null)
            hiHead = e;
        else
            hiTail.next = e;
        hiTail = e;
        ++hc;
    }
}
```
当上面的操作完结后，HashMap会检测两个链表的长度，当元素小于等于6的时候，就会执行取消树化的操作，否则就会将新生成的链表重新树化。<br />取消树化非常简单，因为之前已经是条链表了，所以只需要将里面的元素由TreeNode转为Node即可
> 至于重新树化的过程，请听下回分解～

# 知识扩展
## 除了rehash之外，哪些操作也会将树会退化成链表？
在remove元素的时候，这个过程中也会做退化的判断，如以下代码中，也会在这个分支中执行退化的操作（untreeify），如下代码所示：
```java
if (root == null || (movable && (root.right == null || (rl = root.left) == null|| rl.left == null))) {
  	tab[index] = first.untreeify(map);  // too small
    return;
}
```

