# 典型回答

我们都知道String是不可变的，但是它是怎么实现的呢？

先来看一段String的源码（JDK 1.8）：

```java
public final class String
    implements java.io.Serializable, Comparable<String>, CharSequence {

    /** The value is used for character storage. */
    private final char value[];

    /** use serialVersionUID from JDK 1.0.2 for interoperability */
    private static final long serialVersionUID = -6849794470754667710L;

    public String substring(int beginIndex) {
        if (beginIndex < 0) {
            throw new StringIndexOutOfBoundsException(beginIndex);
        }
        int subLen = value.length - beginIndex;
        if (subLen < 0) {
            throw new StringIndexOutOfBoundsException(subLen);
        }
        return (beginIndex == 0) ? this : new String(value, beginIndex, subLen);
    }

    public String concat(String str) {
        int otherLen = str.length();
        if (otherLen == 0) {
            return this;
        }
        int len = value.length;
        char buf[] = Arrays.copyOf(value, len + otherLen);
        str.getChars(buf, len);
        return new String(buf, true);
    }
}
```

以上代码，其实就包含了String不可变的主要实现了。

1. **String类被声明为final，这意味着它不能被继承。那么他里面的方法就是没办法被覆盖的。**
2. **用final修饰字符串内容的char[]（从JDK 1.9开始，char[]变成了byte[]），由于该数组被声明为final，一旦数组被初始化，就不能再指向其他数组。**
3. **String类没有提供用于修改字符串内容的公共方法。例如，没有提供用于追加、删除或修改字符的方法。如果需要对字符串进行修改，会创建一个新的String对象。**

[为什么JDK 9中把String的char[]改成了byte[]？](https://www.yuque.com/hollis666/fo22bm/hcy7n8p0rhxro0xq?view=doc_embed)


再然后，在他的一些方法中，如substring、concat等，在代码中如果有涉及到字符串的修改，也是通过new String()的方式新建了一个字符串。

所以，通过以上方式，使得一个字符串的内容，一旦被创建出来，就是不可以修改的了。

> 不可变对象是在完全创建后其内部状态保持不变的对象。这意味着，一旦对象被赋值给变量，我们既不能更新引用，也不能通过任何方式改变内部状态。


可是有人会有疑惑，String为什么不可变，我的代码中经常改变String的值啊，如下：

```latex
String s = "abcd";
s = s.concat("ef");
```

这样，操作，不就将原本的"abcd"的字符串改变成"abcdef"了么？

但是，虽然字符串内容看上去从"abcd"变成了"abcdef"，但是实际上，我们得到的已经是一个新的字符串了。

![](https://cdn.nlark.com/yuque/0/2023/jpeg/5378072/1693569145559-1464948e-b069-4234-8f03-40dba93f044b.jpeg#averageHue=%23f5f5f5&clientId=ud59952de-2a57-4&id=Vjiee&originHeight=279&originWidth=650&originalType=binary&ratio=1&rotation=0&showTitle=false&status=done&style=none&taskId=u76ffae3e-d80d-4fce-a5b6-30cb769709c&title=)

如上图，在堆中重新创建了一个"abcdef"字符串，和"abcd"并不是同一个对象。

所以，一旦一个string对象在内存(堆)中被创建出来，他就无法被修改。而且，String类的所有方法都没有改变字符串本身的值，都是返回了一个新的对象。

如果我们想要一个可修改的字符串，可以选择StringBuffer 或者 StringBuilder这两个代替String。
