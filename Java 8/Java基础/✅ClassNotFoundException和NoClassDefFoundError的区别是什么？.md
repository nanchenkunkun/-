# 典型回答

**ClassNotFoundException**是一个受检异常（checked exception）。他通常在运行时，在类加载阶段尝试加载类的过程中，找不到类的定义时触发。通常是由Class.forName()或类加载器loadClass或者findSystemClass时，在类路径中没有找到指定名称的类时，会抛出该异常。表示所需的类在类路径中不存在。这通常是由于类名拼写错误或缺少依赖导致的。

如以下方式加载JDBC驱动：

```java
public class MainClass
{
    public static void main(String[] args)
    {
        try
        {
            Class.forName("oracle.jdbc.driver.OracleDriver");
        }catch (ClassNotFoundException e)
        {
            e.printStackTrace();
        }
    }
}

```

当我们的classpath中没有对应的jar包时，就会抛出这个ClassNotFoundException。

**NoClassDefFoundError**是一个错误（error），它表示运行时尝试加载一个类的定义时，虽然找到了类文件，但在加载、解析或链接类的过程中发生了问题。这通常是由于依赖问题或类定义文件（.class文件）损坏导致的。也就是说这个类在编译时存在，运行时丢失了，就会导致这个异常。

如以下情况，我们定义A类和B类，
```java
class A
{
  // some code
}

public class B
{
    public static void main(String[] args)
    {
        A a = new A();
    }
}

```

在编译后会生成A.class和B.class，当我们删除A.class之后，单独运行B.class的时候，就会发生NoClassDefFoundError

# 扩展知识

## NoSuchMethodError
NoSuchMethodError表示方法找不到，他和NoClassDefFoundError类似，都是编译期找得到，运行期找不到了。

这种error发生在生产环境中是，通常来说大概率是发生了jar包冲突。

[✅Maven如何解决jar包冲突的问题？](https://www.yuque.com/hollis666/fo22bm/vkkiva?view=doc_embed)

