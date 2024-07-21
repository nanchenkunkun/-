# 典型回答
Mybatis内置了三种数据源，分别是Pooled，Unpooled和JNDI，其中Pooled数据源是具有连接池的。同时Mybatis也可以使用三方数据源，如Druid，Hikari，C3P0等等<br />Mybatis数据源的类图如下所示：<br />![image.png](https://cdn.nlark.com/yuque/0/2023/png/719664/1684660340953-1acc1590-1b07-4df6-ad1a-9fc15936c9f3.png#averageHue=%232e2e2e&clientId=u68ffff0f-0162-4&from=paste&height=558&id=ub7138c01&originHeight=1116&originWidth=1476&originalType=binary&ratio=2&rotation=0&showTitle=false&size=133076&status=done&style=none&taskId=u8b909667-1d4c-4c64-b2b4-f435daf9662&title=&width=738)<br />可以看到，在Mybatis中，会通过工厂模式来获得对应的数据源，那么Mybatis是在执行的哪一步获取的呢？<br />答案是在执行SQL之前，Mybatis会获取数据库连接Connection，而此时获得的Connection则是应用的启动的时候，已经通过配置项中的文件加载到内存中了：
```xml
<dataSource type="org.apache.ibatis.datasource.pooled.PooledDataSource">
  <property name="driver" value="com.mysql.jdbc.Driver"/>
  <property name="url" value="jdbc:mysql://localhost:3306/mybatis"/>
  <property name="username" value="root"/>
  <property name="password" value="123456"/>
</dataSource>
```
一般情况下，我们不会使用Mybatis默认的PooledDataSource，而是会用Hikari，如果要增加Sql监控功能的话，也可以使用Druid，这是因为自带的数据库连接池有三个缺点：

1. 空闲连接占用资源：连接池维护一定数量的空闲连接，这些连接会占用系统的资源，如果连接池设置过大，那么会浪费系统资源，如果设置过小，则会导致系统并发请求时连接不够用，影响系统性能。
2. 连接池大小调优困难：连接池的大小设置需要根据系统的并发请求量、数据库的性能和系统的硬件配置等因素综合考虑，而这些因素都是难以预测和调整的。
3. 连接泄漏：如果应用程序没有正确关闭连接，那么连接池中的连接就会泄漏，导致连接池中的连接数量不断增加，最终导致系统崩溃。

总的来说，专业的事情交给专业的组件来做，Mybatis功能的核心是ORM映射和缓存，数据库连接池这种东西，市场上已经有比它做的更好的，我们直接用那些更好的就行了。
# 知识扩展
## 什么是数据源，数据库和数据库连接池？
一般应用程序在连接数据库的时候，会有三步：

1. 输入url，name，pwd产生一个connection
2. 然后在这个connection中完成对应的sql操作
3. 完成事务的提交或者回滚

为了对这三步进行抽象，诞生了数据源的概念，一般被定义为DataSource。数据源负责和实体数据库的连接，如（内存数据库，mysql等），所以数据源是被第三方数据库实现的。同时，因为通过数据源对数据库操作做了抽象，我们也可以在数据源中完成对数据库连接的池化，这就是数据库连接池。<br />借用javaDoc对`DataSource`的注释：
> 数据源用于连接到此DataSource对象所表示的物理数据源的工厂。作为DriverManager功能的替代方案，DataSource对象是获取连接的首选方式。实现DataSource接口的对象通常会向基于Java的命名服务注册™ 命名和目录（JNDI）API。
> DataSource接口由驱动程序供应商实现。有三种类型的实现：
> 1. 基本实现--生成一个标准Connection对象
> 2. 连接池实现--生成一个Connection对象，该对象将自动参与连接池。此实现与中间层连接池管理器一起工作。
> 3. 分布式事务实现--生成一个Connection对象，该对象可以用于分布式事务，并且几乎总是参与连接池。此实现使用中间层事务管理器，并且几乎总是使用连接池

