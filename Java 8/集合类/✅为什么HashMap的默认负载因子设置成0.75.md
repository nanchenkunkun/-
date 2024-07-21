# 典型回答

我们知道，第一次创建HashMap的时候，就会指定其容量（如果未显示制定，默认是16），那随着我们不断的向HashMap中put元素的时候，就有可能会超过其容量，那么就需要有一个扩容机制。

[✅HashMap是如何扩容的？](https://www.yuque.com/hollis666/fo22bm/co1ul8?view=doc_embed)

从代码中我们可以看到，在向HashMap中添加元素过程中，如果 元素个数（size）超过临界值（threshold） 的时候，就会进行自动扩容（resize），并且，在扩容之后，还需要对HashMap中原有元素进行rehash，即将原来桶中的元素重新分配到新的桶中。

在HashMap中，临界值（threshold） = 负载因子（loadFactor） * 容量（capacity）。

**loadFactor是负载因子，表示HashMap满的程度，默认值为0.75f，也就是说默认情况下，当HashMap中元素个数达到了容量的3/4的时候就会进行自动扩容。**

为什么选择0.75呢？背后有什么考虑？为什么不是1，不是0.8？不是0.5，而是0.75呢？

在JDK的官方文档中，有这样一段描述：

> As a general rule, the default load factor (.75) offers a good tradeoff between time and space costs. Higher values decrease the space overhead but increase the lookup cost (reflected in most of the operations of the HashMap class, including get and put).
> 

大概意思是：一般来说，默认的负载因子(0.75)在时间和空间成本之间提供了很好的权衡。更高的值减少了空间开销，但增加了查找成本(反映在HashMap类的大多数操作中，包括get和put

试想一下，如果我们把负载因子设置成1，容量使用默认初始值16，那么表示一个HashMap需要在"满了"之后才会进行扩容。

那么在HashMap中，最好的情况是这16个元素通过hash算法之后分别落到了16个不同的桶中，否则就必然发生哈希碰撞。而且随着元素越多，哈希碰撞的概率越大，查找速度也会越低。

## 0.75的数学依据
另外，我们可以通过一种数学思维来计算下这个值是多少合适。

我们假设一个bucket空和非空的概率为0.5，我们用s表示容量，n表示已添加元素个数。

用s表示添加的键的大小和n个键的数目。根据二项式定理，桶为空的概率为:

`P(0) = C(n, 0) * (1/s)^0 * (1 - 1/s)^(n - 0)`<br />因此，如果桶中元素个数小于以下数值，则桶可能是空的：log(2)/log(s/(s - 1))

当s趋于无穷大时，如果增加的键的数量使P(0) = 0.5，那么n/s很快趋近于log(2) 约等于0.693...所以，合理值大概在0.7左右。

当然，这个数学计算方法，并不是在Java的官方文档中体现的，我们也无从考察到底有没有这层考虑，这个推测来源于Stack Overflor（https://stackoverflow.com/questions/10901752/what-is-the-significance-of-load-factor-in-hashmap）
## 0.75的必然因素
理论上我们认为负载因子不能太大，不然会导致大量的哈希冲突，也不能太小，那样会浪费空间。

通过一个数学推理，测算出这个数值在0.7左右是比较合理的。

那么，为什么最终选定了0.75呢？因为threshold=loadFactor*capacity，并且capacity永远都是2的幂，为了保证负载因子（loadFactor） * 容量（capacity）的结果是一个整数，这个值是0.75(3/4)比较合理，因为这个数和任何2的幂乘积结果都是整数。
