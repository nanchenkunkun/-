# 典型回答

因为如果要实现多继承，就会像C++中一样，存在**菱形继承**的问题，C++为了解决菱形继承问题，又引入了**虚继承**。因为支持多继承，引入了菱形继承问题，又因为要解决菱形继承问题，引入了虚继承。而经过分析，人们发现我们其实真正想要使用多继承的情况并不多。所以，在 Java 中，不允许“多继承”，即一个类不允许继承多个父类。

除了菱形的问题，支持多继承复杂度也会增加。一个类继承了多个父类，可能会继承大量的属性和方法，导致类的接口变得庞大、难以理解和维护。此外，在修改一个父类时，可能会影响到多个子类，增加了代码的耦合度。

在Java 8以前，接口中是不能有方法的实现的。所以一个类同时实现多个接口的话，也不会出现C++中的歧义问题。因为所有方法都没有方法体，真正的实现还是在子类中的。但是，Java 8中支持了默认函数（default method ），即接口中可以定义一个有方法体的方法了。

而又因为Java支持同时实现多个接口，这就相当于通过implements就可以从多个接口中继承到多个方法了，但是，Java8中为了避免菱形继承的问题，在实现的多个接口中如果有相同方法，就会要求该类必须重写这个方法。

# 扩展知识

## 菱形继承问题
Java的创始人James Gosling曾经回答过，他表示：

“Java之所以不支持一个类继承多个类，主要是因为在设计之初我们听取了来自C++和Objective-C等阵营的人的意见。因为多继承会产生很多歧义问题。”

Gosling老人家提到的歧义问题，其实是C++因为支持多继承之后带来的菱形继承问题。

假设我们有类B和类C，它们都继承了相同的类A。另外我们还有类D，类D通过多重继承机制继承了类B和类C。<br />![](https://cdn.nlark.com/yuque/0/2022/jpeg/5378072/1672211742898-80096c34-a056-47fc-bf8b-0f45c4a64498.jpeg#averageHue=%23f3f3f3&clientId=u196b017d-a914-4&from=paste&id=u79a68a72&originHeight=270&originWidth=180&originalType=url&ratio=1&rotation=0&showTitle=false&status=done&style=none&taskId=u78a4e844-6371-4f88-9b8a-7c695887f4b&title=)<br />这时候，因为D同时继承了B和C，并且B和C又同时继承了A，那么，D中就会因为多重继承，继承到两份来自A中的属性和方法。

这时候，在使用D的时候，如果想要调用一个定义在A中的方法时，就会出现歧义。

因为这样的继承关系的形状类似于菱形，因此这个问题被形象地称为菱形继承问题。

而C++为了解决菱形继承问题，又引入了**虚继承**。

因为支持多继承，引入了菱形继承问题，又因为要解决菱形继承问题，引入了虚继承。而经过分析，人们发现我们其实真正想要使用多继承的情况并不多。

所以，在 Java 中，不允许“声明多继承”，即一个类不允许继承多个父类。但是 Java 允许“实现多继承”，即一个类可以实现多个接口，一个接口也可以继承多个父接口。由于接口只允许有方法声明而不允许有方法实现（Java 8之前），这就避免了 C++ 中多继承的歧义问题。

## Java 8中的多继承

Java不支持多继承，但是是支持多实现的，也就是说，同一个类可以同时实现多个接口。

我们知道，在Java 8以前，接口中是不能有方法的实现的。所以一个类同时实现多个接口的话，也不会出现C++中的歧义问题。因为所有方法都没有方法体，真正的实现还是在子类中的。

那么问题来了。

Java 8中支持了默认函数（default method ），即接口中可以定义一个有方法体的方法了。

```
public interface Pet {

    public default void eat(){
        System.out.println("Pet Is Eating");
    }
}
```

而又因为Java支持同时实现多个接口，这就相当于通过implements就可以从多个接口中继承到多个方法了，这不就是变相支持了多继承么。<br />那么，Java是怎么解决菱形继承问题的呢？我们再定义一个哺乳动物接口，也定义一个eat方法。

```
public interface Mammal {

    public default void eat(){
        System.out.println("Mammal Is Eating");
    }
}
```

然后定义一个Cat，让他分别实现两个接口：
```
public class Cat implements Pet,Mammal {

}
```
这时候，编译期会报错：

> error: class Cat inherits unrelated defaults for eat() from types Mammal and Pet


这时候，就要求Cat类中，必须重写eat()方法。
```
public class Cat implements Pet,Mammal {
    @Override
    public void eat() {
        System.out.println("Cat Is Eating");
    }
}
```

所以可以看到，Java并没有帮我们解决多继承的歧义问题，而是把这个问题留给开发人员，通过重写方法的方式自己解决。
