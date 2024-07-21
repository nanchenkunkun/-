# 典型回答

Java进程，在运行时会占用多块内存区域，其中我们比较熟的就是堆、栈、等区域，但是，其实详细列举的话还是有挺多的，**主要包含以下部分区域**：

![image.png](https://cdn.nlark.com/yuque/0/2024/png/5378072/1707114803397-eeb6e8b3-c371-4b57-94a0-30034df8f17d.png#averageHue=%23faf9f9&clientId=u414c80f6-0ce5-4&from=paste&height=664&id=u86dcaaf9&originHeight=1700&originWidth=2326&originalType=binary&ratio=2&rotation=0&showTitle=false&size=1754346&status=done&style=none&taskId=u38ae96c5-cd98-4e48-bbc2-4610f69bf65&title=&width=909)

### 堆

堆是存储对象实例的运行时内存区域。它是虚拟机运行时的内存总体的最大的一块，也一直占据着虚拟机内存总量的一大部分。Java堆由Java虚拟机管理，用于存放对象实例，是几乎所有的对象实例都要在上面分配内存。此外，Java堆还用于垃圾回收，虚拟机发现没有被引用的对象时，就会对堆中对象进行垃圾回收，以释放内存空间。

堆上根据分代可以分为年轻代和老年代。

### 栈

**Java虚拟机栈**：一种线程私有的存储器，用于存储Java中的局部变量。根据Java虚拟机规范，每次方法调用都会创建一个栈帧，该栈帧用于存储局部变量，操作数栈，动态链接，方法出口等信息。当方法执行完毕之后，这个栈帧就会被弹出，变量作用域就会结束，数据就会从栈中消失。

**本地方法栈**：本地方法栈是一种特殊的栈，它与Java虚拟机栈有着相同的功能，但是它支持本地代码（ Native Code ）的执行。本地方法栈中存放本地方法（ Native Method ）的参数和局部变量，以及其他一些附加信息。这些本地方法一般是用C等本地语言实现的，虚拟机在执行这些方法时就会通过本地方法栈来调用这些本地方法。

### 堆外内存

堆外内存则是在堆之外的一块持久化的内存空间。这种内存通常由操作系统管理，因此对于大规模数据存储和快速访问来说，使用堆外内存可以提供更好的性能和控制。在我们熟知的C语言中，分配的就是机器内存，就和我们说的堆外内存类似了。

[✅什么是堆外内存？如何使用堆外内存？](https://www.yuque.com/hollis666/fo22bm/roit5c9y04z6fqae?view=doc_embed)

![image.png](https://cdn.nlark.com/yuque/0/2024/png/5378072/1707115324751-bfa17f40-83b7-465b-99d5-2926817c6bba.png#averageHue=%23cddcef&clientId=u414c80f6-0ce5-4&from=paste&height=302&id=u41f4359e&originHeight=604&originWidth=1028&originalType=binary&ratio=2&rotation=0&showTitle=false&size=324674&status=done&style=none&taskId=u4775dff6-8bda-424d-b950-411f3b056ef&title=&width=514)

堆外内存包括了**元空间**、压**缩类空间**、**代码缓冲区、直接缓冲区 **4个部分。

- **元空间（Meta Space）**：从JDK 1.8开始，HotSpot虚拟机对方法区的实现进行了重大改变。永久代被移除，取而代之的是元空间（Metaspace）。元空间是用来来存储类的元数据信息的。
- **压缩类空间（Compressed Class Space）**：压缩类空间是元空间的一部分，专门用于存储类的元数据，而且在使用64位JVM时，通过使用较小的指针（通常是32位的指针）来引用类的元数据，从而减少了内存的使用量。
- **代码缓冲区（Code Cache）**：主要用于存储编译器编译后的本地机器代码。当Java方法被JVM的即时编译器（JIT编译器）编译成本地代码（Native Code）后，这些代码被存储在代码缓冲区中，以便后续直接执行，提高程序运行效率。
- **直接缓冲区（Direct Buffer）：**直接缓冲区（Direct Buffer）是Java NIO中的一个概念，用于在Java程序和操作系统之间高效地传递数据。与传统的Java IO相比，NIO引入了通道（Channel）和缓冲区（Buffer）的概念，使得数据的读写更加高效。直接缓冲区就是这些缓冲区中的一种，其特点是它在物理内存中分配存储空间，从而减少了数据在Java堆内存和操作系统之间来回复制的需要，提高了数据处理的效率。

### 非JVM内存

**本地运行库**指的是操作系统中用本地编程语言（如C或C++）编写的库，这些库直接运行在操作系统上，而不是在Java虚拟机（JVM）内部执行。

这些库提供了一种方式，允许Java程序执行那些Java本身不直接支持的操作，比如系统级调用、访问特定硬件设备或使用特定于平台的特性和函数。由于这些库是用非Java语言编写的，它们能够提供更接近硬件层面的性能和功能。

**JNI（Java Native Interface）**是一个编程框架，允许Java代码与本地代码（如C和C++代码）进行交互。它是Java平台的一部分，为Java程序调用本地方法提供了一套标准的接口。通过JNI，Java程序能够使用本地方法来执行那些用Java语言难以或无法直接实现的任务，比如直接访问系统资源、调用操作系统API、使用特定硬件设备或实现性能关键型组件。

# 扩展知识

## JVM运行时内存区域

[✅JVM的运行时内存区域是怎样的？](https://www.yuque.com/hollis666/fo22bm/oyxrdhamqrmn291o?view=doc_embed)
