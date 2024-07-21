# 典型回答
List主要有ArrayList、LinkedList与Vector几种实现。这三者都实现了List 接口，使用方式也很相似,主要区别在于因为实现方式的不同,所以对不同的操作具有不同的效率。

**ArrayList 是一个可改变大小的数组**.当更多的元素加入到ArrayList中时,其大小将会动态地增长.内部的元素可以直接通过get与set方法进行访问,因为ArrayList本质上就是一个数组。

**LinkedList 是一个双向链表**，在添加和删除元素时具有比ArrayList更好的性能，但在get与set方面弱于ArrayList。当然,这些对比都是指数据量很大或者操作很频繁的情况下的对比,如果数据和运算量很小,那么对比将失去意义。

**Vector 和ArrayList类似,但属于强同步类。**如果你的程序本身是线程安全的(thread-safe,没有在多个线程之间共享同一个集合/对象),那么使用ArrayList是更好的选择。

Vector和ArrayList在更多元素添加进来时会请求更大的空间。Vector每次请求其大小的双倍空间，而ArrayList每次对size增长50%。

而 LinkedList 还实现了Queue和Deque接口,该接口比List提供了更多的方法,包括offer(),peek(),poll()等。

**注意: 默认情况下ArrayList的初始容量非常小,所以如果可以预估数据量的话,分配一个较大的初始值属于最佳实践,这样可以减少调整大小的开销。**
# 知识扩展
## ArrayList是如何扩容的？
首先，我们要明白ArrayList是基于数组的，我们都知道，申请数组的时候，只能申请一个定长的数组，那么List是如何通过数组扩容的呢？ArrayList的扩容分为以下几步：

1. 检查新增元素后是否会超过数组的容量，如果超过，则进行下一步扩容
2. 设置新的容量为老容量的1.5倍，最多不超过2^31-1 （Java 8中ArrayList的容量最大是Integer.MAX_VALUE - 8，即2^31-9。这是由于在Java 8中，ArrayList内部实现进行了一些改进，使用了一些数组复制的技巧来提高性能和内存利用率，而这些技巧需要额外的8个元素的空间来进行优化。）
3. 之后，申请一个容量为1.5倍的数组，并将老数组的元素复制到新数组中，扩容完成
## 如何利用List实现LRU？
LRU，即最近最少使用策略，基于时空局部性原理（最近访问的，未来也会被访问），往往作为缓存淘汰的策略，如Redis和GuavaMap都使用了这种淘汰策略。<br />我们可以基于LinkedList来实现LRU，因为LinkedList基于双向链表，每个结点都会记录上一个和下一个的节点，具体实现方式如下：
```java
public class LruListCache<E> {

    private final int maxSize;

    private final LinkedList<E> list = new LinkedList<>();

    public LruListCache(int maxSize) {
        this.maxSize = maxSize;
    }

    public void add(E e) {
        if (list.size() < maxSize) {
            list.addFirst(e);
        } else {
            list.removeLast();
            list.addFirst(e);
        }
    }

    public E get(int index) {
        E e = list.get(index);
        list.remove(e);
        add(e);
        return e;
    }

    @Override
    public String toString() {
        return list.toString();
    }
}

```
## 数组和链表的区别
[🔜数组和链表有何区别？](https://www.yuque.com/hollis666/fo22bm/feley4pfqbz6pkr0)
