# 典型回答
多态的概念比较简单，**就是同一操作作用于不同的对象，可以有不同的解释，产生不同的执行结果。**

如果按照这个概念来定义的话，那么多态应该是一种运行期的状态。为了实现运行期的多态，或者说是动态绑定，需要满足三个条件：

- 有类继承或者接口实现。
- 子类要重写父类的方法。
- 父类的引用指向子类的对象。

简单来一段代码解释下：

```java
public class Parent{

    public void call(){
        sout("im Parent");
    }
}
public class Son extends Parent{// 1.有类继承或者接口实现
    public void call(){// 2.子类要重写父类的方法
        sout("im Son");
    }
}
public class Daughter extends Parent{// 1.有类继承或者接口实现
    public void call(){// 2.子类要重写父类的方法
        sout("im Daughter");
    }
}
public class Test{

    public static void main(String[] args){
        Parent p = new Son(); //3.父类的引用指向子类的对象
        Parent p1 = new Daughter(); //3.父类的引用指向子类的对象
    }
}
```

这样，就实现了多态，同样是Parent类的实例，p.call 调用的是Son类的实现、p1.call调用的是Daughter的实现。

有人说，你自己定义的时候不就已经知道p是son，p1是Daughter了么。但是，有些时候你用到的对象并不都是自己声明的 。

比如Spring 中的IOC出来的对象，你在使用的时候就不知道他是谁，或者说你可以不用关心他是谁。根据具体情况而定。

如下面的payService就不是我们自己创建的，而是在运行期根据channel实时决策出来的。
```java
public class PayDomainService {
    @Autowired
    PayServiceFactory payServiceFactory;

    public void pay(PayRequest payRequest) {
        String payChannel = payRequest.getPayChannel();
        payServiceFactory.getPayService(payChannel).pay(payRequest);
    }
}
```

前面说多态是一种运行期的概念。还有一种说法，包括维基百科也说明，认为多态还分为动态多态和静态多态。

一般认为Java中的函数重载是一种静态多态，因为他需要在编译期决定具体调用哪个方法。关于这一点，不同的人有不同的见解，建议在面试中如果被问到，可以这样回答：

> “我认为，多态应该是一种运行期特性，Java中的重写是多态的体现。不过也有人提出重载是一种静态多态的想法，这个问题在StackOverflow等网站上有很多人讨论，但是并没有什么定论。我更加倾向于重载不是多态。”


这样沟通，既能体现出你了解的多，又能表现出你有自己的思维，不是那种别人说什么就是什么的。

# 扩展知识
### 方法的重载与重写

重载是就是函数或者方法有同样的名称，但是参数列表不相同的情形，这样的同名不同参数的函数或者方法之间，互相称之为重载函数或者方法。

```java
class HollisExample {
    // 方法重载 - 第一个方法
    public void display(int a) {
        System.out.println("Got Integer data.");
    }

    // 方法重载 - 第二个方法
    public void display(String b) {
        System.out.println("Got String data.");
    }
}

```

重写指的是在Java的子类与父类中有两个名称、参数列表都相同的方法的情况。由于他们具有相同的方法签名，所以子类中的新方法将覆盖父类中原有的方法。

```java
class Parent {
    // 父类的方法
    public void display() {
        System.out.println("Parent display()");
    }
}

class Child extends Parent {
    // 子类重写了父类的方法
    @Override
    public void display() {
        System.out.println("Child display()");
    }
}

public class Main {
    public static void main(String[] args) {
        Parent obj1 = new Parent();
        obj1.display();  // 输出 "Parent display()"

        Parent obj2 = new Child();
        obj2.display();  // 输出 "Child display()"
    }
}

```

#### 重载和重写的区别

1、重载是一个编译期概念、重写是一个运行期间概念。<br />2、重载遵循所谓“编译期绑定”，即在编译时根据参数变量的类型判断应该调用哪个方法。<br />3、重写遵循所谓“运行期绑定”，即在运行的时候，根据引用变量所指向的实际对象的类型来调用方法

