# 典型回答

MyBatis的RowBounds是一个用于分页查询的简单POJO类，它包含两个属性offset和limit，分别表示分页查询的偏移量和每页查询的数据条数。

**在使用RowBounds进行逻辑分页的时候，我们的SQL语句中是不需要指定分页参数的。**就正常的查询即可，如：

```
<select id="getUsers" resultType="User">
    select * from user
    <where>
        <if test="name != null">
            and name like CONCAT('%',#{name},'%')
        </if>
    </where>
		order by id
</select>
```

然后，在查询的时候，将RowBounds当做一个参数传递：

```
int offset = 10; // 偏移量
int limit = 5; // 每页数据条数
RowBounds rowBounds = new RowBounds(offset, limit);
List<User> userList = sqlSession.selectList("getUserList", null, rowBounds);
```

这样，实际上在查询的时候，将会先所有符合条件的记录返回，然后再在内存中进行分页，分页的方式是根据RowBounds中指定的offset和limit进行数据保留，即抛弃掉不需要的数据再返回。
