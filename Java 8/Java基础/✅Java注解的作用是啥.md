# 典型回答
Java 注解用于为 Java 代码提供元数据。作为元数据，注解不直接影响你的代码执行，但也有一些类型的注解实际上可以用于这一目的。Java 注解是从 Java5 开始添加到 Java 的。

Java的注解，可以说是一种标识，标识一个类或者一个字段，常常是和反射，AOP结合起来使用。中间件一般会定义注解，如果某些类或字段符合条件，就执行某些能力。

[✅使用自定义注解+切面减少冗余代码，提升代码的鲁棒性](https://www.yuque.com/hollis666/fo22bm/kfu24zmltkpx2bd3?view=doc_embed)
# 扩展知识
## 什么是元注解
说简单点，就是 定义其他注解的注解 。<br />比如Override这个注解，就不是一个元注解。而是通过元注解定义出来的。
```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface Override {
}
```
这里面的@Target，@Retention就是元注解。<br />元注解有四个:@Target（表示该注解可以用于什么地方）、@Retention（表示在什么级别保存该注解信息）、@Documented（将此注解包含在javadoc中）、@Inherited（允许子类继承父类中的注解）。

一般@Target是被用的最多的。
### @Retention

指定被修饰的注解的生命周期，即注解在源代码、编译时还是运行时保留。它有三个可选的枚举值：SOURCE、CLASS和RUNTIME。默认为CLASS。

```
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface MyRuntimeAnnotation {
    // some elements and values
}

```

### @Target
指定被修饰的注解可以应用于的元素类型，如类、方法、字段等。这样可以限制注解的使用范围，避免错误使用。

```
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

@Target({ElementType.TYPE, ElementType.METHOD})
public @interface MyTargetAnnotation {
    // some elements and values
}

```

### @Documented

用于指示注解是否会出现在生成的Java文档中。如果一个注解被@Documented元注解修饰，则该注解的信息会出现在API文档中，方便开发者查阅。

```
import java.lang.annotation.Documented;

@Documented
public @interface MyDocumentedAnnotation {
    // some elements and values
}

```

### @Inherited

指示被该注解修饰的注解是否可以被继承。默认情况下，注解不会被继承，即子类不会继承父类的注解。但如果将一个注解用@Inherited修饰，那么它就可以被子类继承。

```
import java.lang.annotation.Inherited;

@Inherited
public @interface MyInheritedAnnotation {
    // some elements and values
}

```

