# 典型回答
Java 中区分 API 和 SPI，通俗的讲：API 和 SPI 都是相对的概念，他们的差别只在语义上，API 直接被应用开发人员使用，SPI 被框架扩展人员使用。

API Application Programming Interface

API是一组定义了软件组件之间交互的规则和约定的接口。提供方来制定接口并完成对接口的不同实现，调用方只需要调用即可。

SPI Service Provider Interface

SPI是一种扩展机制，通常用于在应用程序中提供可插拔的实现。 调用方可选择使用提供方提供的内置实现，也可以自己实现。

请记住这句话：**API用于定义调用接口，而SPI用于定义和提供可插拔的实现方式。**

所以说，API 是面向普通开发者的，提供一组功能，使他们可以利用一个库或框架来实现具体的功能。而SPI 是面向那些希望扩展或定制基础服务的开发者的，它定义了一种机制，让其他开发者可以提供新的实现或扩展现有的功能。
# 知识扩展
## 如何定义一个SPI
步骤1、定义一组接口 (假设是org.foo.demo.IShout)，并写出接口的一个或多个实现，(假设是org.foo.demo.animal.Dog、org.foo.demo.animal.Cat)。
```java
public interface IShout {
    void shout();
}
public class Cat implements IShout {
    @Override
    public void shout() {
    	System.out.println("miao miao");
	}
}
public class Dog implements IShout {
    @Override
    public void shout() {
    	System.out.println("wang wang");
    }
}
```
步骤2、在 src/main/resources/ 下建立 /META-INF/services 目录， 新增一个以接口命名的文件 (org.foo.demo.IShout文件)，内容是要应用的实现类（这里是org.foo.demo.animal.Dog和org.foo.demo.animal.Cat，每行一个类）。<br />org.foo.demo.animal.Dog<br />org.foo.demo.animal.Cat<br />步骤3、使用 ServiceLoader 来加载配置文件中指定的实现。
```java
public class SPIMain {
    public static void main(String[] args) {
        ServiceLoader<IShout> shouts = ServiceLoader.load(IShout.class);
        for (IShout s : shouts) {
        	s.shout();
        }
    }
}
```
代码输出：<br />wang wang<br />miao miao
## SPI的实现原理
看ServiceLoader类的签名类的成员变量：
```java
public final class ServiceLoader<S> implements Iterable<S>{
    private static final String PREFIX = "META-INF/services/";
    // 代表被加载的类或者接口
    private final Class<S> service;
    // 用于定位，加载和实例化providers的类加载器
    private final ClassLoader loader;
    // 创建ServiceLoader时采用的访问控制上下文
    private final AccessControlContext acc;
    // 缓存providers，按实例化的顺序排列
    private LinkedHashMap<String,S> providers = new LinkedHashMap<>();
    // 懒查找迭代器
    private LazyIterator lookupIterator;
    ......
}
```
参考具体源码，梳理了一下，实现的流程如下：

1. 应用程序调用ServiceLoader.load方法，ServiceLoader.load方法内先创建一个新的ServiceLoader，并实例化该类中的成员变量，包括：
   1. loader(ClassLoader类型，类加载器)
   2. acc(AccessControlContext类型，访问控制器)
   3. providers(LinkedHashMap类型，用于缓存加载成功的类)
   4. lookupIterator(实现迭代器功能)
2. 应用程序通过迭代器接口获取对象实例，
   1. ServiceLoader先判断成员变量providers对象中(LinkedHashMap类型)是否有缓存实例对象，如果有缓存，直接返回。
   2. 如果没有缓存，执行类的装载：
      1. 读取META-INF/services/下的配置文件，获得所有能被实例化的类的名称
      2. 通过反射方法Class.forName()加载类对象，并用instance()方法将类实例化
      3. 把实例化后的类缓存到providers对象中(LinkedHashMap类型）
      4. 然后返回实例对象。
## SPI的应用场景
概括地说，适用于：调用者根据实际使用需要，启用、扩展、或者替换框架的实现策略。比较常见的例子：

1. 数据库驱动加载接口实现类的加载
2. JDBC加载不同类型数据库的驱动
3. 日志门面接口实现类加载
4. SLF4J加载不同提供商的日志实现类

**Spring**<br />Spring中大量使用了SPI,比如：对servlet3.0规范对ServletContainerInitializer的实现、自动类型转换Type Conversion SPI(Converter SPI、Formatter SPI)等

**Dubbo**<br />Dubbo中也大量使用SPI的方式实现框架的扩展, 不过它对Java提供的原生SPI做了封装，允许用户扩展实现Filter接口
