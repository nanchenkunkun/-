# MyBatis缓存

### MyBatis缓存介绍

首先看一段wiki上关于MyBatis缓存的介绍：

> MyBatis支持声明式数据缓存（declarative data caching）。当一条SQL语句被标记为“可缓存”后，首次执行它时从数据库获取的所有数据会被存储在一段高速缓存中，今后执行这条语句时就会从高速缓存中读取结果，而不是再次命中数据库。MyBatis提供了默认下基于Java HashMap的缓存实现，以及用于与OSCache、Ehcache、Hazelcast和Memcached连接的默认连接器。MyBatis还提供API供其他缓存实现使用。

重点的那句话就是：**MyBatis执行SQL语句之后，这条语句就是被缓存，以后再执行这条语句的时候，会直接从缓存中拿结果，而不是再次执行SQL**

这也就是大家常说的MyBatis一级缓存，一级缓存的作用域scope是SqlSession。

MyBatis同时还提供了一种全局作用域global scope的缓存，这也叫做二级缓存，也称作全局缓存。

### 一级缓存

##### 测试

同个session进行两次相同查询：

```java
@Test
public void test() {
    SqlSession sqlSession = sqlSessionFactory.openSession();
    try {
        User user = (User)sqlSession.selectOne("org.format.mybatis.cache.UserMapper.getById", 1);
        log.debug(user);
        User user2 = (User)sqlSession.selectOne("org.format.mybatis.cache.UserMapper.getById", 1);
        log.debug(user2);
    } finally {
        sqlSession.close();
    }
}
```

MyBatis只进行1次数据库查询：

```java
==>  Preparing: select * from USERS WHERE ID = ? 
==> Parameters: 1(Integer)
<==      Total: 1
User{id=1, name='format', age=23, birthday=Sun Oct 12 23:20:13 CST 2014}
User{id=1, name='format', age=23, birthday=Sun Oct 12 23:20:13 CST 2014}
```

同个session进行两次不同的查询：

```
@Test
public void test() {
    SqlSession sqlSession = sqlSessionFactory.openSession();
    try {
        User user = (User)sqlSession.selectOne("org.format.mybatis.cache.UserMapper.getById", 1);
        log.debug(user);
        User user2 = (User)sqlSession.selectOne("org.format.mybatis.cache.UserMapper.getById", 2);
        log.debug(user2);
    } finally {
        sqlSession.close();
    }
}
```

MyBatis进行两次数据库查询：

```
==>  Preparing: select * from USERS WHERE ID = ? 
==> Parameters: 1(Integer)
<==      Total: 1
User{id=1, name='format', age=23, birthday=Sun Oct 12 23:20:13 CST 2014}
==>  Preparing: select * from USERS WHERE ID = ? 
==> Parameters: 2(Integer)
<==      Total: 1
User{id=2, name='FFF', age=50, birthday=Sat Dec 06 17:12:01 CST 2014}
```

不同session，进行相同查询：

```
@Test
public void test() {
    SqlSession sqlSession = sqlSessionFactory.openSession();
    SqlSession sqlSession2 = sqlSessionFactory.openSession();
    try {
        User user = (User)sqlSession.selectOne("org.format.mybatis.cache.UserMapper.getById", 1);
        log.debug(user);
        User user2 = (User)sqlSession2.selectOne("org.format.mybatis.cache.UserMapper.getById", 1);
        log.debug(user2);
    } finally {
        sqlSession.close();
        sqlSession2.close();
    }
}
```

MyBatis进行了两次数据库查询：

```
==>  Preparing: select * from USERS WHERE ID = ? 
==> Parameters: 1(Integer)
<==      Total: 1
User{id=1, name='format', age=23, birthday=Sun Oct 12 23:20:13 CST 2014}
==>  Preparing: select * from USERS WHERE ID = ? 
==> Parameters: 1(Integer)
<==      Total: 1
User{id=1, name='format', age=23, birthday=Sun Oct 12 23:20:13 CST 2014}
```

同个session,查询之后更新数据，再次查询相同的语句：

```
@Test
public void test() {
    SqlSession sqlSession = sqlSessionFactory.openSession();
    try {
        User user = (User)sqlSession.selectOne("org.format.mybatis.cache.UserMapper.getById", 1);
        log.debug(user);
        user.setAge(100);
        sqlSession.update("org.format.mybatis.cache.UserMapper.update", user);
        User user2 = (User)sqlSession.selectOne("org.format.mybatis.cache.UserMapper.getById", 1);
        log.debug(user2);
        sqlSession.commit();
    } finally {
        sqlSession.close();
    }
}
```

更新操作之后缓存会被清除：

```
==>  Preparing: select * from USERS WHERE ID = ? 
==> Parameters: 1(Integer)
<==      Total: 1
User{id=1, name='format', age=23, birthday=Sun Oct 12 23:20:13 CST 2014}
==>  Preparing: update USERS SET NAME = ? , AGE = ? , BIRTHDAY = ? where ID = ? 
==> Parameters: format(String), 23(Integer), 2014-10-12 23:20:13.0(Timestamp), 1(Integer)
<==    Updates: 1
==>  Preparing: select * from USERS WHERE ID = ? 
==> Parameters: 1(Integer)
<==      Total: 1
User{id=1, name='format', age=23, birthday=Sun Oct 12 23:20:13 CST 2014}
```

很明显，结果验证了一级缓存的概念，在同个SqlSession中，查询语句相同的sql会被缓存，但是一旦执行新增或更新或删除操作，缓存就会被清除

### 源码分析

在分析MyBatis的一级缓存之前，我们先简单看下MyBatis中几个重要的类和接口：

1. org.apache.ibatis.session.Configuration类：MyBatis全局配置信息类
2. org.apache.ibatis.session.SqlSessionFactory接口：操作SqlSession的工厂接口，具体的实现类是DefaultSqlSessionFactory
3. org.apache.ibatis.session.SqlSession接口：执行sql，管理事务的接口，具体的实现类是DefaultSqlSession
4. org.apache.ibatis.executor.Executor接口：sql执行器，SqlSession执行sql最终是通过该接口实现的，常用的实现类有SimpleExecutor和CachingExecutor,这些实现类都使用了装饰者设计模式

##### 一级缓存的作用域是SqlSession，那么我们就先看一下SqlSession的select过程：

这是DefaultSqlSession（SqlSession接口实现类，MyBatis默认使用这个类）的selectList源码（我们例子上使用的是selectOne方法，调用selectOne方法最终会执行selectList方法）：

```
public <E> List<E> selectList(String statement, Object parameter, RowBounds rowBounds) {
    try {
      MappedStatement ms = configuration.getMappedStatement(statement);
      List<E> result = executor.query(ms, wrapCollection(parameter), rowBounds, Executor.NO_RESULT_HANDLER);
      return result;
    } catch (Exception e) {
      throw ExceptionFactory.wrapException("Error querying database.  Cause: " + e, e);
    } finally {
      ErrorContext.instance().reset();
    }
}
```

