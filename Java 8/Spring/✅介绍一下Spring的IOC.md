# 典型回答

所谓的IOC（inversion of control），就是控制反转的意思。何为控制反转？

在传统的程序设计中，应用程序代码通常控制着对象的创建和管理。例如，一个对象需要依赖其他对象，那么它会直接new出来对象。这样的设计通常被称为 "控制流程"。

而在IOC 中，控制关系发生了反转。控制权被转移到Spring容器中，容器负责创建和管理对象，并在需要的时候将它们注入到应用程序中。

**所以，原来这个对象的控制权在我们的代码中，我们自己new的对象，在Spring中，应用程序不再控制对象的创建，而是被动地接受由容器注入的对象。**

我们拿代码来举个例子：

下面是一个没有IOC的例子
```java
class A {}
class B {

    // B需要将A的实例new出来，也就是我们说的控制
    private A a = new A();

    public void use() {
        System.out.print(a);
    }
        
}
```

当有了IOC之后
```java
@Component // 说明A自己控制自己，把自己初始化出来，注入给了容器
class A {}

class B {

    // B不需要控制a，直接使用。如果A没有把自己注入给容器，B就不能使用
    @Resource
    private A a;

    public void use() {
        System.out.print(a);
    }
        
}
```

也就是说，**没有Spring的话，我们要使用的对象，需要我们自己创建，而有了Spring的IOC之后，对象由IOC容器创建并管理，我们只需要在想要使用的时候从容器中获取就行了。**

值得说明的是，IOC只是一种思想和理念，可以有不同的实现方式。
## IOC的优点

使用IOC，有最少三个好处：

1. **使用者不用关心引用bean的实现细节**，譬如对于`A a = new A(c,d,e,f);`来说，如果要使用A，那还要把c，d，e，f多个类全都感知一遍，这显然是非常麻烦且不合理的

2. **不用创建多个相同的bean导致浪费**，仍然是：
```java
A b = new A();
A z = new A();
```
如果B和Z都引用了A，那么B和Z就可能new 两个A实例，实际上，我们只需要一个就好了。

3. **Bean的修改使用方无需感知。**同样是上面的例子，假如说BeanA需要修改，如果没有IOC的话，所有引用到A的其他bean都需要感知这个逻辑，并且做对应的修改。但是如果使用了IOC，其他bean就完全不用感知到
## Spring的IOC
对于Spring的IOC来说，它是IOC思想的一种实现方式。在容器启动的时候，它会根据每个bean的要求，将bean注入到SpringContainer中。如果有其他bean需要使用，就直接从容器中获取即可，如下图所示：

![image.png](https://cdn.nlark.com/yuque/0/2023/png/719664/1673670476475-0a539106-38c7-4b14-a1eb-ec27d0888405.png#averageHue=%23f7f6f5&clientId=u3e675a2c-a7e5-4&from=paste&height=633&id=u4c03af49&originHeight=633&originWidth=1017&originalType=binary&ratio=1&rotation=0&showTitle=false&size=54316&status=done&style=none&taskId=u990106a3-9b95-48e4-bd16-325248b8ca2&title=&width=1017)
# 扩展知识
## IOC是如何实现的？
使用Spring的IOC容器能力，非常简单，如下代码所示：
```java
ApplicationContext context= new AnnotationConfigApplicationContext("cn.wxxlamp.spring.ioc");
Bean bean = context.getBean(Bean.class);
bean.use();
```
从上面的代码中，我们也能看出来Spring的IOC是如何实现的：

1. 从配置元数据中获取要DI的业务POJO（这里的配置元数据包括xml，注解，configuration类等）
2. 将业务POJO形成BeanDefinition注入到Spring Container中
3. 使用方通过ApplicationContext从Spring Container直接获取即可。如下图所示：

![image.png](https://cdn.nlark.com/yuque/0/2023/png/719664/1673667342516-9f823f13-c1fe-41e2-85e1-f0764ed7cbbe.png#averageHue=%23f7f7f7&clientId=u3e675a2c-a7e5-4&from=paste&height=296&id=u74f2bacf&originHeight=296&originWidth=498&originalType=binary&ratio=1&rotation=0&showTitle=false&size=12367&status=done&style=none&taskId=u12697231-953a-44c5-9328-a6a48f12743&title=&width=498)


