# 典型回答
## 什么是平台无关性
平台无关性就是一种语言在计算机上的运行不受平台的约束，一次编译，到处执行（Write Once ,Run Anywhere）。

也就是说，用Java创建的可执行二进制程序，能够不加改变的运行于多个平台。
## 平台无关性的实现
对于Java的平台无关性的支持，就像对安全性和网络移动性的支持一样，是分布在整个Java体系结构中的。其中扮演着重要的角色的有Java语言规范、Class文件、Java虚拟机（JVM）等。

在计算机世界中，计算机只认识0和1，所以，真正被计算机执行的其实是由0和1组成的二进制文件。

但是，我们日常开发使用的C、C++、Java、Python等都属于高级语言，而非二进制语言。所以，想要让计算机认识我们写出来的Java代码，那就需要把他”翻译”成由0和1组成的二进制文件。这个过程就叫做编译。负责这一过程的处理的工具叫做编译器。

在Java平台中，想要把Java文件，编译成二进制文件，需要经过两步编译，前端编译和后端编译：<br />![](https://cdn.nlark.com/yuque/0/2022/jpeg/719664/1669302406108-cd021301-825a-4050-a2e3-270d7ca864bb.jpeg#averageHue=%23fcfafa&clientId=u29139802-8a1c-4&from=paste&id=uf5d5bd09&originHeight=515&originWidth=819&originalType=url&ratio=1&rotation=0&showTitle=false&status=done&style=none&taskId=u81905392-5885-4b67-a1f4-6f5a7472f2b&title=)<br />前端编译主要指与源语言有关但与目标机无关的部分。Java中，我们所熟知的javac的编译就是前端编译。除了这种以外，我们使用的很多IDE，如eclipse，idea等，都内置了前端编译器。主要功能就是把.java代码转换成.class代码。

这里提到的.class代码，其实就是Class文件。

后端编译主要是将中间代码再翻译成机器语言。Java中，这一步骤就是Java虚拟机来执行的。

所以，我们说的，Java的平台无关性实现主要作用于以上阶段。如下图所示：

![](https://cdn.nlark.com/yuque/0/2022/jpeg/719664/1669302406113-07960b62-6ba8-46c9-90ec-37ae70fd8a5c.jpeg#averageHue=%23f2f1ed&clientId=u29139802-8a1c-4&from=paste&id=u713217ff&originHeight=474&originWidth=861&originalType=url&ratio=1&rotation=0&showTitle=false&status=done&style=none&taskId=uc688ab14-5bb2-40ef-babc-a91fc2c2019&title=)<br />我们从后往前介绍一下这三位主演：Java虚拟机、Class文件、Java语言规范
### Java虚拟机

所谓平台无关性，就是说要能够做到可以在多个平台上都能无缝对接。但是，对于不同的平台，硬件和操作系统肯定都是不一样的。

对于不同的硬件和操作系统，最主要的区别就是指令不同。比如同样执行a+b，A操作系统对应的二进制指令可能是10001000，而B操作系统对应的指令可能是11101110。那么，想要做到跨平台，最重要的就是可以根据对应的硬件和操作系统生成对应的二进制指令。

而这一工作，主要由我们的Java虚拟机完成。虽然Java语言是平台无关的，但是JVM却是平台有关的，不同的操作系统上面要安装对应的JVM。

![](https://cdn.nlark.com/yuque/0/2022/jpeg/719664/1669302406106-606d84f5-edc5-4a2b-8b86-152c3e2b214b.jpeg#averageHue=%23ecece9&clientId=u29139802-8a1c-4&from=paste&id=uba5ed6f8&originHeight=252&originWidth=547&originalType=url&ratio=1&rotation=0&showTitle=false&status=done&style=none&taskId=u35925178-5fc4-4d95-8b64-58cf4dbc1fc&title=)<br />上图是Oracle官网下载JDK的指引，不同的操作系统需要下载对应的Java虚拟机。

有了Java虚拟机，想要执行a+b操作，A操作系统上面的虚拟机就会把指令翻译成10001000，B操作系统上面的虚拟机就会把指令翻译成11101110。<br />![](https://cdn.nlark.com/yuque/0/2022/jpeg/719664/1669302406109-d44333b7-f708-41f4-8ff6-8272e2e84e3d.jpeg#averageHue=%2355fa7a&clientId=u29139802-8a1c-4&from=paste&id=uceb8d1af&originHeight=556&originWidth=1273&originalType=url&ratio=1&rotation=0&showTitle=false&status=done&style=none&taskId=ue77fd1d2-46c3-44a5-ac95-1ed834ad407&title=)ps：图中的Class文件中内容为mock内容

所以，Java之所以可以做到跨平台，是因为Java虚拟机充当了桥梁。他扮演了运行时Java程序与其下的硬件和操作系统之间的缓冲角色。我们可以理解为，Java的平台无关性，正是因为JVM的平台有关性

### 字节码
各种不同的平台的虚拟机都使用统一的程序存储格式——字节码（ByteCode）是构成平台无关性的另一个基石。Java虚拟机只与由字节码组成的Class文件进行交互。

我们说Java语言可以Write Once ,Run Anywhere。这里的Write其实指的就是生成Class文件的过程。

因为Java Class文件可以在任何平台创建，也可以被任何平台的Java虚拟机装载并执行，所以才有了Java的平台无关性。
### Java语言规范
已经有了统一的Class文件，以及可以在不同平台上将Class文件翻译成对应的二进制文件的Java虚拟机，Java就可以彻底实现跨平台了吗？

其实并不是的，Java语言在跨平台方面也是做了一些努力的，这些努力被定义在Java语言规范中。

比如，Java中基本数据类型的值域和行为都是由其自己定义的。而C/C++中，基本数据类型是由它的占位宽度决定的，占位宽度则是由所在平台决定的。所以，在不同的平台中，对于同一个C++程序的编译结果会出现不同的行为。

举一个简单的例子，对于int类型，在Java中，int占4个字节，这是固定的。

但是在C++中却不是固定的了。在16位计算机上，int类型的长度可能为两字节；在32位计算机上，可能为4字节；当64位计算机流行起来后，int类型的长度可能会达到8字节。（这里说的都是可能哦！）

![](https://cdn.nlark.com/yuque/0/2022/jpeg/719664/1669302406494-e9c713b9-4b4d-4743-a212-c3998fb6c5c7.jpeg#averageHue=%23fdfdfd&clientId=u29139802-8a1c-4&from=paste&id=u1c4e8291&originHeight=810&originWidth=1088&originalType=url&ratio=1&rotation=0&showTitle=false&status=done&style=none&taskId=uc1a31de4-0b34-45fc-a471-613adabb715&title=)<br />通过保证基本数据类型在所有平台的一致性，Java语言为平台无关性提供强了强有力的支持。
# 知识扩展

## 平台无关性好处

作为一门平台无关性语言，无论是在自身发展，还是对开发者的友好度上都是很突出的。

因为其平台无关性，所以Java程序可以运行在各种各样的设备上，尤其是一些嵌入式设备，如打印机、扫描仪、传真机等。随着5G时代的来临，也会有更多的终端接入网络，相信平台无关性的Java也能做出一些贡献。

同时，Java通过Swing，FX，可以对客户端进行编写，开发者可以通过Java编写一次，就可以运行到IOS或者Windows，Linux等OS中，也减轻了开发者的开发负担

对于Java开发者来说，Java减少了开发和部署到多个平台的成本和时间。真正的做到一次编译，到处运行。

## 有哪些语言实现了平台无关？

1. 所有基于JVM的语言都实现了平台无关，如Groovy、Scala、Jython等
2. 其他的有VM的语言也同样实现了平台无关，如C#
3. 脚本语言：JavaScript，Python，Php
## Java中基本数据类型的大小都是确定的吗？

非也非也，boolean类型的大小在不同的情况下是不确定的，依据JVM规范第2版：
> Although the Java virtual machine defines a boolean type, it only provides very limited support for it. There are no Java virtual machine instructions solely dedicated to operations on boolean values. Instead, expressions in the Java programming language that operate on boolean values are compiled to use values of the Java virtual machine int data type.
> The Java virtual machine does directly support boolean arrays. Its _newarray_ instruction enables creation of boolean arrays. Arrays of type boolean are accessed and modified using the byte array instructions _baload_ and _bastore_.[2](https://docs.oracle.com/javase/specs/jvms/se6/html/Overview.doc.html#24357)
> The Java virtual machine encodes boolean array components using _1_ to represent true and _0_ to represent false. Where Java programming language boolean values are mapped by compilers to values of Java virtual machine **type int**, the compilers must use the same encoding.

> 1 The first edition of The JavaTM Virtual Machine Specification did not consider boolean to be a Java virtual machine type. However, boolean values do have limited support in the Java virtual machine. This second edition clarifies the issue by treating boolean as a type.

> 2 In Sun's JDK releases 1.0 and 1.1, and the Java 2 SDK, Standard Edition, v1.2, boolean arrays in the Java programming language are encoded as Java virtual machine byte arrays, **using 8 bits per boolean element.**


简单看下来，如果是单个的布尔类型，长度为32bit，如果是布尔数组，则每个布尔值的长度为8bit。

## 打破平台无关性

[✅Java一定就是平台无关的吗？](https://www.yuque.com/hollis666/fo22bm/fgeranr7ts8m4iuy?view=doc_embed)
