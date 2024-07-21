# 典型回答

在Java中，对于字符串使用==比较的是字符串对象的引用地址是否相同。

因为a和b都是由字面量组成的字符串，它们的引用地址在编译时就已经确定了，并且在编译之后，会把字面量直接合在一起。因此，a == b的结果为true，因为它们指向的是同一个字符串对象。

[✅String、StringBuilder和StringBuffer的区别？](https://www.yuque.com/hollis666/fo22bm/pg23qhb7rgnuamd1?view=doc_embed&inner=TpASZ)

# 扩展知识

## 字面量

在计算机科学中，字面量（literal）是用于表达源代码中一个固定值的表示法（notation）。几乎所有计算机编程语言都具有对基本值的字面量表示，诸如：整数、浮点数以及字符串；而有很多也对布尔类型和字符类型的值也支持字面量表示；还有一些甚至对枚举类型的元素以及像数组、记录和对象等复合类型的值也支持字面量表示法。

以上是关于计算机科学中关于字面量的解释，并不是很容易理解。说简单点，字面量就是指由字母、数字等构成的字符串或者数值。

字面量只可以右值出现，所谓右值是指等号右边的值，如：int a=123这里的a为左值，123为右值。在这个例子中123就是字面量。

```c
int a = 123;
String s = "hollis";
```

上面的代码事例中，123和hollis都是字面量。

JVM为了提高性能和减少内存开销，在实例化字符串常量的时候进行了一些优化。为了减少在JVM中创建的字符串的数量，字符串类维护了一个字符串常量池。

在JVM中，有一块区域是运行时常量池，主要用来存储编译期生成的各种**字面量**和**符号引用**。

[✅运行时常量池和字符串常量池的关系是什么？](https://www.yuque.com/hollis666/fo22bm/qbaa4627yid4v1em?view=doc_embed)

了解Class文件结构或者做过Java代码的反编译的朋友可能都知道，在java代码被javac编译之后，文件结构中是包含一部分Constant pool的。比如以下代码：

```c
public static void main(String[] args) {
    String s = "Hollis";
}
```

经过编译后，常量池内容如下：

```c
 Constant pool:
   #1 = Methodref          #4.#20         // java/lang/Object."<init>":()V
   #2 = String             #21            // Hollis
   #3 = Class              #22            // StringDemo
   #4 = Class              #23            // java/lang/Object
   ...
   #16 = Utf8               s
   ..
   #21 = Utf8               Hollis
   #22 = Utf8               StringDemo
   #23 = Utf8               java/lang/Object
```

上面的Class文件中的常量池中，比较重要的几个内容：

```c
   #16 = Utf8               s
   #21 = Utf8               Hollis
   #22 = Utf8               StringDemo
```

上面几个常量中，`s`就是前面提到的符号引用，而`Hollis`就是前面提到的**字面量**。

而Class文件中的常量池部分的内容，会在运行期被运行时常量池加载进去。


