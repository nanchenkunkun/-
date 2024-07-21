# 典型回答

有区别，而且区别很大。

因为double是不精确的，所以使用一个不精确的数字来创建BigDecimal，得到的数字也是不精确的。如0.1这个数字，double只能表示他的近似值。

所以，**当我们使用new BigDecimal(0.1)创建一个BigDecimal 的时候，其实创建出来的值并不是正好等于0.1的。**

而是0.1000000000000000055511151231257827021181583404541015625。这是因为double自身表示的只是一个近似值。

而对于BigDecimal(String) ，当我们使用new BigDecimal("0.1")创建一个BigDecimal 的时候，其实创建出来的值正好就是等于0.1的。

那么他的标度也就是1

# 扩展知识
在《阿里巴巴Java开发手册》中有一条建议，或者说是要求：

![](http://www.hollischuang.com/wp-content/uploads/2021/01/16119907257353.jpg#id=PAiBS&originHeight=379&originWidth=1443&originalType=binary&ratio=1&rotation=0&showTitle=false&status=done&style=none&title=)

### BigDecimal如何精确计数？

如果大家看过BigDecimal的源码，其实可以发现，**实际上一个BigDecimal是通过一个"无标度值"和一个"标度"来表示一个数的。**

> **无标度值（Unscaled Value）**：这是一个整数，表示BigDecimal的实际数值。
> **标度（Scale）**：这是一个整数，表示小数点后的位数。
> BigDecimal的实际数值计算公式为：unscaledValue × 10^(-scale)。


假设有一个BigDecimal表示的数值是123.45，那么无标度值（Unscaled Value）是12345。标度（Scale）是2。因为123.45 = 12345 × 10^(-2)。

涉及到的字段就是这几个：

```java
public class BigDecimal extends Number implements Comparable<BigDecimal> {
    private final BigInteger intVal;
    private final int scale; 
    private final transient long intCompact;
}
```

关于无标度值的压缩机制大家了解即可，不是本文的重点，大家只需要知道BigDecimal主要是通过一个无标度值和标度来表示的就行了。

**那么标度到底是什么呢？**

除了scale这个字段，在BigDecimal中还提供了scale()方法，用来返回这个BigDecimal的标度。

```java
/**
 * Returns the <i>scale</i> of this {@code BigDecimal}.  If zero
 * or positive, the scale is the number of digits to the right of
 * the decimal point.  If negative, the unscaled value of the
 * number is multiplied by ten to the power of the negation of the
 * scale.  For example, a scale of {@code -3} means the unscaled
 * value is multiplied by 1000.
 *
 * @return the scale of this {@code BigDecimal}.
 */
public int scale() {
    return scale;
}
```

那么，scale到底表示的是什么，其实上面的注释已经说的很清楚了。

> 当标度为正数时，它表示小数点后的位数。例如，在数字123.45中，他的无标度值为12345，标度是2。
> 当标度为零时，BigDecimal表示一个整数。
> 当标度为负数时，它表示小数点向左移动的位数，相当于将数字乘以 10 的绝对值的次方。例如，一个数值为1234500，那么他可以用value是12345，scale为-2来表示，因为1234500 * 10^(-2) = 12345。（当需要处理非常大的整数时，可以使用负数的标度来指定小数点左侧的位数。这在需要保持整数的精度而又不想丢失尾部零位时很有用。）


**而二进制无法表示的0.1，使用BigDecimal就可以表示了，及通过无标度值1和标度1来表示。**

我们都知道，想要创建一个对象，需要使用该类的构造方法，在BigDecimal中一共有以下4个构造方法：

```java
BigDecimal(int)
BigDecimal(double) 
BigDecimal(long) 
BigDecimal(String)
```

以上四个方法，创建出来的BigDecimal的标度（scale）是不同的。

其中 BigDecimal(int)和BigDecimal(long) 比较简单，因为都是整数，所以他们的标度都是0。

而BigDecimal(double) 和BigDecimal(String)的标度就有很多学问了。

### BigDecimal(double)有什么问题

BigDecimal中提供了一个通过double创建BigDecimal的方法——BigDecimal(double) ，但是，同时也给我们留了一个坑！

因为我们知道，double表示的小数是不精确的，如0.1这个数字，double只能表示他的近似值。

所以，**当我们使用new BigDecimal(0.1)创建一个BigDecimal 的时候，其实创建出来的值并不是正好等于0.1的。**

而是0.1000000000000000055511151231257827021181583404541015625。这是因为double自身表示的只是一个近似值。

![](http://www.hollischuang.com/wp-content/uploads/2021/01/16119945021181.jpg#id=PQAZA&originHeight=402&originWidth=1080&originalType=binary&ratio=1&rotation=0&showTitle=false&status=done&style=none&title=)

**所以，如果我们在代码中，使用BigDecimal(double) 来创建一个BigDecimal的话，那么是损失了精度的，这是极其严重的。**

### 使用BigDecimal(String)创建

那么，该如何创建一个精确的BigDecimal来表示小数呢，答案是使用String创建。

而对于BigDecimal(String) ，当我们使用new BigDecimal("0.1")创建一个BigDecimal 的时候，其实创建出来的值正好就是等于0.1的。

那么他的标度也就是1。

但是需要注意的是，new BigDecimal("0.10000")和new BigDecimal("0.1")这两个数的标度分别是5和1，如果使用BigDecimal的equals方法比较，得到的结果是false。

那么，想要创建一个能精确的表示0.1的BigDecimal，请使用以下两种方式：

```java
BigDecimal recommend1 = new BigDecimal("0.1");
BigDecimal recommend2 = BigDecimal.valueOf(0.1);
```

这里，留一个思考题，BigDecimal.valueOf()是调用Double.toString方法实现的，那么，既然double都是不精确的，BigDecimal.valueOf(0.1)怎么保证精确呢？

### 总结

因为计算机采用二进制处理数据，但是很多小数，如0.1的二进制是一个无限循环小数，而这种数字在计算机中是无法精确表示的。

所以，人们采用了一种通过近似值的方式在计算机中表示，于是就有了单精度浮点数和双精度浮点数等。

所以，作为单精度浮点数的float和双精度浮点数的double，在表示小数的时候只是近似值，并不是真实值。

所以，当使用BigDecimal(Double)创建一个的时候，得到的BigDecimal是损失了精度的。

而使用一个损失了精度的数字进行计算，得到的结果也是不精确的。

想要避免这个问题，可以通过BigDecimal(String)的方式创建BigDecimal，这样的情况下，0.1就会被精确的表示出来。

其表现形式是一个无标度数值1，和一个标度1的组合。
