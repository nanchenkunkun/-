# 典型回答

final、finally、finalize有什么区别？这个问题就像周杰、周杰伦和周星驰之间有啥关系的问题一样。其实没啥关系，放在一起比较无非是名字有点像罢了。

final、finally和finalize是Java中的三个不同的概念。

- **final**：用于声明变量、方法或类，使之不可变、不可重写或不可继承。
- **finally**：是异常处理的一部分，用于确保代码块（通常用于资源清理）总是执行。
- **finalize**：是Object类的一个方法，用于在对象被垃圾回收前执行清理操作，但通常不推荐使用。
### final

final是一个关键字，可以用来修饰变量、方法和类。分别代表着不同的含义。

**final变量**：即我们所说的常量，一旦被赋值后，就不能被修改。

```sql
final int x = 100;
// x = 200; // 编译错误，不能修改final变量的值

public static final String AUTHOR_NAME = "Hollis";
```

**final方法**：不能被子类重写。

```sql
public final void show() {
    // ...
}
```

**final类**：不能被继承。

```sql
public final class MyFinalClass {
    // ...
}
```

### finally

finally是一个用于异常处理，它和try、catch块一起使用。无论是否捕获或处理异常，finally块中的代码总是执行（程序正常执行的情况）。通常用于关闭资源，如输入/输出流、数据库连接等。

```sql
try {
    // 可能产生异常的代码
} catch (Exception e) {
    // 异常处理代码
} finally {
    // 清理代码，总是执行
}

```

[✅finally中代码一定会执行吗？](https://www.yuque.com/hollis666/fo22bm/rs846vlvpa7dwe3v?view=doc_embed)

[✅try中return A，catch中return B，finally中return C，最终返回值是什么？](https://www.yuque.com/hollis666/fo22bm/ltw8ngs7yntrdk3a?view=doc_embed)


### finalize
finalize是Object类的一个方法，用于垃圾收集过程中的资源回收。在对象被垃圾收集器回收之前，finalize方法会被调用，用于执行清理操作（例如释放资源）。但是，不推荐依赖finalize方法进行资源清理，因为它的调用时机不确定且不可靠。

```sql
protected void finalize() throws Throwable {
    // 在对象被回收时执行清理工作
}
```


