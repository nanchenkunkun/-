# 典型回答

序列化是将对象转换为可传输格式的过程。 是一种数据的持久化手段。一般广泛应用于网络传输，RMI和RPC等场景中。  几乎所有的商用编程语言都有序列化的能力，不管是数据存储到硬盘，还是通过网络的微服务传输，都需要序列化能力。

在Java的序列化机制中，如果是String，枚举或者实现了Serializable接口的类，均可以通过Java的序列化机制，将类序列化为符合编码的数据流，然后通过InputStream和OutputStream将内存中的类持久化到硬盘或者网络中；同时，也可以通过反序列化机制将磁盘中的字节码再转换成内存中的类。

**如果一个类想被序列化，需要实现Serializable接口。**否则将抛出NotSerializableException异常。Serializable接口没有方法或字段，仅用于标识可序列化的语义。

自定义类通过实现Serializable接口做标识，进而在IO中实现序列化和反序列化，具体的执行路径如下：

`#writeObject -> #writeObject0(判断类是否是自定义类) -> #writeOrdinaryObject(区分Serializable和Externalizable) -> writeSerialData(序列化fields) -> invokeWriteObject(反射调用类自己的序列化策略)`

其中，在invokeWriteObject的阶段，系统就会处理自定义类的序列化方案。

这是因为，在序列化操作过程中会对类型进行检查，要求被序列化的类必须属于Enum、Array和Serializable类型其中的任何一种。
# 知识拓展
## Serializable 和 Externalizable 接口有何不同？
类通过实现 java.io.Serializable 接口以启用其序列化功能。未实现此接口的类将无法使其任何状态序列化或反序列化。可序列化类的所有子类型本身都是可序列化的。序列化接口没有方法或字段，仅用于标识可序列化的语义。

当试图对一个对象进行序列化的时候，如果遇到不支持 Serializable 接口的对象。在此情况下，将抛出 NotSerializableException。

如果要序列化的类有父类，要想同时将在父类中定义过的变量持久化下来，那么父类也应该实现java.io.Serializable接口。<br />Externalizable继承了Serializable，该接口中定义了两个抽象方法：writeExternal()与readExternal()。当使用Externalizable接口来进行序列化与反序列化的时候需要开发人员重写writeExternal()与readExternal()方法。如果没有在这两个方法中定义序列化实现细节，那么序列化之后，对象内容为空。实现Externalizable接口的类必须要提供一个public的无参的构造器。

所以，实现Externalizable，并实现writeExternal()和readExternal()方法可以指定序列化哪些属性。
## 如果序列化后的文件或者原始类被篡改，还能被反序列化吗？
[✅serialVersionUID 有何用途? 如果没定义会有什么问题？](https://www.yuque.com/hollis666/fo22bm/yy4icr?view=doc_embed)

## 在Java中，有哪些好的序列化框架，有什么好处
Java中常用的序列化框架：

java、kryo、hessian、protostuff、gson、fastjson等。

Kryo：速度快，序列化后体积小；跨语言支持较复杂

Hessian：默认支持跨语言；效率不高

Protostuff：速度快，基于protobuf；需静态编译

Protostuff-Runtime：无需静态编译，但序列化前需预先传入schema；不支持无默认构造函数的类，反序列化时需用户自己初始化序列化后的对象，其只负责将该对象进行赋值

Java：使用方便，可序列化所有类；速度慢，占空间

