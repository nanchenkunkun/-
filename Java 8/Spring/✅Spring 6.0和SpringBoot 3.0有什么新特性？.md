Spring在2022年相继推出了Spring Framework 6.0和SpringBoot 3.0，Spring把这次升级称之为新一代框架的开始，下一个10年的新开端

主要更新内容是以下几个：

-  A Java 17 baseline 
-  Support for Jakarta EE 10 with an EE 9 baseline 
-  Support for generating native images with GraalVM, superseding the experimental Spring Native project 
-  Ahead-Of-Time transformations and the corresponding AOT processing support for Spring application contexts 

首先，前两个比较容易理解，主要说的是依赖的服务的版本升级的信息，那就是Spring Framework 6.0和SpringBoot 3.0都要求JDK的版本最低也得是JDK 17；并且底层依赖的J2EE也迁移到了Jakarta EE 9。

虽然JDK 17有很多新的特性，并且也是目前最新的一个LTS版本。

但是其实真正的使用比较多的版本还是JDK 1.8，而Spring彻底抛弃17之前的所有版本！！！

## AOT编译

Ahead-Of-Time，即预先编译，这是相对于我们熟知的Just-In-Time（JIT，即时编译）来说的。

相比于JIT编译，AOT指的是在程序运行前编译，这样就可以避免在运行时的编译性能消耗和内存消耗，可以在程序运行初期就达到最高性能、也可以显著的加快程序的启动。

AOT的引入，意味着Spring生态正式引入了提前编译技术，相比于JIT编译，AOT有助于优化Spring框架启动慢、占用内存多、以及垃圾无法被回收等问题。

## Spring Native

在Spring的新版本中引入了Spring Native。

有了Spring Native ，Spring可以不再依赖Java虚拟机，而是基于 GraalVM 将 Spring 应用程序编译成原生镜像（native image），提供了一种新的方式来部署 Spring 应用。这种部署Spring的方式是云原生友好的。

SpringNative的优点是编译出来的原生 Spring 应用可以作为一个独立的可执行文件进行部署，而不需要安装JVM，而且启动时间非常短、并且有更少的资源消耗。他的缺点就是构建时长要比JVM更长一些。

