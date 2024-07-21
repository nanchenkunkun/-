# 典型回答

MyBatis中可以通过两种方式来实现分页：基于**物理分页**和基于**逻辑分页**。

所谓**物理分页**，指的是最终执行的SQL中进行分页，即SQL语句中带limit，这样SQL语句执行之后返回的内容就是分页后的结果。

所谓**逻辑分页**，就是在SQL语句中不进行分页，照常全部查询，在查询到的结果集中，再进行分页。

在MyBatis中，想要实现分页通常有**四种做法**：

**1、在SQL中添加limit语句：**

```
<select id="getUsers" resultType="User">
    select * from user
    <where>
        <if test="name != null">
            and name like CONCAT('%',#{name},'%')
        </if>
    </where>
    limit #{offset}, #{limit}
</select>

```


**2、基于PageHelper分页插件，实现分页：**

在使用PageHelper时，只需要在查询语句前调用PageHelper.startPage()方法，然后再进行查询操作。PageHelper会自动将查询结果封装到一个PageInfo对象中，包含了分页信息和查询结果。

```
// Java代码中使用 PageHelper
PageHelper.startPage(1, 10);
List<User> userList = userMapper.getUsers();
PageInfo<User> pageInfo = new PageInfo<>(userList);
```

**使用PageHelper时，不需要在mapper.xml文件中使用limit语句。**

**3、基于RowBounds实现分页**

RowBounds是MyBatis中提供的一个分页查询工具，其中可以设置offset和limit用于分页。

```
int offset = 10; // 偏移量
int limit = 5; // 每页数据条数
RowBounds rowBounds = new RowBounds(offset, limit);
List<User> userList = sqlSession.selectList("getUsers", null, rowBounds);
```


**4、基于MyBatis-Plus实现分页**

MyBatis-Plus中提供了分页插件，可实现简单易用的分页功能，可以根据传入的分页参数自动计算出分页信息，无需手动编写分页SQL语句。

```
public interface UserMapper extends BaseMapper<User> {
 List<User> selectUserPage(Page<User> page, @Param("name") String name);
}
```


**以上四种做法中，能实现逻辑分页的是RowBounds和MyBatis-Plus，能实现物理分页的是手动添加limit、PageHelper以及MyBatis-Plus**


物理分页和逻辑分页，工作中推荐使用那种分页呢？

数据小的话无所谓，逻辑分页更简单点，数据量大的话，一定是物理分页，避免查询慢，也避免内存被撑爆、


