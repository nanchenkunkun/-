# 典型回答

[✅介绍一下Spring的AOP](https://www.yuque.com/hollis666/fo22bm/nget4r5wl2imegi7?view=doc_embed)

**首先，Spring的AOP其实是通过动态代理实现的，所以，想要让AOP生效，前提必须是动态代理生效，并且可以调用到代理对象的方法。**

什么情况下会不走代理对象的调用呢？

首先就是类内部的调用，比如一些私有方法调用，内部类调用，以及同一个类中方法的自调用等。如：

```java
//1
public class MyService {
    public void doSomething() {
        doInternal(); // 自调用方法
    }

    public void doInternal() {
        System.out.println("Doing internal work...");
    }
}

//2
public class MyService {
    public void doSomething() {
        doInternal(); // 自调用私有方法
    }

    private void doInternal() {
        System.out.println("Doing internal work...");
    }
}


//3
public class OuterClass {
    private class InnerClass {
        public void doSomething() {
            System.out.println("Doing something in inner class...");
        }
    }

    public void invokeInnerClassMethod() {
        InnerClass innerClass = new InnerClass();
        innerClass.doSomething(); // 调用内部类方法
    }
}
```

以上，都是因为在对象内部直接调用其他方法，就会用原始对象直接调用了，不会调用到代理对象，所以代理会失效。

**类似的还有一种情况，虽然不是对象的自调用，但是他也是因为没有调用到代理对象，那就是调用static方法，因为这类方法是属于这个类的，并不是对象的，所以无法被AOP。**

```java
public class MyService {
    public staic void doSomething() {
        // static 方法
    }
}
```

**还有一种方法，也无法被代理，那就是final方法，由于AOP是通过创建代理对象来实现的，而无法对final方法进行子类化和覆盖，所以无法拦截这些方法。**

```java
public class MyService {
    public final void doSomethingFinal() {
        System.out.println("Doing something final...");
    }
}
```


所以，那么总结一下就是以下几种情况，会导致代理失效，AOP不起作用：

1、私有方法调用<br />2、静态方法调用<br />3、final方法调用<br />4、类内部自调用<br />5、内部类方法调用
