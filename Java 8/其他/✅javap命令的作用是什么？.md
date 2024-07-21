# 典型回答

javap是jdk自带的一个工具，可以对代码反编译，也可以查看java编译器生成的字节码。

[✅什么是编译和反编译？](https://www.yuque.com/hollis666/fo22bm/gpd672xz0sx2qyyh?view=doc_embed)

一般情况下，很少有人使用javap对class文件进行反编译，因为有很多成熟的反编译工具可以使用，比如jad。但是，javap还可以查看java编译器为我们生成的字节码。通过它，可以对照源代码和字节码，从而了解很多编译器内部的工作。

# 扩展知识

## 使用

javap命令分解一个class文件，它根据options来决定到底输出什么。如果没有使用options，那么javap将会输出包，类里的protected和public域以及类里的所有方法。javap将会把它们输出在标准输出上。来看这个例子，先编译(javac)下面这个类。

```c
public class JavapTest {
    public static void main(String[] args) {
        String info = "this is hollis testing javap";

        System.out.println(info);

        Integer integer = new Integer(1);

        String string = integer.toString();

        String string1 = "Hollis" + "Chuang";
    }
}

```

在命令行上键入javap DocFooter后，输出结果如下：

```c
➜  javap JavapTest 


Compiled from "JavapTest.java"
public class JavapTest {
  public JavapTest();
  public static void main(java.lang.String[]);
}

```

如果加入了-c，即javap -c JavapTest，那么输出结果如下：

```c
➜  javap -c JavapTest


Compiled from "JavapTest.java"
public class JavapTest {
  public JavapTest();
    Code:
       0: aload_0
       1: invokespecial #1                  // Method java/lang/Object."<init>":()V
       4: return

  public static void main(java.lang.String[]);
    Code:
       0: ldc           #2                  // String this is hollis testing javap
       2: astore_1
       3: getstatic     #3                  // Field java/lang/System.out:Ljava/io/PrintStream;
       6: aload_1
       7: invokevirtual #4                  // Method java/io/PrintStream.println:(Ljava/lang/String;)V
      10: new           #5                  // class java/lang/Integer
      13: dup
      14: iconst_1
      15: invokespecial #6                  // Method java/lang/Integer."<init>":(I)V
      18: astore_2
      19: aload_2
      20: invokevirtual #7                  // Method java/lang/Integer.toString:()Ljava/lang/String;
      23: astore_3
      24: ldc           #8                  // String HollisChuang
      26: astore        4
      28: return
}

```

其中的ldf dup astore都是一条条指令。

如果想要查看编译后的class文件中的常量池信息，可以加上-v

```c
➜  javap -c -v JavapTest
Classfile /Users/hollis/workspace/chaojue/HLab/src/main/java/JavapTest.class
  Last modified 2023-10-9; size 578 bytes
  MD5 checksum 7cc427c7df602940ea859d1847034844
  Compiled from "JavapTest.java"
public class JavapTest
  minor version: 0
  major version: 52
  flags: ACC_PUBLIC, ACC_SUPER
Constant pool:
   #1 = Methodref          #10.#19        // java/lang/Object."<init>":()V
   #2 = String             #20            // this is hollis testing javap
   #3 = Fieldref           #21.#22        // java/lang/System.out:Ljava/io/PrintStream;
   #4 = Methodref          #23.#24        // java/io/PrintStream.println:(Ljava/lang/String;)V
   #5 = Class              #25            // java/lang/Integer
   #6 = Methodref          #5.#26         // java/lang/Integer."<init>":(I)V
   #7 = Methodref          #5.#27         // java/lang/Integer.toString:()Ljava/lang/String;
   #8 = String             #28            // HollisChuang
   #9 = Class              #29            // JavapTest
  #10 = Class              #30            // java/lang/Object
  #11 = Utf8               <init>
  #12 = Utf8               ()V
  #13 = Utf8               Code
  #14 = Utf8               LineNumberTable
  #15 = Utf8               main
  #16 = Utf8               ([Ljava/lang/String;)V
  #17 = Utf8               SourceFile
  #18 = Utf8               JavapTest.java
  #19 = NameAndType        #11:#12        // "<init>":()V
  #20 = Utf8               this is hollis testing javap
  #21 = Class              #31            // java/lang/System
  #22 = NameAndType        #32:#33        // out:Ljava/io/PrintStream;
  #23 = Class              #34            // java/io/PrintStream
  #24 = NameAndType        #35:#36        // println:(Ljava/lang/String;)V
  #25 = Utf8               java/lang/Integer
  #26 = NameAndType        #11:#37        // "<init>":(I)V
  #27 = NameAndType        #38:#39        // toString:()Ljava/lang/String;
  #28 = Utf8               HollisChuang
  #29 = Utf8               JavapTest
  #30 = Utf8               java/lang/Object
  #31 = Utf8               java/lang/System
  #32 = Utf8               out
  #33 = Utf8               Ljava/io/PrintStream;
  #34 = Utf8               java/io/PrintStream
  #35 = Utf8               println
  #36 = Utf8               (Ljava/lang/String;)V
  #37 = Utf8               (I)V
  #38 = Utf8               toString
  #39 = Utf8               ()Ljava/lang/String;
{
  public JavapTest();
    descriptor: ()V
    flags: ACC_PUBLIC
    Code:
      stack=1, locals=1, args_size=1
         0: aload_0
         1: invokespecial #1                  // Method java/lang/Object."<init>":()V
         4: return
      LineNumberTable:
        line 1: 0

  public static void main(java.lang.String[]);
    descriptor: ([Ljava/lang/String;)V
    flags: ACC_PUBLIC, ACC_STATIC
    Code:
      stack=3, locals=5, args_size=1
         0: ldc           #2                  // String this is hollis testing javap
         2: astore_1
         3: getstatic     #3                  // Field java/lang/System.out:Ljava/io/PrintStream;
         6: aload_1
         7: invokevirtual #4                  // Method java/io/PrintStream.println:(Ljava/lang/String;)V
        10: new           #5                  // class java/lang/Integer
        13: dup
        14: iconst_1
        15: invokespecial #6                  // Method java/lang/Integer."<init>":(I)V
        18: astore_2
        19: aload_2
        20: invokevirtual #7                  // Method java/lang/Integer.toString:()Ljava/lang/String;
        23: astore_3
        24: ldc           #8                  // String HollisChuang
        26: astore        4
        28: return
      LineNumberTable:
        line 3: 0
        line 5: 3
        line 7: 10
        line 9: 19
        line 11: 24
        line 12: 28
}
SourceFile: "JavapTest.java"

```

上面的Constant pool部分就是常量池，可以看到其中包含了两个字符串常量：

![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1696857383822-11f7a85a-c60c-4b2c-ae3c-d6939ca957d2.png#averageHue=%23fbf5f2&clientId=u1881cd1f-3a68-4&from=paste&height=183&id=u4419da0a&originHeight=183&originWidth=742&originalType=binary&ratio=1&rotation=0&showTitle=false&size=141298&status=done&style=none&taskId=ucb8b7f1a-af75-4781-a42b-955759cdb1a&title=&width=742)

## 

通过上面的结果我们也可以知道，javap并没有将字节码反编译成java文件，而是生成了一种我们可以看得懂字节码。其实javap生成的文件仍然是字节码，只是程序员可以稍微看得懂一些。如果你对字节码有所掌握，还是可以看得懂以上的代码的。其实就是把String转成hashcode，然后进行比较。

一般情况下我们会用到javap命令的时候不多，一般只有在真的需要看字节码的时候才会用到。但是字节码中间暴露的东西是最全的，你肯定有机会用到，比如我在分析synchronized的原理的时候就有是用到javap。通过javap生成的字节码，我发现synchronized底层依赖了ACC_SYNCHRONIZED标记和monitorenter、monitorexit两个指令来实现同步。
