# 典型回答

while(true)和for(;;)都是做无限循环的代码，他俩有啥区别呢？

关于这个问题，网上有很多讨论，说那么多没用，直接反编译，看看字节码有啥区别就行了。

准备两段代码：

```java
public class HollisTest {

    public static void main(String[] args) {
        for(;;){
            System.out.println("this is hollis testing....");
        }
    }
}
```

```java
public class HollisTest {
    
    public static void main(String[] args) {
        while (true){
            System.out.println("this is hollis testing....");
        }
    }
}
```

分别将他们编译成class文件：

```java
javac HollisTest.java
```

然后再通过javap对class文件进行反编译，然后我们就会发现，两个文件内容，**一模一样！！！**

```java
Classfile /Users/hollis/workspace/chaojue/HLab/src/main/java/HollisTest.class
  Last modified 2023-6-18; size 463 bytes
  MD5 checksum 38eddb7d25748625d7c9aa377b6f66d3
  Compiled from "HollisTest.java"
public class HollisTest
  minor version: 0
  major version: 52
  flags: ACC_PUBLIC, ACC_SUPER
Constant pool:
   #1 = Methodref          #6.#16         // java/lang/Object."<init>":()V
   #2 = Fieldref           #17.#18        // java/lang/System.out:Ljava/io/PrintStream;
   #3 = String             #19            // this is hollis testing....
   #4 = Methodref          #20.#21        // java/io/PrintStream.println:(Ljava/lang/String;)V
   #5 = Class              #22            // HollisTest
   #6 = Class              #23            // java/lang/Object
   #7 = Utf8               <init>
   #8 = Utf8               ()V
   #9 = Utf8               Code
  #10 = Utf8               LineNumberTable
  #11 = Utf8               main
  #12 = Utf8               ([Ljava/lang/String;)V
  #13 = Utf8               StackMapTable
  #14 = Utf8               SourceFile
  #15 = Utf8               HollisTest.java
  #16 = NameAndType        #7:#8          // "<init>":()V
  #17 = Class              #24            // java/lang/System
  #18 = NameAndType        #25:#26        // out:Ljava/io/PrintStream;
  #19 = Utf8               this is hollis testing....
  #20 = Class              #27            // java/io/PrintStream
  #21 = NameAndType        #28:#29        // println:(Ljava/lang/String;)V
  #22 = Utf8               HollisTest
  #23 = Utf8               java/lang/Object
  #24 = Utf8               java/lang/System
  #25 = Utf8               out
  #26 = Utf8               Ljava/io/PrintStream;
  #27 = Utf8               java/io/PrintStream
  #28 = Utf8               println
  #29 = Utf8               (Ljava/lang/String;)V
{
  public HollisTest();
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
      stack=2, locals=1, args_size=1
         0: getstatic     #2                  // Field java/lang/System.out:Ljava/io/PrintStream;
         3: ldc           #3                  // String this is hollis testing....
         5: invokevirtual #4                  // Method java/io/PrintStream.println:(Ljava/lang/String;)V
         8: goto          0
      LineNumberTable:
        line 5: 0
      StackMapTable: number_of_entries = 1
        frame_type = 0 /* same */
}
SourceFile: "HollisTest.java"
```

可以看到，都是通过goto来干的，所以，这两者其实是没啥区别的。用哪个都行

有人愿意用while(true)因为他更清晰的看出来这里是个无限循环。有人愿意用for(;;)，因为有些IDE对于while(true)会给出警告。至于你，爱用啥用啥。
