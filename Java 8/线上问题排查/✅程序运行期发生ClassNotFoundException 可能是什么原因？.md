# 典型回答

找不到类，一般是编译器可能会出现的问题，就是编译的时候找不到对应的类，这时候IDE 会爆红，无法编译通过。

如果编译正常了，说明类都是存在的，那么为啥在运行期还会可能报错提示ClassNotFoundException呢？

**ClassNotFoundException**是一个受检异常（checked exception）。他通常在运行时，在类加载阶段尝试加载类的过程中，找不到类的定义时触发。

一般来说，有以下几种可能，一般都是喝类加载有关的。

[✅Java中类加载的过程是怎么样的？](https://www.yuque.com/hollis666/fo22bm/tuikxhaa2urq32ds?view=doc_embed)

### 动态加载类

在 Java 中，提供了很多可以在运行期动态的加载类的方法，而这些运行期动态加载的类，不会在编译器进行编译检查，那么运行期如果无法加载，那么就会抛出 `ClassNotFoundException`。

在Java中，有几种方式可以进行动态加载类：

1. **使用 Class.forName() 方法**：

```java
Class<?> clazz = Class.forName("com.example.HollisTest");
Object obj = clazz.newInstance(); // 创建类的实例
// 可以通过 clazz 进行更多操作，如调用方法等
```

   - `Class.forName(String className)`方法可以根据类的全限定名动态加载类。它会返回一个 Class 对象，可以通过该对象创建实例或者调用类的静态方法。

2. **使用反射机制**：

```java
Class<?> clazz = Class.forName("com.example.HollisTest");
Object obj = clazz.getDeclaredConstructor().newInstance(); // 创建类的实例
Method method = clazz.getMethod("methodName", parameterTypes);
Object result = method.invoke(obj, args); // 调用方法
// 其它操作...
```

   - Java 的反射机制（`java.lang.reflect`包）可以在运行时动态地获取类的信息、调用类的方法、访问字段等。虽然不是直接加载类，但可以在已知类名的情况下，动态地操作类的成员和行为。

2. **使用 ClassLoader.loadClass() 方法**：

```java
ClassLoader classLoader = ClassLoader.getSystemClassLoader();
Class<?> clazz = classLoader.loadClass("com.example.HollisTest");
Object obj = clazz.newInstance(); // 创建类的实例
// 可以通过 clazz 进行更多操作，如调用方法等
```

   - `ClassLoader.loadClass(String className)`方法也可以用来动态加载类。与`Class.forName()`不同的是，`loadClass()`是通过类加载器来加载类的，可以更加灵活地控制加载的过程。

3. **使用 ClassLoader.defineClass() 方法**：

```java
byte[] classData = // 从某处获取类的字节码数据
ClassLoader classLoader = new MyClassLoader(); // 自定义类加载器
Class<?> clazz = classLoader.defineClass("com.example.HollisTest", classData, 0, classData.length);
Object obj = clazz.newInstance(); // 创建类的实例
```

   - `ClassLoader.defineClass(String name, byte[] b, int off, int len)`方法可以通过字节数组形式定义类，然后加载它。通常用于自定义类加载器的实现。

### 版本兼容性问题

如果你的程序依赖的某个类发生了版本变化，并且新的版本在运行时无法被找到，也会导致 `ClassNotFoundException`。这一版出现在 jar 包冲突的情况下，比如我们在代码中多个地方依赖了 apache commons 这个包，但是依赖的 jar 包的版本不一样，那么在编译期可能都可以正常编译，但是最终会被 maven 给仲裁成其中的某一个版本，假如仲裁的结果是一个1.0.0的版本，这样就会导致某些在新版本1.0.1中新的 Class 就无法被找到。

[✅Maven如何解决jar包冲突的问题？](https://www.yuque.com/hollis666/fo22bm/vkkiva?view=doc_embed)

所以，线上在出现ClassNotFoundException的时候，可以考虑 jar 包冲突的问题。

### 类路径动态变化

如果在运行时类路径发生了变化（比如某些类被动态移除或者替换），而此时程序尝试加载被移除的类，就会出现 `ClassNotFoundException`。

### 模块化系统问题
 如果你在使用 Java 9 或更新版本的模块化系统中，模块依赖的配置可能会影响类的可见性，进而导致 `ClassNotFoundException`。

Java 9 中的模块需要明确声明它所依赖的其他模块。只有声明了依赖关系，模块才能使用其它模块提供的类和服务。如果没有正确声明依赖关系，或者依赖的模块无法满足版本要求或不存在，就会导致运行时的 ModuleNotFoundException 或 ClassNotFoundException。

