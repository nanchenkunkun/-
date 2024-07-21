# 典型回答

String是不可变的，StringBuilder和StringBuffer是可变的。而StringBuffer是线程安全的，而StringBuilder是非线程安全的。

# 扩展知识

## String的不可变性

[✅String是如何实现不可变的？](https://www.yuque.com/hollis666/fo22bm/ik9x1gx4zddllhhg?view=doc_embed)

## 为什么设计成不可变的

[✅String为什么设计成不可变的？](https://www.yuque.com/hollis666/fo22bm/hhkgh2nsrlnf2g0g?view=doc_embed)
## String的"+"是如何实现的

使用+拼接字符串，其实只是Java提供的一个语法糖， 那么，我们就来解一解这个语法糖，看看他的内部原理到底是如何实现的。<br />还是这样一段代码。我们把他生成的字节码进行反编译，看看结果。

```latex
String wechat = "Hollis";
String introduce = "Chuang";
String hollis = wechat + "," + introduce;
```

反编译后的内容如下，反编译工具为jad。

```latex
String wechat = "Hollis";
String introduce = "Chuang";
String hollis = (new StringBuilder()).append(wechat).append(",").append(introduce).toString();
```

通过查看反编译以后的代码，我们可以发现，原来字符串常量在拼接过程中，是将String转成了StringBuilder后，使用其append方法进行处理的。

那么也就是说，Java中的+对字符串的拼接，其实现原理是使用StringBuilder.append。


## StringBuffer和StringBuilder

接下来我们看看`StringBuffer`和`StringBuilder`的实现原理。

和`String`类类似，`StringBuilder`类也封装了一个字符数组，定义如下：

```latex
char[] value;
```


与`String`不同的是，它并不是`final`的，所以他是可以修改的。另外，与`String`不同，字符数组中不一定所有位置都已经被使用，它有一个实例变量，表示数组中已经使用的字符个数，定义如下：

```latex
int count;
```

其append源码如下：

```latex
public StringBuilder append(String str) {
    super.append(str);
    return this;
}
```

该类继承了`AbstractStringBuilder`类，看下其`append`方法：

```latex
public AbstractStringBuilder append(String str) {
    if (str == null)
        return appendNull();
    int len = str.length();
    ensureCapacityInternal(count + len);
    str.getChars(0, len, value, count);
    count += len;
    return this;
}
```


append会直接拷贝字符到内部的字符数组中，如果字符数组长度不够，会进行扩展。

`StringBuffer`和`StringBuilder`类似，最大的区别就是`StringBuffer`是线程安全的，看一下`StringBuffer`的`append`方法。

```latex
public synchronized StringBuffer append(String str) {
    toStringCache = null;
    super.append(str);
    return this;
}
```

该方法使用`synchronized`进行声明，说明是一个线程安全的方法。而`StringBuilder`则不是线程安全的。

## 不要在for循环中使用+拼接字符串


前面我们分析过，其实使用`+`拼接字符串的实现原理也是使用的`StringBuilder`，那为什么不建议大家在for循环中使用呢？

```latex
我们把以下代码反编译下：

long t1 = System.currentTimeMillis();
String str = "hollis";
for (int i = 0; i < 50000; i++) {
    String s = String.valueOf(i);
    str += s;
}
long t2 = System.currentTimeMillis();
System.out.println("+ cost:" + (t2 - t1));
```


反编译后代码如下：

```latex
long t1 = System.currentTimeMillis();
String str = "hollis";
for(int i = 0; i < 50000; i++)
{
    String s = String.valueOf(i);
    str = (new StringBuilder()).append(str).append(s).toString();
}

long t2 = System.currentTimeMillis();
System.out.println((new StringBuilder()).append("+ cost:").append(t2 - t1).toString());
```

我们可以看到，反编译后的代码，在`for`循环中，每次都是`new`了一个`StringBuilder`，然后再把`String`转成`StringBuilder`，再进行`append`。

而频繁的新建对象当然要耗费很多时间了，不仅仅会耗费时间，频繁的创建对象，还会造成内存资源的浪费。

所以，阿里巴巴Java开发手册建议：循环体内，字符串的连接方式，使用 `StringBuilder` 的 `append` 方法进行扩展。而不要使用`+`。
