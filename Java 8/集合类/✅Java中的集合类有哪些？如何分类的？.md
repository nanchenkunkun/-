# 典型回答
Java的整个集合框架中，主要分为List，Set，Queue，Stack，Map等五种数据结构。其中，前四种数据结构都是单一元素的集合，而最后的Map则是以KV对的形式使用。

从继承关系上讲，List，Set，Queue都是Collection的子接口，Collection又继承了Iterable接口，说明这几种集合都是可以遍历的。

![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1693116712014-5a1db396-f4e1-4d57-b2fd-33ae8f0410d7.png#averageHue=%23fcfcfc&clientId=u70b6e827-f4cc-4&from=paste&height=612&id=u54ca1825&originHeight=612&originWidth=1334&originalType=binary&ratio=1&rotation=0&showTitle=false&size=258000&status=done&style=none&taskId=u0d0dafaa-8789-40da-a07e-fea25a9d047&title=&width=1334)

从功能上讲，List代表一个容器，可以是先进先出，也可以是先进后出。而Set相对于List来说，是无序的，同时也是一个去重的列表，既然会去重，就一定会通过equals，compareTo，hashCode等方法进行比较。Map则是KV的映射，也会涉及到Key值的查询等能力。

从实现上讲，List可以有链表实现或者数组实现，两者各有优劣，链表增删快，数组查询快。Queue则可以分为优先队列，双端队列等等。Map则可以分为普通的HashMap和可以排序的TreeMap等等。
# 知识扩展
## **Collection和Collections有什么区别？**

1. **Collection 是一个集合接口：**它提供了对集合对象进行基本操作的通用接口方法。Collection接口在Java 类库中有很多具体的实现。是list，set等的父接口。
2. **Collections 是一个包装类：**它包含有各种有关集合操作的静态多态方法。此类不能实例化，就像一个工具类，服务于Java的Collection框架。

日常开发中，不仅要了解Java中的Collection及其子类的用法，还要了解Collections用法。可以提升很多处理集合类的效率。
## **Java中的Collection如何遍历迭代？**

1. **传统的for循环遍历，基于计数器的：**遍历者自己在集合外部维护一个计数器，然后依次读取每一个位置的元素，当读取到最后一个元素后，停止。主要就是需要按元素的位置来读取元素。
2. **迭代器遍历，Iterator：**每一个具体实现的数据集合，一般都需要提供相应的Iterator。相比于传统for循环，Iterator取缔了显式的遍历计数器。所以基于顺序存储集合的Iterator可以直接按位置访问数据。而基于链式存储集合的Iterator，正常的实现，都是需要保存当前遍历的位置。然后根据当前位置来向前或者向后移动指针。
3. **foreach循环遍历：**根据反编译的字节码可以发现，foreach内部也是采用了Iterator的方式实现，只不过Java编译器帮我们生成了这些代码。
4. **迭代器遍历：Enumeration：**Enumeration 接口是Iterator迭代器的“古老版本”，从JDK 1.0开始，Enumeration接口就已经存在了（Iterator从JDK 1.2才出现）
5. **Stream：**JDK 1.8中新增Stream，使用一种类似用 SQL 语句从数据库查询数据的直观方式来提供一种对 Java 集合运算和表达的高阶抽象。Stream API可以极大提高Java程序员的生产力，让程序员写出高效率、干净、简洁的代码。
## Iterable和Iterator如何使用?
Iterator和Iterable是两个接口，前者代表的是迭代的方式，如next和hasNext方法就是需要在该接口中实现。后者代表的是是否可以迭代，如果可以迭代，会返回Iterator接口，即返回迭代方式

常见的使用方式一般是集合实现Iterable表明该集合可以遍历，同时选择Iterator或者自定义一个Iterator的实现类去选择遍历方式，如：
```java
class AbstractList<E> implements Iterable<E> {
    public Iterator<E> iterator() {
        return new Itr();
    }

    private class Itr implements Iterator<E> {}
}
```
### 
### 为什么不把Iterable和Iterator合成一个使用

1. 通过Javadoc文档我们可以发现，Iterable和Iterator并不是同时出现的，Iterator于1.2就出现了，目的是为了代替Enumeration，而Iterable则是1.5才出现的
2. 将<是否可以迭代>和<迭代方式>抽出来，更符合单一职责原则，如果抽出来，迭代方式就可以被多个可迭代的集合复用，更符合面向对象的特点。
