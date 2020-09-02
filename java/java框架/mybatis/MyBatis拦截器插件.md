## MyBatis拦截器插件

### 拦截的对象

我们知道，MyBatis有四大核心对象：

1) Executor是 Mybatis的内部执行器，它负责调用StatementHandler操作数据库，并把结果集通过 ResultSetHandler进行自动映射，另外，他还处理了二级缓存的操作。从这里可以看出，我们也是可以通过插件来实现自定义的二级缓存的。

2) StatementHandler是Mybatis直接和数据库执行sql脚本的对象。另外它也实现了Mybatis的一级缓存。这里，我们可以使用插件来实现对一级缓存的操作(禁用等等)。

3) ParameterHandler是Mybatis实现Sql入参设置的对象。插件可以改变我们Sql的参数默认设置。

4) ResultSetHandler是Mybatis把ResultSet集合映射成POJO的接口对象。我们可以定义插件对Mybatis的结果集自动映射进行修改。



### 拦截的技术方案

在Java里面，我们想拦截某个对象，只需要把这个对象包装一下，用代码行话来说，就是重新生成一个代理对象。

这样在每次调用Executor类的方法的时候，总是要经过Interceptor接口的拦截。

Mybatis的插件实现要实现Interceptor接口，我们看下这个接口定义的方法。

```java
public interface Interceptor {   
   //插件运行的代码，它将代替原有的方法
   Object intercept(Invocation invocation) throws Throwable;     
   // 拦截四大接口  
   Object plugin(Object target);    
   // 配置自定义相关属性
   void setProperties(Properties properties);
}
```

plugin接口返回参数target对象（Executor/ParameterHandler/ResultSetHander/StatementHandler）的代理对象。在调用对应对象的接口的时候，可以进行拦截并处理。

```java
@Intercepts({ @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class}) })
public class LogInterceptor implements Interceptor {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {

    }
}
```

@Intercepts({ @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class}) })

这一行是标注要拦截哪个类上的方法，参数等。然后使用动态代理的invoke(Object obj, Object... args)方法，执行我们的逻辑

如果有多个拦截器则按照顺序(因为他内部是使用的ArrayList容器，有序可重复，大家都懂得)

具体执行的话是使用Plugin类中的Object invoke(Object proxy, Method method, Object[] args) throws Throwable 方法来做的。可以看得出来。它里面调用了我们编写的 Object intercept(Invocation invocation) throws Throwable; 

之后我们需要把我们写的拦截器类注入到拦截器链中。



## Mybatis四大接口对象创建方法

```java
public Executor newExecutor(Transaction transaction, ExecutorType executorType) {
   //确保ExecutorType不为空(defaultExecutorType有可能为空)
   executorType = executorType == null ? defaultExecutorType : executorType;
   executorType = executorType == null ? ExecutorType.SIMPLE : executorType;
   Executor executor;   
    if (ExecutorType.BATCH == executorType) {
      executor = new BatchExecutor(this, transaction);
   } else if (ExecutorType.REUSE == executorType) {
      executor = new ReuseExecutor(this, transaction);
   } else {
      executor = new SimpleExecutor(this, transaction);
   }   if (cacheEnabled) {
      executor = new CachingExecutor(executor);
   }
   executor = (Executor) interceptorChain.pluginAll(executor);
   return executor;
}

public StatementHandler newStatementHandler(Executor executor, MappedStatement mappedStatement, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
   StatementHandler statementHandler = new RoutingStatementHandler(executor, mappedStatement, parameterObject, rowBounds, resultHandler, boundSql);
   statementHandler = (StatementHandler) interceptorChain.pluginAll(statementHandler);
   return statementHandler;
}

public ParameterHandler newParameterHandler(MappedStatement mappedStatement, Object parameterObject, BoundSql boundSql) {
   ParameterHandler parameterHandler = mappedStatement.getLang().createParameterHandler(mappedStatement, parameterObject, boundSql);
   parameterHandler = (ParameterHandler) interceptorChain.pluginAll(parameterHandler);
   return parameterHandler;
}

public ResultSetHandler newResultSetHandler(Executor executor, MappedStatement mappedStatement, RowBounds rowBounds, ParameterHandler parameterHandler, ResultHandler resultHandler, BoundSql boundSql) {
   ResultSetHandler resultSetHandler = new DefaultResultSetHandler(executor, mappedStatement, parameterHandler, resultHandler, boundSql, rowBounds);
   resultSetHandler = (ResultSetHandler) interceptorChain.pluginAll(resultSetHandler);
   return resultSetHandler;
}
```



## 实现原理

有可能从上面的使用猜出来它的拦截器的实现原理，那就是动态代理的方式，只不过`mybatis`这里并不是很直接的来使用代理，绕了个弯，于是给人感觉特别晕，也说不好这个实现是不是有些问题

就先从`Executor`说起吧

我们从`SqlSessionFactory`获取一个`SqlSession`时，会创建一个新的`Executor`实例，这个实际的创建动作在这里

```
org.apache.ibatis.session.Configuration#newExecutor
```

```java
public Executor newExecutor(Transaction transaction, ExecutorType executorType) {
  // 根据设置的执行器类型，创建相应类型的执行器
  executorType = executorType == null ? defaultExecutorType : executorType;
  executorType = executorType == null ? ExecutorType.SIMPLE : executorType;
  Executor executor;
  if (ExecutorType.BATCH == executorType) {
    executor = new BatchExecutor(this, transaction);
  } else if (ExecutorType.REUSE == executorType) {
    executor = new ReuseExecutor(this, transaction);
  } else {
    executor = new SimpleExecutor(this, transaction);
  }
  // 如果开启了缓存，使用缓存，这里缓存执行器有点类似静态代理了
  if (cacheEnabled) {
    executor = new CachingExecutor(executor);
  }
  // 将原始执行器对象，包装下，生成一个新的执行器，代理后的对象
  executor = (Executor) interceptorChain.pluginAll(executor);
  return executor;
}
```

`InterceptorChain`是一个管家，有点类似于`FilterChain`，但是注意，其实差别非常的大

```java
public class InterceptorChain {
	// 所有拦截器列表
  private final List<Interceptor> interceptors = new ArrayList<>();

  // 这里可能会是层层代理对象，一套又一套的，具体取决于拦截器的个数和被拦截的
  // 方法所在的类
  // 配置的拦截器数和每一次代理对象生成次数并不相同，会小于等于拦截器的个数
  public Object pluginAll(Object target) {
    for (Interceptor interceptor : interceptors) {
      target = interceptor.plugin(target);
    }
    // 返回最后一次创建的代理对象
    return target;
  }
  // 注册拦截器的
  public void addInterceptor(Interceptor interceptor) {
    interceptors.add(interceptor);
  }
}
```

`Interceptor`是一个接口，也就是mybatis直接暴露给用户使用的需要用户实现的拦截器接口

```java
public interface Interceptor {
   // 实现类填充自己的逻辑，参数为Invocation，
  Object intercept(Invocation invocation) throws Throwable;

   // 默认方法，创建代理对象
  default Object plugin(Object target) {
    return Plugin.wrap(target, this);
  }
   // 实现类去做些事情
  default void setProperties(Properties properties) {
    // NOP
  }

}
```

```java
// 首先这是一个Jdk动态代理的InvocationHandler实现类
public class Plugin implements InvocationHandler {

  private final Object target;
  private final Interceptor interceptor;
  private final Map<Class<?>, Set<Method>> signatureMap;

  private Plugin(Object target, Interceptor interceptor, Map<Class<?>, Set<Method>> signatureMap) {
    this.target = target;
    this.interceptor = interceptor;
    this.signatureMap = signatureMap;
  }
  // 静态方法，用来直接创建代理对象
  public static Object wrap(Object target, Interceptor interceptor) {
    // 获取当前拦截器需要被拦截的所有的方法
    Map<Class<?>, Set<Method>> signatureMap = getSignatureMap(interceptor);
    // 获取被代理对象的Class
    Class<?> type = target.getClass();
    // 把被代理对象所有能在签名Map中找到的直接实现的接口和祖先接口，查找出来
    Class<?>[] interfaces = getAllInterfaces(type, signatureMap);
    // 找到了就创建代理对象
    if (interfaces.length > 0) {
      return Proxy.newProxyInstance(
          type.getClassLoader(),
          interfaces,
          // 创建一个InvocationHandler实例
          new Plugin(target, interceptor, signatureMap));
    }
    // 没找到 就返回
    // 这个对象不一定说一个未经代理过的对象，也可能是代理过的
    return target;
  }

  // 当调用这个代理对象的任何方法时，调用此方法
  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    try {
      // 首先找下当前被调用的这个方法所在的类，被拦截的所有的方法 
      Set<Method> methods = signatureMap.get(method.getDeclaringClass());
      if (methods != null && methods.contains(method)) {
        // 如果当前被调用的方法是需要被拦截的，那么就执行我们自定义的拦截逻辑
        // interceptor是我们自定义的拦截器，在我们自定义的拦截器里，需要获取到
        // 原委托对象，被调用的方法，以及参数，这里做了很好的封装，将用户的使用和
        // 具体的实现，做了一个完全的分离，用户感知不到任何具体的实现
        // Invocation#proceed 就做了一件事 method.invoke(target,args);
        return interceptor.intercept(new Invocation(target, method, args));
      }
      // 如果当前被调用的方法没有被拦截，那么直接调用原方法
      return method.invoke(target, args);
    } catch (Exception e) {
      throw ExceptionUtil.unwrapThrowable(e);
    }
  }

  // 获取拦截器指定的被拦截方法的方法签名
  // key是被拦截方法的返回值类型
  private static Map<Class<?>, Set<Method>> getSignatureMap(Interceptor interceptor) {
    // 获取注解 @Intercepts信息
    Intercepts interceptsAnnotation = interceptor.getClass().getAnnotation(Intercepts.class);
    // issue #251 避免出现没有具体的拦截信息的情况
    if (interceptsAnnotation == null) {
      throw new PluginException("No @Intercepts annotation was found in interceptor " + interceptor.getClass().getName());
    }
    // 获取配置的被拦截方法的签名信息
    Signature[] sigs = interceptsAnnotation.value();
    Map<Class<?>, Set<Method>> signatureMap = new HashMap<>();
    // 统统放到Map
    for (Signature sig : sigs) {
      Set<Method> methods = signatureMap.computeIfAbsent(sig.type(), k -> new HashSet<>());
      try {
        // 将定义构建成实际的Method对象
        Method method = sig.type().getMethod(sig.method(), sig.args());
        methods.add(method);
      } catch (NoSuchMethodException e) {
        throw new PluginException("Could not find method on " + sig.type() + " named " + sig.method() + ". Cause: " + e, e);
      }
    }
    return signatureMap;
  }
  // 获取被代理对象所有的接口信息
  private static Class<?>[] getAllInterfaces(Class<?> type, Map<Class<?>, Set<Method>> signatureMap) {
    Set<Class<?>> interfaces = new HashSet<>();
    while (type != null) {
      for (Class<?> c : type.getInterfaces()) {
        if (signatureMap.containsKey(c)) {
          // 如果被代理类接口和返回值类型一致，接口加进来
          interfaces.add(c);
        }
      }
      // 找到被代理类的父类，然后继续查找接口信息
      type = type.getSuperclass();
    }
    return interfaces.toArray(new Class<?>[interfaces.size()]);
  }
}
```

