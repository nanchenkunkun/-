反射之中包含了一个「反」字，所以想要解释反射就必须先从「正」开始解释。

一般情况下，我们使用某个类时必定知道它是什么类，是用来做什么的。于是我们直接对这个类进行实例化，之后使用这个类对象进行操作。

```java
Apple apple = new Apple(); //直接初始化，「正射」
apple.setPrice(4);
```

上面这样子进行类对象的初始化，我们可以理解为「正」。

而反射则是一开始并不知道我要初始化的类对象是什么，自然也无法使用 new 关键字来创建对象了。

这时候，我们使用 JDK 提供的反射 API 进行反射调用：

```java
Class clz = Class.forName("com.chenshuyi.reflect.Apple");
Method method = clz.getMethod("setPrice", int.class);
Constructor constructor = clz.getConstructor();
Object object = constructor.newInstance();
method.invoke(object, 4);
```

上面两段代码的执行结果，其实是完全一样的。但是其思路完全不一样，第一段代码在未运行时就已经确定了要运行的类（Apple），而第二段代码则是在运行时通过字符串值才得知要运行的类（com.chenshuyi.reflect.Apple）。

所以说什么是反射？

*反射就是在运行时才知道要操作的类是什么，并且可以在运行时获取类的完整构造，并调用对应的方法。*

**一个简单的例子**

上面提到的示例程序，其完整的程序代码如下：

```java
public class Apple {

    private int price;

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public static void main(String[] args) throws Exception{
        //正常的调用
        Apple apple = new Apple();
        apple.setPrice(5);
        System.out.println("Apple Price:" + apple.getPrice());
        //使用反射调用
        Class clz = Class.forName("com.chenshuyi.api.Apple");
        Method setPriceMethod = clz.getMethod("setPrice", int.class);
        Constructor appleConstructor = clz.getConstructor();
        Object appleObj = appleConstructor.newInstance();
        setPriceMethod.invoke(appleObj, 14);
        Method getPriceMethod = clz.getMethod("getPrice");
        System.out.println("Apple Price:" + getPriceMethod.invoke(appleObj));
    }
}
```

从代码中可以看到我们使用反射调用了 setPrice 方法，并传递了 14 的值。之后使用反射调用了 getPrice 方法，输出其价格。上面的代码整个的输出结果是：

```java
Apple Price:5
Apple Price:14
```

从这个简单的例子可以看出，一般情况下我们使用反射获取一个对象的步骤：

- 获取类的 Class 对象实例

```java
Class clz = Class.forName("com.zhenai.api.Apple");
```

- 根据 Class 对象实例获取 Constructor 对象

```java
Constructor appleConstructor = clz.getConstructor();
```

- 使用 Constructor 对象的 newInstance 方法获取反射类对象

```java
Object appleObj = appleConstructor.newInstance();
```

而如果要调用某一个方法，则需要经过下面的步骤：

- 获取方法的 Method 对象

```java
Method setPriceMethod = clz.getMethod("setPrice", int.class);
```

- 利用 invoke 方法调用方法

```java
setPriceMethod.invoke(appleObj, 14);
```

到这里，我们已经能够掌握反射的基本使用。但如果要进一步掌握反射，还需要对反射的常用 API 有更深入的理解。

在 JDK 中，反射相关的 API 可以分为下面几个方面：获取反射的 Class 对象、通过反射创建类对象、通过反射获取类属性方法及构造器。

**反射常用API**

## 获取反射中的Class对象

在反射中，要获取一个类或调用一个类的方法，我们首先需要获取到该类的 Class 对象。

在 Java API 中，获取 Class 类对象有三种方法：

*第一种，使用 Class.forName 静态方法。*当你知道该类的全路径名时，你可以使用该方法获取 Class 类对象。

```java
Class clz = Class.forName("java.lang.String");
```

*第二种，使用 .class 方法。*

这种方法只适合在编译前就知道操作的 Class。

```java
Class clz = String.class;
```

*第三种，使用类对象的 getClass() 方法。*

```java
String str = new String("Hello");
Class clz = str.getClass();
```

## 通过反射创建类对象

通过反射创建类对象主要有两种方式：通过 Class 对象的 newInstance() 方法、通过 Constructor 对象的 newInstance() 方法。

第一种：通过 Class 对象的 newInstance() 方法。

```java
Class clz = Apple.class;
Apple apple = (Apple)clz.newInstance();
```

第二种：通过 Constructor 对象的 newInstance() 方法

```java
Class clz = Apple.class;
Constructor constructor = clz.getConstructor();
Apple apple = (Apple)constructor.newInstance();
```

通过 Constructor 对象创建类对象可以选择特定构造方法，而通过 Class 对象则只能使用默认的无参数构造方法。下面的代码就调用了一个有参数的构造方法进行了类对象的初始化。

```java
Class clz = Apple.class;
Constructor constructor = clz.getConstructor(String.class, int.class);
Apple apple = (Apple)constructor.newInstance("红富士", 15);
```

## 通过反射获取类属性、方法、构造器

我们通过 Class 对象的 getFields() 方法可以获取 Class 类的属性，但无法获取私有属性。

```java
Class clz = Apple.class;
Field[] fields = clz.getFields();
for (Field field : fields) {
    System.out.println(field.getName());
}
```

输出结果是：

```java
price
```

而如果使用 Class 对象的 getDeclaredFields() 方法则可以获取包括私有属性在内的所有属性：

```java
Class clz = Apple.class;
Field[] fields = clz.getDeclaredFields();
for (Field field : fields) {
    System.out.println(field.getName());
}
```

输出结果是：

```java
name
price
```

与获取类属性一样，当我们去获取类方法、类构造器时，如果要获取私有方法或私有构造器，则必须使用有 declared 关键字的方法。

**反射源码解析**

当我们懂得了如何使用反射后，今天我们就来看看 JDK 源码中是如何实现反射的。或许大家平时没有使用过反射，但是在开发 Web 项目的时候会遇到过下面的异常：

```text
java.lang.NullPointerException 
...
sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
  at java.lang.reflect.Method.invoke(Method.java:497)
```

可以看到异常堆栈指出了异常在 Method 的第 497 的 invoke 方法中，其实这里指的 invoke 方法就是我们反射调用方法中的 invoke。

```text
Method method = clz.getMethod("setPrice", int.class); 
method.invoke(object, 4);   //就是这里的invoke方法
```

例如我们经常使用的 Spring 配置中，经常会有相关 Bean 的配置：

```text
<bean class="com.chenshuyi.Apple">
</bean>
```

当我们在 XML 文件中配置了上面这段配置之后，Spring 便会在启动的时候利用反射去加载对应的 Apple 类。而当 Apple 类不存在或发生启发异常时，异常堆栈便会将异常指向调用的 invoke 方法。



