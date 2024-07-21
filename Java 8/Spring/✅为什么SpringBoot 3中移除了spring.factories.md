# 典型回答

在SpringBoot 2.7中，官方已经明确的说了，使用spring.factories这种自动配置的方式已经过时了，并且将在SpringBoot 3.0中彻底移除。

![image.png](https://cdn.nlark.com/yuque/0/2024/png/5378072/1708752644884-43731084-6c6b-4bdc-8090-4182c4e239cc.png#averageHue=%23e4c698&clientId=u2c76b1f8-4bd8-4&from=paste&height=763&id=uc6e928d7&originHeight=763&originWidth=1901&originalType=binary&ratio=1&rotation=0&showTitle=false&size=177300&status=done&style=none&taskId=u528b65a7-6d2c-46ba-b691-7bc0725efea&title=&width=1901)

![image.png](https://cdn.nlark.com/yuque/0/2024/png/5378072/1708752609452-32240507-454f-459d-8f8d-6f441ed78858.png#averageHue=%23ecd0a4&clientId=u2c76b1f8-4bd8-4&from=paste&height=628&id=u990d9441&originHeight=628&originWidth=1655&originalType=binary&ratio=1&rotation=0&showTitle=false&size=109547&status=done&style=none&taskId=u31b90346-9db3-4c29-955a-1b36082394b&title=&width=1655)


官方文档以及网上有很多资料也提到了，可以使用`org.springframework.boot.autoconfigure.AutoConfiguration.imports`文件替代。具体参考：

[✅如何自定义一个starter？](https://www.yuque.com/hollis666/fo22bm/sn0vo662fz3r7aux?view=doc_embed)

**但是我并没有找到有人说为啥要这么做！！！**

但是Spring这么做肯定不是无缘无故的吧，总要有理由啊。虽然官方没说，网上也没人写，那我就尝试着分析一下。

在Spring开始提出要废弃spring.factories文件的时候，有且仅有这样一句话：

> Using spring.factories to find auto-configuration classes is causing problems with our native work. We initially used it because the code was already available, but we want to offer an alternative in 2.7 and stop using spring.factories for auto-configuration in 3.0


这句话我很早就看到了，但是一直没明白是啥意思，但是当SpringBoot 3.0提出来之后我知道了，一切都是为了云原生！（也就是上面这段英文中提到的native，我以前以为他是"本地"的意思，就一直不理解。原来是cloud native中的native）

下面这篇文章中我介绍过SpringBoot 3.0中新增了很多对云原生的支持。

[✅Spring 6.0和SpringBoot 3.0有什么新特性？](https://www.yuque.com/hollis666/fo22bm/gvwpq6q0h4ixd9g1?view=doc_embed)

我们知道，云原生时代中，最重要的是什么？启动速度。那Java如何提升的启动速度？AOT编译+云原生镜像

所以，SpringBoot 3.0中支持基于 GraalVM 将 Spring 应用程序编译成原生镜像。重点是编译，所以这个动作需要在编译期进行。

可是，传统方法使用spring.factories依赖于**运行时**扫描和加载自动配置类，这这么做效率肯定不高。不适合云原生！

相比之下，使用`org.springframework.boot.autoconfigure.AutoConfiguration.imports`这种方式，允许在编译时确定自动配置类，减少了运行时开销，并使得像GraalVM这样的工具更容易分析和编译Spring Boot应用到原生映像。这种方法可以带来更快的启动时间和更低的内存消耗，这对于从可扩展性和效率受益的云原生应用至关重要。

**同样是配置文件，为啥它能在编译期就确定自动配置类，而spring.factories要到运行期才行？**

这是因为Spring 2.7之后，不仅新增了这种配置方式，还提供了一个新的注解，就是这个@AutoConfiguration 注解。

![image.png](https://cdn.nlark.com/yuque/0/2024/png/5378072/1708754895534-1c7558a5-c914-4d5f-8fdd-a6aa34a4f42b.png#averageHue=%23f1f0f0&clientId=u2c76b1f8-4bd8-4&from=paste&height=804&id=u78c8e46d&originHeight=804&originWidth=1530&originalType=binary&ratio=1&rotation=0&showTitle=false&size=102414&status=done&style=none&taskId=u5bad2607-652a-4564-8a93-877c99983fc&title=&width=1530)


`org.springframework.boot.autoconfigure.AutoConfiguration.imports`的配置方式，能够在编译时确定自动配置类的原理主要基于**Java编译期的注解处理器**。

> 在Java中，处理注解主要有以下几种方式：
> 1、运行时反射是最常见的注解处理方式，通过Java反射API在运行时访问注解信息。这种方法允许程序动态地查询注解和注解的属性，从而根据注解执行特定的逻辑。这种方式广泛用于框架开发中，如Spring的依赖注入、Hibernate的ORM映射等。
> 2、APT（Annotation Processing Tool）允许在编译时处理注解。通过创建实现Processor接口的注解处理器，开发者可以在源代码编译阶段读取注解信息，并根据注解生成新的源代码或资源文件。用于生成额外的源代码，如Lombok库自动生成getter和setter方法，或用于编译时的代码检查。


而SpringBoot的这种新的自动配置方式，就是借助的APT的方式，它允许在编译时检查、处理注解，并且可以生成新的源代码、资源文件或其他文件。

所以，我们在`org.springframework.boot.autoconfigure.AutoConfiguration.imports`中定义一个自动配置类，如XxlJobConfiguration，这就相当于在XxlJobConfiguration类上增加了`AutoConfiguration`注解。

那么有了注解、有了APT，就可以实现在编译期确定自动化配置类的目的了。




