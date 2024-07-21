# 典型回答

在Java中，equals()和hashCode()方法通常是成对的，它们在使用基于Hash机制的数据结构时非常重要，例如HashMap、HashSet和Hashtable等。

- equals()：用于判断两个对象是否相等
- hashCode：生成对象的哈希码，返回值是一个整数，用于确定对象在哈希表中的位置。

为什么需要hashCode，主要是为了方便用在Hash结构的数据结构中，因为对于这种数据结构来说，想要把一个对象存进去，需要定位到他应该存放在哪个桶中，而这个桶的位置，就需要通过一个整数来获取，然后再对桶的长度取模（实际hashmap要比这复杂一些，可以看：[https://www.yuque.com/hollis666/fo22bm/sz24zwwrdg92qizg](https://www.yuque.com/hollis666/fo22bm/sz24zwwrdg92qizg)）。

HashMap的数据结构详细的请参考：<br />[✅HashMap的数据结构是怎样的？](https://www.yuque.com/hollis666/fo22bm/klz889cad0dpv2am?view=doc_embed)

那么，怎么能快速获取一个和这个对象有关的整数呢，那就是hashCode方法了。所以，hashCode的结果是和对象的内容息息相关的。那么也就意味着**如果两个对象通过equals()方法比较是相等的，那么它们的hashCode()方法必须返回相同的整数值。**

那么，在一个对象中，定义了equals方法之后，同时还需要定义hashCode方法， 因为这样在向hashMap、hashTable等中存放的时候，才能快速的定位到位置。

所以，基于两方面考虑，**一方面是效率，hashCode() 方法提供了一种快速计算对象哈希值的方式，这些哈希值用于确定对象在哈希表中的位置。这意味着可以快速定位到对象应该存储在哪个位置或者从哪个位置检索，显著提高了查找效率。**

**另外一方面是可以和equals做协同来保证数据的一致性和准确性。**根据 Java 的规范，如果两个对象通过 equals() 方法比较时是相等的，那么这两个对象的 hashCode() 方法必须返回相同的整数值。如果违反了这一规则，将导致哈希表等数据结构无法正确地处理对象，从而导致数据丢失和检索失败。

