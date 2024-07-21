序列化是将对象的状态信息转换为可存储或传输的形式的过程。我们都知道，Java对象是保存在JVM的堆内存中的，也就是说，如果JVM堆不存在了，那么对象也就跟着消失了。

而序列化提供了一种方案，可以让你在即使JVM停机的情况下也能把对象保存下来的方案。就像我们平时用的U盘一样。

把Java对象序列化成可存储或传输的形式（如二进制流），比如保存在文件中。这样，当再次需要这个对象的时候，从文件中读取出二进制流，再从二进制流中反序列化出对象。

但是，**虚拟机是否允许反序列化，不仅取决于类路径和功能代码是否一致，一个非常重要的一点是两个类的序列化 ID 是否一致，即serialVersionUID要求一致。**

在进行反序列化时，JVM会把传来的字节流中的serialVersionUID与本地相应实体类的serialVersionUID进行比较，如果相同就认为是一致的，可以进行反序列化，否则就会出现序列化版本不一致的异常，即是InvalidCastException。这样做是为了保证安全，因为文件存储中的内容可能被篡改。

当实现java.io.Serializable接口的类没有显式地定义一个serialVersionUID变量时候，Java序列化机制会根据编译的Class自动生成一个serialVersionUID作序列化版本比较用，这种情况下，如果Class文件没有发生变化，就算再编译多次，serialVersionUID也不会变化的。但是，如果发生了变化，那么这个文件对应的serialVersionUID也就会发生变化。

基于以上原理，如果我们一个类实现了Serializable接口，但是没有定义serialVersionUID，然后序列化。在序列化之后，由于某些原因，我们对该类做了变更，重新启动应用后，我们相对之前序列化过的对象进行反序列化的话就会报错。

# 扩展知识

## 改了会怎么样

我们举个例子吧，看看如果serialVersionUID被修改了会发生什么？
```
public class SerializableDemo1 {
    public static void main(String[] args) {
        //Initializes The Object
        User1 user = new User1();
        user.setName("hollis");
        //Write Obj to File
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(new FileOutputStream("tempFile"));
            oos.writeObject(user);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(oos);
        }
    }
}

class User1 implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
 }
```
我们先执行以上代码，把一个User1对象写入到文件中。然后我们修改一下User1类，把serialVersionUID的值改为2L。

```
class User1 implements Serializable {
    private static final long serialVersionUID = 2L;
    private String name;
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
}
```
然后执行以下代码，把文件中的对象反序列化出来：

```
public class SerializableDemo2 {
    public static void main(String[] args) {
        //Read Obj from File
        File file = new File("tempFile");
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(new FileInputStream(file));
            User1 newUser = (User1) ois.readObject();
            System.out.println(newUser);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(ois);
            try {
                FileUtils.forceDelete(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
```
执行结果如下：

```
java.io.InvalidClassException: com.hollis.User1; local class incompatible: stream classdesc serialVersionUID = 1, local class serialVersionUID = 2
```

可以发现，以上代码抛出了一个java.io.InvalidClassException，并且指出serialVersionUID不一致。

这是因为，在进行反序列化时，JVM会把传来的字节流中的serialVersionUID与本地相应实体类的serialVersionUID进行比较，如果相同就认为是一致的，可以进行反序列化，否则就会出现序列化版本不一致的异常，即是InvalidCastException。

## 为什么要明确定一个serialVersionUID

如果我们没有在类中明确的定义一个serialVersionUID的话，看看会发生什么。<br />尝试修改上面的demo代码，先使用以下类定义一个对象，该类中不定义serialVersionUID，将其写入文件。

```
class User1 implements Serializable {
    private String name;
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
 }
```
然后我们修改User1类，向其中增加一个属性。在尝试将其从文件中读取出来，并进行反序列化。

```
class User1 implements Serializable {
    private String name;
    private int age;
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public int getAge() {
        return age;
    }
    public void setAge(int age) {
        this.age = age;
    }
 }
```
执行结果： java.io.InvalidClassException: com.hollis.User1; local class incompatible: stream classdesc serialVersionUID = -2986778152837257883, local class serialVersionUID = 7961728318907695402<br />同样，抛出了InvalidClassException，并且指出两个serialVersionUID不同，分别是-2986778152837257883和7961728318907695402。

从这里可以看出，系统自己添加了一个serialVersionUID。<br />所以，一旦类实现了Serializable，就建议明确的定义一个serialVersionUID。不然在修改类的时候，就会发生异常。

serialVersionUID有两种显示的生成方式：<br />一是默认的1L，比如：private static final long serialVersionUID = 1L;<br />二是根据类名、接口名、成员方法及属性等来生成一个64位的哈希字段，比如：<br />private static final long serialVersionUID = xxxxL;
