
# 典型回答

Spring 的事务有多种实现，主要包括了声明式事务和编程式事务。

[✅Spring中如何开启事务？](https://www.yuque.com/hollis666/fo22bm/dmp6qs?view=doc_embed)

**如果是我们常用的@Transactional这种声明式事务的话，在多线程情况下是无法生效的**。主要是因为@Transactional 的事务管理使用的是 ThreadLocal 机制来存储事务上下文，而** ThreadLocal 变量是线程隔离的**，即每个线程都有自己的事务上下文副本。因此，在多线程环境下，Spring 的声明式事务会“失效”，即新线程中的操作不会被包含在原有的事务中。

[✅父子线程之间怎么共享数据？](https://www.yuque.com/hollis666/fo22bm/adgan2125uzrsbte?view=doc_embed)

不过，**如果需要管理跨线程的事务，我们可以使用编程式事务**，即自己用 TransactionTemplate 或PlatformTransactionManager 来控制事务的提交。

# 扩展知识

## 源码解析

@Transactional 的事务管理入口在TransactionManager的实现中，如我们看一下[DataSourceTransactionManager类的实现](https://github.com/spring-projects/spring-framework/blob/main/spring-jdbc/src/main/java/org/springframework/jdbc/datasource/DataSourceTransactionManager.java#L304)。

看一下他的doBegin方法：

```javascript
@Override
protected void doBegin(Object transaction, TransactionDefinition definition) {
  // 将传入的事务对象转换为 DataSourceTransactionObject
  DataSourceTransactionObject txObject = (DataSourceTransactionObject) transaction;
  Connection con = null;

  try {
    // 如果当前事务对象没有持有连接，或者持有的连接已经与事务同步，则获取一个新的数据库连接
    if (!txObject.hasConnectionHolder() ||
        txObject.getConnectionHolder().isSynchronizedWithTransaction()) {
      Connection newCon = obtainDataSource().getConnection(); // 从数据源获取新的连接
      if (logger.isDebugEnabled()) {
        logger.debug("Acquired Connection [" + newCon + "] for JDBC transaction");
      }
      // 设置当前事务对象持有的连接
      txObject.setConnectionHolder(new ConnectionHolder(newCon), true);
    }

    // 标记连接已经与事务同步
    txObject.getConnectionHolder().setSynchronizedWithTransaction(true);
    con = txObject.getConnectionHolder().getConnection(); // 获取当前持有的连接

    // 准备连接的事务设置，比如隔离级别
    Integer previousIsolationLevel = DataSourceUtils.prepareConnectionForTransaction(con, definition);
    txObject.setPreviousIsolationLevel(previousIsolationLevel);
    txObject.setReadOnly(definition.isReadOnly()); // 设置事务是否只读

    // 如果需要，切换连接为手动提交模式。这在某些 JDBC 驱动中可能代价很高，因此不是非必要不进行设置
    if (con.getAutoCommit()) {
      txObject.setMustRestoreAutoCommit(true);
      if (logger.isDebugEnabled()) {
        logger.debug("Switching JDBC Connection [" + con + "] to manual commit");
      }
      con.setAutoCommit(false); // 关闭自动提交
    }

    // 准备事务性连接，可能包括设置保存点等
    prepareTransactionalConnection(con, definition);
    txObject.getConnectionHolder().setTransactionActive(true); // 标记事务为活跃状态

    // 设置事务超时时间
    int timeout = determineTimeout(definition);
    if (timeout != TransactionDefinition.TIMEOUT_DEFAULT) {
      txObject.getConnectionHolder().setTimeoutInSeconds(timeout);
    }

    // 如果是新的连接持有者，则将连接持有者绑定到当前线程
    if (txObject.isNewConnectionHolder()) {
      TransactionSynchronizationManager.bindResource(obtainDataSource(), txObject.getConnectionHolder());
    }
  }

  catch (Throwable ex) {
    // 如果在尝试开始事务过程中出现异常，并且是新的连接持有者，则释放连接并清理连接持有者
    if (txObject.isNewConnectionHolder()) {
      DataSourceUtils.releaseConnection(con, obtainDataSource());
      txObject.setConnectionHolder(null, false);
    }
    // 抛出无法创建事务的异常
    throw new CannotCreateTransactionException("Could not open JDBC Connection for transaction", ex);
  }
}

```

重点是上面的第47-49行代码，这里是把一个connection和当前线程进行绑定。看下[绑定代码的实现](https://github.com/spring-projects/spring-framework/blob/main/spring-tx/src/main/java/org/springframework/transaction/support/TransactionSynchronizationManager.java#L76)：

```javascript
public static void bindResource(Object key, Object value) throws IllegalStateException {
    // 如果必要的话，解包资源的实际键值
    Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
    // 断言value不为空，否则抛出异常
    Assert.notNull(value, "Value must not be null");
    // 从ThreadLocal中获取当前线程的资源映射表
    Map<Object, Object> map = resources.get();
    // 如果当前线程还没有资源映射表，则创建一个新的HashMap，并设置到ThreadLocal中
    if (map == null) {
        map = new HashMap<>();
        resources.set(map);
    }
    // 尝试将资源（value）与键（actualKey）绑定，如果该键已经绑定过资源，则返回原来的资源
    Object oldValue = map.put(actualKey, value);
    // 如果之前绑定的资源是一个被标记为void（无效）的ResourceHolder，则忽略它，视为未绑定过资源
    if (oldValue instanceof ResourceHolder resourceHolder && resourceHolder.isVoid()) {
        oldValue = null;
    }
    // 如果该键已经绑定过其他资源，则抛出IllegalStateException异常
    if (oldValue != null) {
        throw new IllegalStateException(
                "Already value [" + oldValue + "] for key [" + actualKey + "] bound to thread");
    }
}

```

这里面关键的一部就是第11行， resources.set(map);这个resources是啥呢？

```javascript
private static final ThreadLocal<Map<Object, Object>> resources =
			new NamedThreadLocal<>("Transactional resources");
```

看到了吧，ThreadLocal ！！！这个[NamedThreadLocal](https://github.com/spring-projects/spring-framework/blob/main/spring-core/src/main/java/org/springframework/core/NamedThreadLocal.java)其实就是可以定义一个名字的ThreadLocal而已。

