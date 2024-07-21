# 典型回答

<? extends T> 表示类型的上界，表示参数化类型的可能是T 或是 T的子类

```java
// 定义一个泛型方法，接受任何继承自Number的类型
public <T extends Number> void processNumber(T number) {
    // 在这个方法中，可以安全地调用Number的方法
    double value = number.doubleValue();
    // 其他操作...
}
```

<? super T> 表示类型下界（Java Core中叫超类型限定），表示参数化类型是此类型的超类型（父类型），直至Object

```java
// 定义一个泛型方法，接受任何类型的List，并向其中添加元素
public <T> void addElements(List<? super T> list, T element) {
    list.add(element);
    // 其他操作...
}
```

在使用 限定通配符的时候，需要遵守**PECS原则**，即Producer Extends, Consumer Super；上界生产，下界消费。

如果要从集合中读取类型T的数据，并且不能写入，可以使用 ? extends 通配符；(Producer Extends)，如上面的processNumber方法。<br />如果要从集合中写入类型T的数据，并且不需要读取，可以使用 ? super 通配符；(Consumer Super)，如上面的addElements方法

> extend的时候是可读取不可写入，那为什么叫上界生产呢？
> 因为这个消费者/生产者描述的<集合>，当我们从集合读取的时候，集合是生产者。


如果既要存又要取，那么就不要使用任何通配符。
