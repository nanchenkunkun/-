## mybaits简介

​	mybatis是一款优秀的持久层框架，它支持定制化SQL、存储过程以及高级映射，mybatis避免了几乎所有的jdbc代码和手动设置参数以及获取结果集。mybatis可以使用简单的xml或者注解来配置和隐射原生信息，将接口和java的POJOs(Plain Old Java Objects,普通的java对象)映射成数据库中的记录，本文将通过debug的方式来了解其工作原理.

## mybatis核心类

 **SqlSessionFactory：**每个基于 MyBatis 的应用都是以一个 SqlSessionFactory 的实例为中心的。SqlSessionFactory 的实例可以通过 SqlSessionFactoryBuilder 获得。而 SqlSessionFactoryBuilder 则可以从 XML 配置文件或通过Java的方式构建出 SqlSessionFactory 的实例。SqlSessionFactory 一旦被创建就应该在应用的运行期间一直存在，建议使用单例模式或者静态单例模式。一个SqlSessionFactory对应配置文件中的一个环境（environment），如果你要使用多个数据库就配置多个环境分别对应一个SqlSessionFactory。

**SqlSession：**SqlSession是一个接口，它有2个实现类，分别是DefaultSqlSession(默认使用)以及SqlSessionManager。SqlSession通过内部存放的执行器（Executor）来对数据进行CRUD。此外SqlSession不是线程安全的，因为每一次操作完数据库后都要调用close对其进行关闭，官方建议通过try-finally来保证总是关闭SqlSession。

  **Executor：**Executor（执行器）接口有两个实现类，其中BaseExecutor有三个继承类分别是BatchExecutor（重用语句并执行批量更新），ReuseExecutor（重用预处理语句prepared statement，跟Simple的唯一区别就是内部缓存statement），SimpleExecutor（默认，每次都会创建新的statement）。以上三个就是主要的Executor。

**MappedStatement：**MappedStatement就是用来存放我们SQL映射文件中的信息包括sql语句，输入参数，输出参数等等。一个SQL节点对应一个MappedStatement对象。

### **Mybatis工作流程：**

![mybaits工作流程](https://img-blog.csdn.net/20180613095608594?watermark/2/text/aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3UwMTA4OTAzNTg=/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70)

下面将通过debug方式对Mybatis进行一步步解析。

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE configuration
  PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
  "http://mybatis.org/dtd/mybatis-3-config.dtd">
<configuration>
<properties resource="jdbc.properties"></properties>
  <settings>
  <setting name="cacheEnabled" value="true"/>
  <setting name="defaultExecutorType" value="REUSE"/>
  </settings>
  <typeAliases>
   <typeAlias alias="User" type="com.ctc.model.User"/>
  </typeAliases>
  <environments default="development">
    <environment id="development">
      <transactionManager type="JDBC"/>
      <dataSource type="POOLED">
        <property name="driver" value="${driver}"/>
        <property name="url" value="${url}"/>
        <property name="username" value="${username}"/>
        <property name="password" value="${password}"/>
      </dataSource>
    </environment>
  </environments>
  <mappers>
    <package name="com.ctc.mapper"/>
  </mappers>
</configuration>
```

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
  PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
  "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.ctc.mapper.UserMapper">
 <cache readOnly="true" size="200" eviction="FIFO"></cache>
 <sql id="select"> select * from user </sql>
  <select id="selectUser" resultMap="selectUserMap" useCache="true">
    <include refid="select"></include> User where id = #{id}
  </select>
  <resultMap type="User" id="selectUserMap">
  	<result property="name" column="username"/>
  </resultMap>
  <insert id="insertUser" useGeneratedKeys="true"  keyProperty="id" keyColumn="id">
   insert into User (username,birthday,sex,address)
   values (#{name},#{birthday},#{sex},#{address})
  </insert>
  <update id="updateUser">
    update User set username = #{username},birthday = #{birthday},
    sex = #{sex},address = #{address} where id = #{id}
  </update>
  <delete id="deleteUser" >
  	delete from User where id = #{id}
  </delete>
  <select id="selectUserByName"  resultMap="selectUserMap">
  	  select * from User where sex = #{param1} 
      <choose>
          <when test="{param2} != null">
             and username like #{param2}
          </when>
      	  <otherwise>and address = #{parma3}</otherwise>
      </choose>
  </select>
  <select id="selectUserCount" resultType="int" >
  	  select count(*) from user where username like #{username}
  </select>
  
  <select id="selectUserNew" resultMap="selectUserMap">
      <bind name="pattern" value="'%' + name + '%'" />
      <include refid="select"></include> 
      <where>
          <if test="pattern !=null">
              username like #{pattern}
          </if>
          <if test="sex !=null">
              and sex = #{sex}
          </if>
          <if test="address != null">
              and address = #{address}
          </if>
      </where>
  </select>
  
  <select id="selectUserByIds" resultMap="selectUserMap">
      <include refid="select"></include>
         where id in
      <foreach collection="list" item="id" index="0" open="(" close=")" separator="," >
         #{id}
      </foreach>
  </select>
</mapper>
```

```java
private static SqlSessionFactory sf;

public static void getSqlSessionFacotry(){
	InputStream inputStream;
	try{
		inputStream = Resources.getResourceAsStream("mybaits-config.xml");
		sf = new SqlSessionFactoryBuilder().build(inputStream);
	}catch(IOException e){
		e.printStackTrace();
	}
}

@org.junit.Test
public void selectUser(){
    SqlSession session = sf.openSession();
    try{
        UserMapper mapper = session.getMapper(UserMapper.class);
        User user = mapper.selectUser(4);
        System.out.println(user);
        User user2= mapper.selectUser(4);
        System.out.println(user2);
    }finally{
        session.close();
    }
}
```

第一步通过SqlSessionFactoryBuilder创建SqlSessionFactory：

​		首先在SqlSessionFactoryBuilder的build（）方法中可以看到MyBatis内部定义了一个类XMLConfigBuilder用来解析配置文件mybatis-config.xml。针对配置文件中的每一个节点进行解析并将数据存放到Configuration这个对象中，紧接着使用带有Configuration的构造方法发返回一个DefautSqlSessionFactory。

```java
  public SqlSessionFactory build(InputStream inputStream) {
    return build(inputStream, null, null);
  }
 
  public SqlSessionFactory build(InputStream inputStream, String environment, Properties properties) {
    try {
      XMLConfigBuilder parser = new XMLConfigBuilder(inputStream, environment, properties);
      //解析mybatis-config.xml
      return build(parser.parse());
    } catch (Exception e) {
      throw ExceptionFactory.wrapException("Error building SqlSession.", e);
    } finally {
      ErrorContext.instance().reset();
      try {
        inputStream.close();
      } catch (IOException e) {
        // Intentionally ignore. Prefer previous error.
      }
    }
  }
    
  //返回SqlSessionFactory，默认使用的是实现类DefaultSqlSessionFactory
  public SqlSessionFactory build(Configuration config) {
    return new DefaultSqlSessionFactory(config);
  }
 
  public Configuration parse() {
    if (parsed) {
      throw new BuilderException("Each XMLConfigBuilder can only be used once.");
    }
    parsed = true;
    //获取根节点configuration
    parseConfiguration(parser.evalNode("/configuration"));
    return configuration;
  }
 
  //开始解析mybatis-config.xml,并把解析后的数据存放到configuration中
  private void parseConfiguration(XNode root) {
    try {
      //保存mybatis-config.xml中的标签setting,本例中开启全局缓存cacheEnabled，设置默认执行器defaultExecutorType=REUSE
      Properties settings = settingsAsPropertiess(root.evalNode("settings"));
      //issue #117 read properties first
      //解析是否配置了外部properties，例如本例中配置的jdbc.propertis
      propertiesElement(root.evalNode("properties"));
      //查看是否配置了VFS，默认没有，本例也没有使用
      loadCustomVfs(settings);
      //查看是否用了类型别名，减少完全限定名的冗余，本例中使用了别名User代替了com.ctc.Model.User
      typeAliasesElement(root.evalNode("typeAliases"));
      //查看是否配置插件来拦截映射语句的执行，例如拦截Executor的Update方法，本例没有使用
      pluginElement(root.evalNode("plugins"))
      //查看是否配置了ObjectFactory，默认情况下使用对象的无参构造方法或者是带有参数的构造方法，本例没有使用
      objectFactoryElement(root.evalNode("objectFactory"));
      //查看是否配置了objectWrapperFatory,这个用来或者ObjectWapper，可以访问：对象，Collection，Map属性。本例没有使用
      objectWrapperFactoryElement(root.evalNode("objectWrapperFactory"));
      //查看是否配置了reflectorFactory,mybatis的反射工具，提供了很多反射方法。本例没有使用
      reflectorFactoryElement(root.evalNode("reflectorFactory"));
      //放入参数到configuration对象中
      settingsElement(settings);
      // read it after objectFactory and objectWrapperFactory issue #631
      //查看数据库环境配置
      environmentsElement(root.evalNode("environments"));
      //查看是否使用多种数据库，本例没有使用
      databaseIdProviderElement(root.evalNode("databaseIdProvider"));
      //查看是否配置了新的类型处理器，如果跟处理的类型跟默认的一致就会覆盖。本例没有使用
      typeHandlerElement(root.evalNode("typeHandlers"));
      //查看是否配置SQL映射文件,有四种配置方式，resource，url，class以及自动扫包package。本例使用package
      mapperElement(root.evalNode("mappers"));
    } catch (Exception e) {
      throw new BuilderException("Error parsing SQL Mapper Configuration. Cause: " + e, e);
    }
  }
 
```

第二步通过SqlSessionFactory创建SqlSession：

```java
  @Override
  public SqlSession openSession() {
    return openSessionFromDataSource(configuration.getDefaultExecutorType(), null, false);
  }
 
 
  private SqlSession openSessionFromDataSource(ExecutorType execType, TransactionIsolationLevel level, boolean autoCommit) {
    Transaction tx = null;
    try {
      //拿到前文从mybatis中解析到的数据库环境配置
      final Environment environment = configuration.getEnvironment();
      final TransactionFactory transactionFactory = getTransactionFactoryFromEnvironment(environment);
      //拿到jdbc的事务管理器，有两种一种是jbc,一种的managed。本例使用的是JdbcTransaction
      tx = transactionFactory.newTransaction(environment.getDataSource(), level, autoCommit);
      //从mybatis配置文件可以看到本例使用了REUSE，因此返回的是ReuseExecutor并把事务传入对象中
      final Executor executor = configuration.newExecutor(tx, execType);
      return new DefaultSqlSession(configuration, executor, autoCommit);
    } catch (Exception e) {
      closeTransaction(tx); // may have fetched a connection so lets call close()
      throw ExceptionFactory.wrapException("Error opening session.  Cause: " + e, e);
    } finally {
      ErrorContext.instance().reset();
    }
  }
 
 
  public Executor newExecutor(Transaction transaction, ExecutorType executorType) {
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
    if (cacheEnabled) {
      executor = new CachingExecutor(executor);
    }
    executor = (Executor) interceptorChain.pluginAll(executor);
    return executor;
  }
 
  //返回一个SqlSession，默认使用DefaultSqlSession
  public DefaultSqlSession(Configuration configuration, Executor executor, boolean autoCommit) {
    this.configuration = configuration;
    this.executor = executor;
    this.dirty = false;
    this.autoCommit = autoCommit;
  }
```

第三步通过SqlSession拿到Mapper对象的代理：

```java
  @Override
  public <T> T getMapper(Class<T> type) {
    return configuration.<T>getMapper(type, this);
  }
 
  public <T> T getMapper(Class<T> type, SqlSession sqlSession) {
    //前文解析Mybatis-config.xml的时候，在解析标签mapper就是用configuration对象的mapperRegistry存放数据
    return mapperRegistry.getMapper(type, sqlSession);
  }
 
  @SuppressWarnings("unchecked")
  public <T> T getMapper(Class<T> type, SqlSession sqlSession) {
    //knownMapper是一个HashMap在存放mapperRegistry的过程中，以每个Mapper对象的类型为Key, MapperProxyFactory 为value保存。
    //例如本例中保存的就是Key:com.ctc.mapper.UserMapper,value就是保存了key的MapperProxyFactory对象
    final MapperProxyFactory<T> mapperProxyFactory = (MapperProxyFactory<T>) knownMappers.get(type);
    if (mapperProxyFactory == null) {
      throw new BindingException("Type " + type + " is not known to the MapperRegistry.");
    }
    try {
      return mapperProxyFactory.newInstance(sqlSession);
    } catch (Exception e) {
      throw new BindingException("Error getting mapper instance. Cause: " + e, e);
    }
  }
 
  public T newInstance(SqlSession sqlSession) {
    //生成一个mapperProxy对象，这个对象实现了InvocationHandler, Serializable。就是JDK动态代理中的方法调用处理器
    final MapperProxy<T> mapperProxy = new MapperProxy<T>(sqlSession, mapperInterface, methodCache);
    return newInstance(mapperProxy);
  }
 
  public MapperProxy(SqlSession sqlSession, Class<T> mapperInterface, Map<Method, MapperMethod> methodCache) {
    this.sqlSession = sqlSession;
    this.mapperInterface = mapperInterface;
    this.methodCache = methodCache;
  }
 
  @SuppressWarnings("unchecked")
  protected T newInstance(MapperProxy<T> mapperProxy) {
    //通过JDK动态代理生成一个Mapper的代理，在本例中的就是UserMapper的代理类，它实现了UserMapper接口
    return (T) Proxy.newProxyInstance(mapperInterface.getClassLoader(), new Class[] { mapperInterface }, mapperProxy);
  }
 
```

第四步通过MapperProxy调用Maper中相应的方法：

```java
  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
   //判断当前调用的method是不是Object中声明的方法，如果是的话直接执行。
   if (Object.class.equals(method.getDeclaringClass())) {
      try {
        return method.invoke(this, args);
      } catch (Throwable t) {
        throw ExceptionUtil.unwrapThrowable(t);
      }
    }
    final MapperMethod mapperMethod = cachedMapperMethod(method);
    return mapperMethod.execute(sqlSession, args);
  }
 
  //把当前请求放入一个HashMap中，一旦下次还是同样的方法进来直接返回。
  private MapperMethod cachedMapperMethod(Method method) {
    MapperMethod mapperMethod = methodCache.get(method);
    if (mapperMethod == null) {
      mapperMethod = new MapperMethod(mapperInterface, method, sqlSession.getConfiguration());
      methodCache.put(method, mapperMethod);
    }
    return mapperMethod;
  }
 
  public Object execute(SqlSession sqlSession, Object[] args) {
    Object result;
    switch (command.getType()) {
      case INSERT: {
        Object param = method.convertArgsToSqlCommandParam(args);
        result = rowCountResult(sqlSession.insert(command.getName(), param));
        break;
      }
      case UPDATE: {
        Object param = method.convertArgsToSqlCommandParam(args);
        result = rowCountResult(sqlSession.update(command.getName(), param));
        break;
      }
      case DELETE: {
        Object param = method.convertArgsToSqlCommandParam(args);
        result = rowCountResult(sqlSession.delete(command.getName(), param));
        break;
      }
      case SELECT:
        if (method.returnsVoid() && method.hasResultHandler()) {
          executeWithResultHandler(sqlSession, args);
          result = null;
        } else if (method.returnsMany()) {
          result = executeForMany(sqlSession, args);
        } else if (method.returnsMap()) {
          result = executeForMap(sqlSession, args);
        } else if (method.returnsCursor()) {
          result = executeForCursor(sqlSession, args);
        } else {
          //本次案例会执行selectOne
          Object param = method.convertArgsToSqlCommandParam(args);
          result = sqlSession.selectOne(command.getName(), param);
        }
        break;
      case FLUSH:
        result = sqlSession.flushStatements();
        break;
      default:
        throw new BindingException("Unknown execution method for: " + command.getName());
    }
    if (result == null && method.getReturnType().isPrimitive() && !method.returnsVoid()) {
      throw new BindingException("Mapper method '" + command.getName() 
          + " attempted to return null from a method with a primitive return type (" + method.getReturnType() + ").");
    }
    return result;
  }
 
  @Override
  public <T> T selectOne(String statement, Object parameter) {
    // Popular vote was to return null on 0 results and throw exception on too many.
    List<T> list = this.<T>selectList(statement, parameter);
    if (list.size() == 1) {
      return list.get(0);
    } else if (list.size() > 1) {
      throw new TooManyResultsException("Expected one result (or null) to be returned by selectOne(), but found: " + list.size());
    } else {
      return null;
    }
  }
 
  @Override
  public <E> List<E> selectList(String statement, Object parameter, RowBounds rowBounds) {
    try {
      MappedStatement ms = configuration.getMappedStatement(statement);
      return executor.query(ms, wrapCollection(parameter), rowBounds, Executor.NO_RESULT_HANDLER);
    } catch (Exception e) {
      throw ExceptionFactory.wrapException("Error querying database.  Cause: " + e, e);
    } finally {
      ErrorContext.instance().reset();
    }
  }
 
  //这边调用的是CachingExecutor类的query,还记得前文解析mybatis-config.xml的时候我们指定了REUSE但是因为在配置文件中开启了缓存
  //所以ReuseExecutor被CachingExecotur装饰，新增了缓存的判断，最后还是会调用ReuseExecutor
  @Override
  public <E> List<E> query(MappedStatement ms, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler) throws SQLException {
    BoundSql boundSql = ms.getBoundSql(parameterObject);
    CacheKey key = createCacheKey(ms, parameterObject, rowBounds, boundSql);
    return query(ms, parameterObject, rowBounds, resultHandler, key, boundSql);
  }
 
  @Override
  public <E> List<E> query(MappedStatement ms, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler, CacheKey key, BoundSql boundSql)
      throws SQLException {
    Cache cache = ms.getCache();
    if (cache != null) {
      flushCacheIfRequired(ms);
      if (ms.isUseCache() && resultHandler == null) {
        ensureNoOutParams(ms, parameterObject, boundSql);
        @SuppressWarnings("unchecked")
        List<E> list = (List<E>) tcm.getObject(cache, key);
        if (list == null) {
          //如果缓存中没有数据则查询数据库
          list = delegate.<E> query(ms, parameterObject, rowBounds, resultHandler, key, boundSql);
          //结果集放入缓存
          tcm.putObject(cache, key, list); // issue #578 and #116
        }
        return list;
      }
    }
    return delegate.<E> query(ms, parameterObject, rowBounds, resultHandler, key, boundSql);
  }
 
 
 
 
```

