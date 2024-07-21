# 典型回答
Hibernate和Mybatis都是ORM框架，都支持JDBC和JTA事务处理，它们创建的目的都是为了简化Java原生程序操作数据库的步骤。增加开发者的开发效率。

不同点有以下几点：

1. **Hibernate是全自动的，Mybatis是半自动的。**在Hibernate当中，开发者只需要定义好数据库的表字段和Java DO的映射关系和规则即可，Hibernate会开放出来接口自动去处理数据库表的CURD，并按照规定好的规则映射到DO对象中，这个过程中操作者是完全不需要感知Sql逻辑的。但是在Mybatis中则完全不是这样，Mybatis不会帮助开发者编写sql逻辑，Mybatis只会按照定义好的规则将数据库字段映射到Java的DO中，但是具体的sql逻辑还是需要开发者自己编写的。

2. 正是因为Mybatis需要自己编写Sql逻辑，这是一个包袱，因为开发者需要根据不同的DB，选择不同的SQL语句（DB移植性不高），而且正是因为要自己写SQL，项目初期的开发工作量要比Hibernate大一点。但是这也是优点，**通过Mybatis开发者就可以定制sql**，譬如多表join，sql优化等操作是Hibernate不具备的

3. Hibernate的缓存系统要优于Mybatis，如果二级缓存出现脏数据，Hibernate会及时报错，但是Mybatis就需要开发者自己去感知

总的来说，MyBatis 是一个小巧、方便、高效、简单、直接、半自动化的持久层框架，Hibernate 是一个强大、方便、高效、复杂、间接、全自动化的持久层框架。

对于性能要求不太苛刻的系统，比如管理系统、ERP 等推荐使用 Hibernate，而对于性能要求高、响应快、灵活的系统则推荐使用 MyBatis。
