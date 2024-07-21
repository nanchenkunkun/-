# 典型回答

（本文并不算一道面试题，因为面试的时候很少有人问，但是这个对于理解intern的原理是比较有帮助的，所以就写了。然后有人反馈自己代码执行和我文中的不一样，可能的原因有很多，比如JDK版本不同、操作系统不同、本地编译过的其他代码也有影响等。故而如果现象不一致，可以使用一些在线的Java代码执行工具测试，如：[https://www.bejson.com/runcode/java/](https://www.bejson.com/runcode/java/) 。）

以下代码中，在JDK 1.8中，JDK 11及以上版本中执行后结果不是一样的。

```
String s3 = new String("1") + new String("1");
s3.intern();
String s4 = "11";
System.out.println(s3 == s4);
```

你会发现，在JDK 1.8中，以上代码得到的结果是true，而JDK 11及以上的版本中结果却是false。

那么，再稍作修改呢？在目前的所有JDK版本中，执行以下代码：

```
String s3 = new String("3") + new String("3");// ①
s3.intern();// ②
String s4 = "33";
System.out.println(s3 == s4);// ③
```

得到的结果也是true，你知道为什么嘛？

看这篇文章之前，请先阅读以下文章，先确保自己了解了intern的原理！！！

[✅String中intern的原理是什么？](https://www.yuque.com/hollis666/fo22bm/yr32wu44yxt5l8nh?view=doc_embed)

出现上述现象，肯定是因为在JDK 11 及以上的版本中，"11"这个字面量已经被提前存入字符串池了。那什么时候存进去的呢？（这个问题，全网应该没人提过）

经过我七七四十九天的研究，终于发现了端倪，就在以下代码中：[Source.java](https://github.com/zxiaofan/JDK/blob/19a6c71e52f3ecd74e4a66be5d0d552ce7175531/jdk-11.0.2/src/jdk.compiler/com/sun/tools/javac/code/Source.java)

```
public enum Source {
    /** 1.0 had no inner classes, and so could not pass the JCK. */
    // public static final Source JDK1_0 =              new Source("1.0");

    /** 1.1 did not have strictfp, and so could not pass the JCK. */
    // public static final Source JDK1_1 =              new Source("1.1");

    /** 1.2 introduced strictfp. */
    JDK1_2("1.2"),

    /** 1.3 is the same language as 1.2. */
    JDK1_3("1.3"),

    /** 1.4 introduced assert. */
    JDK1_4("1.4"),

    /** 1.5 introduced generics, attributes, foreach, boxing, static import,
     *  covariant return, enums, varargs, et al. */
    JDK5("5"),

    /** 1.6 reports encoding problems as errors instead of warnings. */
    JDK6("6"),

    /** 1.7 introduced try-with-resources, multi-catch, string switch, etc. */
    JDK7("7"),

    /** 1.8 lambda expressions and default methods. */
    JDK8("8"),

    /** 1.9 modularity. */
    JDK9("9"),

    /** 1.10 local-variable type inference (var). */
    JDK10("10"),

    /** 1.11 covers the to be determined language features that will be added in JDK 11. */
    JDK11("11");
}
```

看到了么，xdm，在JDK 11 的源码中，定义了"11"这个字面量，那么他会提前进入到字符串池中，那么后续的intern的过程就会直接从字符串池中获取到这个字符串引用。

按照这个思路，大家可以在JDK 11中执行以下代码：

```
String s3 = new String("1") + new String("1");
s3.intern();
String s4 = "11";
System.out.println(s3 == s4);


String s3 = new String("1") + new String("2");
s3.intern();
String s4 = "12";
System.out.println(s3 == s4);
```

得到的结果就是false和true。

或者我是在JDK 21中分别执行了以下代码：

```
String s3 = new String("2") + new String("1");
s3.intern();
String s4 = "21";
System.out.println(s3 == s4);


String s3 = new String("2") + new String("2");
s3.intern();
String s4 = "22";
System.out.println(s3 == s4);
```

得到的结果就也是false和true。
