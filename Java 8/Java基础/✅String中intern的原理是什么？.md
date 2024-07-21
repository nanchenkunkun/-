# 典型回答

字符串常量池中的常量有两种来源：

1、字面量会在编译期先进入到Class常量池，然后再在运行期进去到字符串池，<br />2、在运行期通过intern将字符串对象手动添加到字符串常量池中。

> 字面量：[https://www.yuque.com/hollis666/fo22bm/pwmmw153wb4f2cgq#AfDwW](https://www.yuque.com/hollis666/fo22bm/pwmmw153wb4f2cgq#AfDwW)


intern的作用是这样的：

**如果字符串池中已经存在一个等于该字符串的对象，intern()方法会返回这个已存在的对象的引用。**

**如果字符串池中没有等于该字符串的对象，intern()方法会将该字符串添加到字符串池中，并返回对新添加的字符串对象的引用。**

```
String s = new String("Hollis") + new String("Chuang");
s.intern();
```


所以，无论何时通过intern()方法获取字符串的引用，都会得到字符串池中的引用，这样可以确保相同的字符串在内存中只有一个实例。

很多人以为知道以上信息，就算是了解intern了，那么请回答一下这个问题：

```
public static void main(String[] args) {
    String s1 = new String("a"); 
    s1.intern(); 
    String s2 = "a";
    System.out.println(s1 == s2); // false
    
    String s3 = new String("a") + new String("a");
    s3.intern();
    String s4 = "aa";
    System.out.println(s3 == s4);//  true
}
```

大家可以在 JDK 1.7以上版本中尝试运行以上两段代码，就会发现，s1 == s2的结果是 false，但是s3 == s4的结果是 true。

**这是为什么呢？（后文所有case均基于JDK 1.8运行）**
# 扩展知识

## 字符串常量进入常量池的时机

[✅字符串常量是什么时候进入到字符串常量池的？](https://www.yuque.com/hollis666/fo22bm/os0m38kyugpxvgsq?view=doc_embed)

## intern原理

先看一下上面这篇（让你看就去看，你不看，然后就看不懂这篇，就怪我讲的不清楚！哼，咋那么犟呢！？），了解了字符串常量进入常量池的时机之后，我们再回过头分析一下前面的例子：

```
public static void main(String[] args) {
    String s1 = new String("a"); // ①
    s1.intern(); // ②
    String s2 = "a";// ③
    System.out.println(s1 == s2); // ④   false
    
    String s3 = new String("a") + new String("a");// ⑤
    s3.intern();// ⑥
    String s4 = "aa";// ⑦
    System.out.println(s3 == s4);// ⑧    true

}
```


这个类被编译后，Class常量池中应该有"a"和"aa"这两个字符串，这两个字符串最终会进到字符串池。**但是，字面量"a"在代码①这一行，就会被存入字符串池，而字面量"aa"则是在代码⑦这一行才会存入字符串池。**

以上代码的执行过程：<br />第①行，new 一个 String 对象，并让 s1指向他。<br />第②行，对 s1执行 intern，但是因为"a"这个字符串已经在字符串池中，所以会直接返回原来的引用，但是并没有赋值给任何一个变量。<br />第③行，s2指向常量池中的"a"；

所以，s1和 s2并不相等！

第⑤行，new 一个 String 对象，并让 s3 指向他。<br />第⑥行，对 s3 执行 intern，但是目前字符串池中还没有"aa"这个字符串，于是会把<s3指向的String对象的引用>放入<字符串常量池><br />第⑦行，因为"aa"这个字符串已经在字符串池中，所以会直接返回原来的引用，并赋值给 s4；

所以，s3和 s4 相等！

而如果我们对代码稍作修改：

```
String s = "aa";// ①
String s3 = new String("a") + new String("a");// ②
s3.intern();// ③
String s4 = "aa";
System.out.println(s3 == s4);// ④
```

以上代码得到的结果则是：false

第①行，创建一个字符串aa，并且因为它是字面量，所以把他放到字符串池。<br />第②行，new一个 String 对象，并让 s3 指向他。<br />第③行，对 s3 执行 intern，但是目前字符串池中已经有"aa"这个字符串，所以会直接返回s的引用，但是并没有对s3进行赋值<br />第④行，因为"aa"这个字符串已经在字符串池中，所以会直接返回原来的引用，即s的引用，并赋值给 s4；所以，s3和 s4 不相等。

## a和1有什么不同

关于这个问题，我们还有一个变型，可以帮大家更好的理解intern，请大家分别在JDK 1.8和JDK 11及以上的版本中执行以下代码：

```
String s3 = new String("1") + new String("1");// ①
s3.intern();// ②
String s4 = "11";
System.out.println(s3 == s4);// ③
```

你会发现，在JDK 1.8中，以上代码得到的结果是true，而JDK 11及以上的版本中结果却是false。（有人反馈自己代码执行和我文中的不一样，可能的原因有很多，比如JDK版本不同、操作系统不同、本地编译过的其他代码也有影响等。故而如果现象不一致，可以使用一些在线的Java代码执行工具测试，如：[https://www.bejson.com/runcode/java/](https://www.bejson.com/runcode/java/) 。）

那么，再稍作修改呢？在目前的所有JDK版本中，执行以下代码：

```
String s3 = new String("3") + new String("3");// ①
s3.intern();// ②
String s4 = "33";
System.out.println(s3 == s4);// ③
```

得到的结果也是true，你知道为什么嘛？

答案在下文中：

[✅为什么这段代码在JDK不同版本中结果不同](https://www.yuque.com/hollis666/fo22bm/iky8sebui0cv6sli?view=doc_embed)
