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



### 拦截对象的变身

一旦我们在mybatis-config.xml里面配置了插件：

```xml
<plugins>
    <plugin interceptor="cn.mybatis.MyInterceptor"></plugin>
</plugins>
```

ParameterHandler，ResultSetHandler，StatementHandler，Executor这“四大金刚”就发生变身，而不再是原身。这个变身的过程是这样的，以ParameterHandler为例：

第一步：根据插件配置，利用反射技术，创建interceptor拦截器

Interceptor interceptor = (Interceptor) MyInterceptor.class.newInstance();

第一步：创建原身
ParameterHandler parameterHandler = createParameterHandler();

第二步：原身+拦截器
parameterHandler = (ParameterHandler) Plugin.wrap(parameterHandler, interceptor);
在这一步，将parameterHandler和interceptor包装到一起，生成了变身，并重新赋值给parameterHandler变量

> 代码备注：也许上面的代码，你没有看懂，但是没有关系，其中的细节你可以不去探究，你只要明白代码的字面意思，createParameterHandler()，这是创建ParameterHandler对象，Plugin.wrap就是包装，把两个类包装一下，重新生成一个新的类。你只要能理清“原身->变身”这个过程，这就足够了，就能把MyBatis插件机制的精髓已经掌握了。



## MyBatis拦截器插件整个运行过程

### 没有插件的运行过程

![](image\3.png)

### 有插件的运行过程

![](image\4.png)



## 小结

一旦配置上插件，ParameterHandler，ResultSetHandler，StatementHandler，Executor这四大核心对象，将会生成变身，是一种代理对象，而不再是原身，仅此而已。