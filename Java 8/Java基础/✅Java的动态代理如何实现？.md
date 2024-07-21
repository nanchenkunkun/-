# 典型回答
**在Java中，实现动态代理有两种方式：**

1. **JDK动态代理**：Java.lang.reflect 包中的Proxy类和InvocationHandler接口提供了生成动态代理类的能力。
2. **Cglib动态代理**：Cglib (Code Generation Library )是一个第三方代码生成类库，运行时在内存中动态生成一个子类对象从而实现对目标对象功能的扩展。


**JDK动态代理和Cglib动态代理的区别：**

JDK的动态代理有一个限制，就是使用动态代理的对象必须实现一个或多个接口。如果想代理没有实现接口的类，就可以使用CGLIB实现。

Cglib是一个强大的高性能的代码生成包，它可以在运行期扩展Java类与实现Java接口。它广泛的被许多AOP的框架使用，例如Spring AOP和dynaop，为他们提供方法的interception（拦截）。

Cglib包的底层是通过使用一个小而快的字节码处理框架ASM，来转换字节码并生成新的类。不鼓励直接使用ASM，因为它需要你对JVM内部结构包括class文件的格式和指令集都很熟悉。

**所以，使用JDK动态代理的对象必须实现一个或多个接口；而使用cglib代理的对象则无需实现接口，达到代理类无侵入。**
# 拓展知识
## 静态代理和动态代理的区别
最大的区别就是静态代理是编译期确定的，但是动态代理却是运行期确定的。

同时，使用静态代理模式需要程序员手写很多代码，这个过程是比较浪费时间和精力的。一旦需要代理的类中方法比较多，或者需要同时代理多个对象的时候，这无疑会增加很大的复杂度。

反射是动态代理的实现方式之一。

## 动态代理的用途

Java的动态代理，在日常开发中可能并不经常使用，但是并不代表他不重要。**Java的动态代理的最主要的用途就是应用在各种框架中。因为使用动态代理可以很方便的运行期生成代理类，通过代理类可以做很多事情，比如AOP，比如过滤器、拦截器等。**

在我们平时使用的框架中，像servlet的filter、包括spring提供的aop以及struts2的拦截器都使用了动态代理功能。我们日常看到的mybatis分页插件，以及日志拦截、事务拦截、权限拦截这些几乎全部有动态代理的身影。
## Spring AOP的实现方式
Spring AOP中的动态代理主要有两种方式，JDK动态代理和CGLIB动态代理。

JDK动态代理通过反射来接收被代理的类，并且要求被代理的类必须实现一个接口。JDK动态代理的核心是InvocationHandler接口和Proxy类。

如果目标类没有实现接口，那么Spring AOP会选择使用CGLIB来动态代理目标类。

CGLIB（Code Generation Library），是一个代码生成的类库，可以在运行时动态的生成某个类的子类，注意，CGLIB是通过继承的方式做的动态代理，因此如果某个类被标记为final，那么它是无法使用CGLIB做动态代理的。
## JDK 动态代理的代码段
```java
public class UserServiceImpl implements UserService {
    @Override
    public void add() {
        // TODO Auto-generated method stub
        System.out.println("--------------------add----------------------");
    }
}

public class MyInvocationHandler implements InvocationHandler {
    private Object target;
    public MyInvocationHandler(Object target) {
        super();
        this.target = target;
    }
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        PerformanceMonior.begin(target.getClass().getName()+"."+method.getName());
        //System.out.println("-----------------begin "+method.getName()+"-----------------");
        Object result = method.invoke(target, args);
        //System.out.println("-----------------end "+method.getName()+"-----------------");
        PerformanceMonior.end();
        return result;
    }
    public Object getProxy(){
        return Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), target.getClass().getInterfaces(), this);
    }
}
public static void main(String[] args) {
    UserService service = new UserServiceImpl();
    MyInvocationHandler handler = new MyInvocationHandler(service);
    UserService proxy = (UserService) handler.getProxy();
    proxy.add();
}
```
## Cglib动态代理的代码段
```java
public class UserServiceImpl implements UserService {
    @Override
    public void add() {
        // TODO Auto-generated method stub
        System.out.println("--------------------add----------------------");
    }
}
public class CglibProxy implements MethodInterceptor{ 
    private Enhancer enhancer = new Enhancer(); 
    public Object getProxy(Class clazz){ 
        //设置需要创建子类的类 
        enhancer.setSuperclass(clazz); 
        enhancer.setCallback(this); 
        //通过字节码技术动态创建子类实例 
        return enhancer.create(); 
    } 
    //实现MethodInterceptor接口方法 
    public Object intercept(Object obj, Method method, Object[] args, 
        MethodProxy proxy) throws Throwable { 
        System.out.println("前置代理"); 
        //通过代理类调用父类中的方法 
        Object result = proxy.invokeSuper(obj, args); 
        System.out.println("后置代理"); 
        return result; 
    } 
} 
public class DoCGLib { 
    public static void main(String[] args) { 
        CglibProxy proxy = new CglibProxy(); 
        //通过生成子类的方式创建代理类 
        UserServiceImpl proxyImp = (UserServiceImpl)proxy.getProxy(UserServiceImpl.class); 
        proxyImp.add(); 
    } 
}
```
