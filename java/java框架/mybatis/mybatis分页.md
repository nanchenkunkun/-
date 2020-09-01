# 使用MyBatis框架分页的五种方式

#### 初始准备

1. 创建分页对象类，方便模块间传值

   ```
   //PageInfo.java
   import lombok.Data;
   
   @Data
   public class PageInfo {
   
       private int pageNo;
   
       private int pageSize;
   
   }
   
   ```

   2.定义DAO接口

   ```
   import org.apache.ibatis.session.RowBounds;
   import org.springframework.stereotype.Repository;
   
   import java.util.List;
   
   @Repository
   public interface UserMapper {
   
       User selectByUser(User user);
   
       List<User> selectAll();
   
       List<User> selectByPageInfo(PageInfo info);
   
       List<User> selectByInterceptor(PageInfo info);
   
       List<User> selectByRowBounds(RowBounds rowBounds);
   
   }
   
   ```

   3.mapper中定义结果集合BaseResult

   ```
   <resultMap id="BaseResult" type="cn.edu.yau.pojo.User">
           <id property="id" column="id" jdbcType="INTEGER"></id>
           <result property="username" column="username" jdbcType="VARCHAR"></result>
           <result property="password" column="password" jdbcType="VARCHAR"></result>
    </resultMap>
   
   ```

   #### 一、原始切分：最原始方法，不建议使用

1. mapper代码:查询所有数据

```
<select id="selectAll" resultMap="BaseResult">
    select * from tb_user
</select>

```



2.业务层代码:利用List的subList()方法对数据进行切片

```
    public List<User> findByAll(PageInfo info) {
        List<User> users = userMapper.selectAll();
        return users.subList(info.getPageNo(), info.getPageSize());
    }

```



3.控制层代码

```
    @RequestMapping(value = "/userlist_1", method = RequestMethod.GET)
    @ResponseBody
    public Result findUserBySubList(PageInfo info) {
        List<User> users = userService.findByAll(info);
        if(users.size() == 0) {
            return ResultGenerator.genFailResult("未查找到用户");
        }
        return ResultGenerator.genSuccessResult(users);
    }

```



#### 二、LIMIT关键字

直接利用 sql分页

mapper代码:利用limit关键字实现分页

```
    <select id="selectByPageInfo" resultMap="BaseResult">
        select * from tb_user limit #{pageNo}, #{pageSize}
    </select>

```



#### 三、RowBounds实现分页

这种分页是逻辑分页，不是物理分页，当数据量大的时候不建议使用

```java
   public List<User> findByRowBounds(PageInfo info) {
        return userMapper.selectByRowBounds(new RowBounds(info.getPageNo(), info.getPageSize()));
    }
123
```



#### 四、MyBatis的Interceptor实现:实现复杂，需要明白MyBatis的实现

1. 创建Interceptor

   ```
   import org.apache.ibatis.executor.parameter.ParameterHandler;
   import org.apache.ibatis.executor.statement.StatementHandler;
   import org.apache.ibatis.mapping.MappedStatement;
   import org.apache.ibatis.plugin.*;
   import org.apache.ibatis.reflection.MetaObject;
   import org.apache.ibatis.reflection.SystemMetaObject;
   
   import java.sql.Connection;
   import java.util.Properties;
   
   /**
    *  利用MyBatis拦截器进行分页
    *
    *  @Intercepts 说明是一个拦截器
    *  @Signature 拦截器的签名
    *  type 拦截的类型 四大对象之一( Executor,ResultSetHandler,ParameterHandler,StatementHandler)
    *  method 拦截的方法
    *  args 参数,高版本需要加个Integer.class参数,不然会报错
    *
    */
   @Intercepts({@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})})
   public class DefinedPageInterceptor implements Interceptor {
   
       @Override
       public Object intercept(Invocation invocation) throws Throwable {
           //获取StatementHandler,默认的是RoutingStatementHandler
           StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
           //获取StatementHandler的包装类
           MetaObject metaObject = SystemMetaObject.forObject(statementHandler);
           //分隔代理对象
           while (metaObject.hasGetter("h")) {
               Object obj = metaObject.getValue("h");
               metaObject = SystemMetaObject.forObject(obj);
           }
           while (metaObject.hasGetter("target")) {
               Object obj = metaObject.getValue("target");
               metaObject = SystemMetaObject.forObject(obj);
           }
           //获取查看接口映射的相关信息
           MappedStatement mappedStatement = (MappedStatement) metaObject.getValue("delegate.mappedStatement");
           String mapId = mappedStatement.getId();
           //拦截以ByInterceptor结尾的请求，统一实现分页
           if (mapId.matches(".+ByInterceptor$")) {
               System.out.println("LOG:已触发分页拦截器");
               //获取进行数据库操作时管理参数的Handler
               ParameterHandler parameterHandler = (ParameterHandler) metaObject.getValue("delegate.parameterHandler");
               //获取请求时的参数
               PageInfo info = (PageInfo) parameterHandler.getParameterObject();
               //获取原始SQL语句
               String originalSql = (String) metaObject.getValue("delegate.boundSql.sql");
               //构建分页功能的SQL语句
               String sql = originalSql.trim() + " limit " + info.getPageNo() + ", " + info.getPageSize();
               metaObject.setValue("delegate.boundSql.sql", sql);
           }
           //调用原对象方法，进入责任链下一级
           return invocation.proceed();
       }
   
       @Override
       public Object plugin(Object target) {
           //生成Object对象的动态代理对象
           return Plugin.wrap(target, this);
       }
   
       @Override
       public void setProperties(Properties properties) {
           //如果分页每页数量是统一的，可以在这里进行统一配置，也就无需再传入PageInfo信息了
       }
   }
   
   
   ```

   

2.将Interceptor添加至MyBatisConfig中，这里采用JavaConfig的方式

```
    @Bean
    public SqlSessionFactoryBean sqlSession() {
        SqlSessionFactoryBean sqlSession = new SqlSessionFactoryBean();
        sqlSession.setDataSource(dataSource());
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver().getResources("classpath:mapper/*.xml");
            sqlSession.setMapperLocations(resources);
            //配置自定义的Interceptro作为MyBatis的Interceptor,完成分页操作
            DefinedPageInterceptor definedPageInterceptor = new DefinedPageInterceptor();
            sqlSession.setPlugins(new Interceptor[]{definedPageInterceptor});
            return sqlSession;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

```



3.DAO层接口方法名需要和代码中自定义的".+ByInterceptor$"正则表达式相匹配,mapper的书写依然是查询所有数据

```
    <select id="selectByInterceptor" resultMap="BaseResult">
        select * from tb_user
    </select>

```



#### 五、开源项目PageHelper实现：本质还是自己封装了个Interceptor

1. 引入PageHelper的jar包

```xml
    <dependency>
        <groupId>com.github.pagehelper</groupId>
        <artifactId>pagehelper</artifactId>
        <version>5.1.10</version>
    </dependency>

```

1. 配置PageInterceptor

```java
    public PageInterceptor initPageInterceptor(){
        PageInterceptor pageInterceptor = new PageInterceptor();
        Properties properties = new Properties();
        //设置数据库类型
        properties.setProperty("helperDialect", "mysql");
        //该参数默认为false
        //设置为true时，会将RowBounds第一个参数offset当成pageNum页码使用
        //和startPage中的pageNum效果一样
        properties.setProperty("offsetAsPageNum", "true");
        //该参数默认为false
        //设置为true时，使用RowBounds分页会进行count查询
        properties.setProperty("rowBoundsWithCount", "true");
        pageInterceptor.setProperties(properties);
        return pageInterceptor;
    }

    @Bean
    public SqlSessionFactoryBean sqlSession() {
        SqlSessionFactoryBean sqlSession = new SqlSessionFactoryBean();
        sqlSession.setDataSource(dataSource());
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver().getResources("classpath:mapper/*.xml");
            sqlSession.setMapperLocations(resources);
            //配置PageHelper作为MyBatis的Interceptor，完成分页操作
            PageInterceptor pageInterceptor = this.initPageInterceptor();
            //配置自定义的Interceptro作为MyBatis的Interceptor,完成分页操作
            DefinedPageInterceptor definedPageInterceptor = new DefinedPageInterceptor();
            sqlSession.setPlugins(new Interceptor[]{pageInterceptor, definedPageInterceptor});
            return sqlSession;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

```

1. mapper依然是查询所有数据
2. 为DAO层再封装一次方法

```java
@Repository
public class PageHelperHandler {

    @Autowired
    private SqlSessionFactory sqlSessionFactory;

    public List<User> findByPageHelper(PageInfo info) {
        SqlSession session = sqlSessionFactory.openSession();
        PageHelper.startPage(info.getPageNo(), info.getPageSize());
        //写到要使用到的类名和方法名
        List<User> users = session.selectList("cn.edu.yau.mapper.UserMapper.selectAll");
        return users;
    }

}

```

