# 典型回答

这个问题，要么面试官是想问你反射，要么就是在给你挖坑！

因为，**在Java中，String 类型确实是不可变的。这意味着一旦一个 String 对象被创建，其内容就不能被改变。任何看似修改了 String 值的操作实际上都是创建了一个新的 String 对象。**

[✅String为什么设计成不可变的？](https://www.yuque.com/hollis666/fo22bm/hhkgh2nsrlnf2g0g?view=doc_embed)

当然，如果不考虑这个可不可变的问题，新建一个也算改了的话。那么就有以下几种方式：

1、在Java中，`private` 访问修饰符限制了只有类本身可以访问和修改其成员变量。如果需要在类的外部修改一个 `private` 修饰的 `String` 参数，通常有几种方法：

### 1. 使用 Setter 方法

这是最常用且最符合对象导向设计原则的方法。在类内部提供一个公开的 `setter` 方法来修改 `private` 变量的值。

```java
public class MyClass {
    private String myString;

    public void setMyString(String value) {
        this.myString = value;
    }
}

// 使用
MyClass obj = new MyClass();
obj.setMyString("new value");
```

### 2. 使用反射

如果没有 `setter` 方法可用，可以使用反射。这种方法可以突破正常的访问控制规则，但应谨慎使用，因为它破坏了封装性，增加了代码的复杂性和出错的可能性。并且性能并不好。

[✅什么是反射机制？为什么反射慢？](https://www.yuque.com/hollis666/fo22bm/sr19rp?view=doc_embed)

```java
import java.lang.reflect.Field;

public class MyClass {
    private String myString = "initial value";
}

// 使用反射修改
MyClass obj = new MyClass();
try {
    Field field = MyClass.class.getDeclaredField("myString");
    field.setAccessible(true); // 使得private字段可访问
    field.set(obj, "new value");
} catch (NoSuchFieldException | IllegalAccessException e) {
    e.printStackTrace();
}
```



