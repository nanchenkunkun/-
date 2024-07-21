# 典型回答

OutOfMemory 是内存溢出错误，他通常发生在程序试图分配内存时，但是超出可用内存限制。这可能是因为程序使用了太多内存，或者由于内存泄漏而导致内存不断累积。

StackOverflow 是栈溢出错误，他通常发生在程序的调用栈变得过深时，如递归调用。每次函数调用都会在栈上分配一些内存，当递归调用或者函数调用层次过深时，栈空间会被耗尽，从而导致StackOverflowError。

[✅请分别写出一个Java堆、栈、元空间溢出的代码](https://www.yuque.com/hollis666/fo22bm/qlzszvlm5siwrl2p?view=doc_embed)

OutOfMemory一般发生在Java的堆内存上，StackOverflow一般发生在Java的栈内存中。但是也不绝对，在栈上也可能发生OutOfMemory。
# 扩展知识

## OutOfMemory

OutOfMemory在具体报错上还有以下几种情况：

1. Java Heap Space：这是最常见的OutOfMemoryError。它发生在Java堆内存不足，通常由程序中创建的对象过多或者单个对象太大引起。这种错误可能导致Java应用程序崩溃。
2. PermGen Space（在Java 7之前）或 Metaspace（在Java 8及更高版本）：这种错误发生在永久代（Java 7之前）或元空间（Java 8及更高版本）不足。通常由于加载过多的类或创建过多的动态代理类等原因引起。
3. Native Heap：这种错误发生在本机堆内存不足。Java虚拟机使用本机代码（native code）来执行某些操作，如本机方法，这些操作可能会占用本机堆内存。
4. Direct Memory：这种错误发生在程序使用NIO（New I/O）库或直接内存缓冲区时，由于分配了过多的直接内存而耗尽。
5. GC Overhead Limit Exceeded：这个错误发生在垃圾收集器花费了太多时间进行垃圾回收，而没有足够的内存被释放。这通常是由于内存不足以满足垃圾收集需求而引起的。
6. Requested array size exceeds VM limit：这个错误发生在试图创建一个太大的数组，超过了虚拟机的限制。
7. Unable to create new native thread：这个错误发生在虚拟机无法创建更多的本机线程，通常由于操作系统限制引起。

## 栈上OOM

在《Java虚拟机规范》规定了：**如果虚拟机的栈内存允许动态扩展，当扩展栈容量无法申请到足够的内存时，将抛出OutOfMemoryError异常。**

在某些编程语言和运行时环境中，栈内存允许动态扩展，而不会固定在一个特定的大小。这种情况下，栈内存可以动态增加，以适应程序的需要。然而，这种实现在Java中并不常见，如我们常用的Hotspot虚拟机种，栈内存是有限且固定的，不能动态扩展。

所以，在HotSpot虚拟机中是不会出现因为栈空间不足而抛出OutOfMemoryError异常的情况的，只会发生StackOverflow。



## 真实排查过程

[✅OOM问题排查过程](https://www.yuque.com/hollis666/fo22bm/vdnaxh?view=doc_embed)
