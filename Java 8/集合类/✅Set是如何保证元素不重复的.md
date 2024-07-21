# 典型回答
在Java的Set体系中，根据实现方式不同主要分为两大类。HashSet和TreeSet。

1. TreeSet 是二叉树实现的，TreeSet中的数据是自动排好序的，不允许放入null值；底层基于TreeMap
2. HashSet 是哈希表实现的，HashSet中的数据是无序的，可以放入null，但只能放入一个null，两者中的值都不能重复，就如数据库中唯一约束；底层基于HashMap

在HashSet中，基本的操作都是有HashMap底层实现的，因为HashSet底层是用HashMap存储数据的。当向HashSet中添加元素的时候，首先计算元素的hashCode值，然后通过**扰动计算和按位与**的方式计算出这个元素的存储位置，如果这个位置为空，就将元素添加进去；如果不为空，则用equals方法比较元素是否相等，相等就不添加，否则找一个空位添加。

TreeSet的底层是TreeMap的keySet()，而TreeMap是基于红黑树实现的，红黑树是一种平衡二叉查找树，它能保证任何一个节点的左右子树的高度差不会超过较矮的那棵的一倍。

TreeMap是按key排序的，元素在插入TreeSet时compareTo()方法要被调用，所以TreeSet中的元素要实现Comparable接口。TreeSet作为一种Set，它不允许出现重复元素。TreeSet是用compareTo()来判断重复元素的。
# 知识扩展
## HashSet，TreeSet，LinkedHashSet，BitSet有何区别

1. 功能不同：HashSet是功能最简单的Set，只提供去重的能力；LinkedHashSet不仅提供去重功能，而且还能记录插入和查询顺序；TreeSet提供了去重和排序的能力；BitSet不仅能提供去重能力，同时也能减少存储空间的浪费，不过对于普通的对象不太友好，需要做额外处理
2. 实现方式不同：HashSet基于HashMap，去重是根据HashCode和equals方法的；LinkedHashSet是基于LinkedHashMap，通过双向链表记录插入顺序；TreeSet是基于TreeMap的，去重是根据compareTo方法的；BitSet基于位数组，一般只用于数字的存储和去重
3. 其实BitSet只是叫做Set而已，它既没有实现Collection接口，也和Iterable接口没有什么关系，但是是名字相似而已
### 什么是BitSet？有什么作用？
顾名思义，BitSet是位集合，通常来说，位集合的底层的数据结构是一个bit数组，如果第n位为1，则表明数字n在该数组中。<br />举个例子，如果调用BitSet#set(10)，业务语意是把10放到BitSet中，内部的操作则是通过把二进制的第十位（低位）置为1。这样，就代表BitSet中包含了10这个数字。<br />不过，对于Java中的BitSet来讲，因为Java不知道bit类型，所以它的底层结构并不是一个bit类型数组，但是也不是一个byte类型数组，而是一个long类型的数组，这样设置的目的是因为long有64位，每次可以读取64位，在进行set或者or操作的时候，for循环的次数会更少，提高了性能。

它最大的好处就是对于多个数字来说，降低了存储空间，如正常情况下，将每一个int类型（32bit）的数字存储到内存中需要 4B * (2^31-1) = 8 GB，但是如果用BitSet的话，就会节省到原来的1/32。

> 一个整型占4个字节，一共有2^31-1个。


BitSet常见的使用例子往往和大数相关：

1. 现在有1千万个随机数，随机数的范围在1到1亿之间。求出将1到1亿之间没有在随机数中的数
2. 统计N亿个数据中没有出现的数据
3. 将N亿个不同数据进行排序等

但是BitSet也有缺点，譬如集合中存储一些差值比较大的数，如1亿和1两个数，就会导致内存的严重浪费





