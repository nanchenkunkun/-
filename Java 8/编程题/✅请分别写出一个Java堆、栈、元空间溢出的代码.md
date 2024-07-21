# 典型回答
### Java堆溢出

Java堆用于存储对象实例。堆溢出通常是由于创建了太多对象，而没有足够的堆内存来存储这些对象。

我们只需要写一个死循环， 在里面不断地创建对象就行了。一直运行下去，就会导致OOM

```latex
import java.util.ArrayList;
import java.util.List;

public class HeapOverflow {
    public static void main(String[] args) {
        List<Object> objects = new ArrayList<>();
        while (true) {
            objects.add(new Object()); // 不断创建对象并保留引用
        }
    }
}

```

### Java 栈溢出

Java栈用于存储局部变量和方法调用。栈溢出通常是因为过深的方法调用，如递归调用没有正确的终止条件。

```latex
public class StackOverflow {
    public static void main(String[] args) {
        recursiveMethod(1); // 递归调用，没有终止条件
    }

    private static void recursiveMethod(int i) {
        recursiveMethod(i);
    }
}

```

### 元空间溢出

元空间用于存储类的元数据。元空间溢出通常是由于加载了太多类或者类的元数据过大。

```latex
import javassist.ClassPool;

public class MetaspaceOverflow {
    public static void main(String[] args) {
        ClassPool classPool = ClassPool.getDefault();
        for (int i = 0; ; i++) {
            classPool.makeClass("Class" + i).toClass(); // 动态创建大量的类
        }
    }
}

```

对于元空间溢出的示例，需要使用javassist这个第三方库来动态创建类。你可以通过 Maven 将 javassist 添加到项目依赖中。
