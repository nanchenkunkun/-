# 典型回答
**MyBatis-Plus支持分页插件——PaginationInnerInterceptor**

**PaginationInnerInterceptor采用的是物理分页方式，物理分页是在数据库中进行分页，即直接在SQL语句中加入LIMIT语句，只查询所需的部分数据。**

物理分页的优点是可以减少内存占用，减轻数据库的负载，缺点是无法对结果进行任意操作，比如说在分页过程中做二次过滤、字段映射、json解析等。

[**PaginationInnerInterceptor**](https://github.com/baomidou/mybatis-plus/blob/3.0/mybatis-plus-extension/src/main/java/com/baomidou/mybatisplus/extension/plugins/inner/PaginationInnerInterceptor.java)这个分页插件就会自动拦截所有的SQL查询请求，计算分页查询的起始位置和记录数，并在SQL语句中加入LIMIT语句。

核心的操作在beforeQuery中：

```

@Override
public void beforeQuery(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
    IPage<?> page = ParameterUtils.findPage(parameter).orElse(null);
    if (null == page) {
        return;
    }

    // 处理 orderBy 拼接
    boolean addOrdered = false;
    String buildSql = boundSql.getSql();
    List<OrderItem> orders = page.orders();
    if (CollectionUtils.isNotEmpty(orders)) {
        addOrdered = true;
        buildSql = this.concatOrderBy(buildSql, orders);
    }

    // size 小于 0 且不限制返回值则不构造分页sql
    Long _limit = page.maxLimit() != null ? page.maxLimit() : maxLimit;
    if (page.getSize() < 0 && null == _limit) {
        if (addOrdered) {
            PluginUtils.mpBoundSql(boundSql).sql(buildSql);
        }
        return;
    }

    handlerLimit(page, _limit);
    IDialect dialect = findIDialect(executor);

    final Configuration configuration = ms.getConfiguration();
    DialectModel model = dialect.buildPaginationSql(buildSql, page.offset(), page.getSize());
    PluginUtils.MPBoundSql mpBoundSql = PluginUtils.mpBoundSql(boundSql);

    List<ParameterMapping> mappings = mpBoundSql.parameterMappings();
    Map<String, Object> additionalParameter = mpBoundSql.additionalParameters();
    model.consumers(mappings, configuration, additionalParameter);
    mpBoundSql.sql(model.getDialectSql());
    mpBoundSql.parameterMappings(mappings);
}

```

其中比较关键的就是第31行，buildPaginationSql方法。这里不同的数据库有不同的实现，我们看一下MySQL的实现：

```
public class MySqlDialect implements IDialect {

    @Override
    public DialectModel buildPaginationSql(String originalSql, long offset, long limit) {
        StringBuilder sql = new StringBuilder(originalSql).append(" LIMIT ").append(FIRST_MARK);
        if (offset != 0L) {
            sql.append(StringPool.COMMA).append(SECOND_MARK);
            return new DialectModel(sql.toString(), offset, limit).setConsumerChain();
        } else {
            return new DialectModel(sql.toString(), limit).setConsumer(true);
        }
    }
}
```

这段代码就比较好理解了，其实就是在原来的SQL后面拼上`LIMIT ?,?` ，这样在后续执行的过程中，就可以把offerset和limit赋值给这两个占位符，实现分页查询了。

# 扩展知识

## 使用方法

使用mybatis-plus实现分页，挺简单的，参考以下步骤：

**添加分页插件**：首先，在 MyBatis-Plus 的配置中添加分页插件，在 Spring Boot 应用中，可以这样配置：

```
@Configuration
@MapperScan("scan.your.mapper.package")
public class MybatisPlusConfig {

    /**
     * 添加分页插件
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));//如果配置多个插件,切记分页最后添加
        return interceptor;
    }
}
```

**编写 Mapper 接口**：定义一个 Mapper 接口，用于执行数据库操作。这个接口不需要特别指定分页相关的方法，MyBatis-Plus 会自动处理。

```
public interface UserMapper extends BaseMapper<User> {
    // 这里可以添加其他数据库操作的方法
}
```

**执行分页查询**：在服务层或者控制器层，使用 MyBatis-Plus 提供的 Page 类来执行分页查询。例如，要查询第 1 页的数据，每页显示 10 条记录，可以这样写：

```
@Autowired
private UserMapper userMapper;

public IPage<User> selectUserPage(int currentPage, int pageSize) {
    Page<User> page = new Page<>(currentPage, pageSize);
    IPage<User> userPage = userMapper.selectPage(page, null);
    return userPage;
}
```

selectPage 方法是 MyBatis-Plus 提供的内置方法，用于执行分页查询。null 作为第二个参数表示没有查询条件，即查询所有记录。

selectPage 方法返回的 IPage 对象包含了分页信息（如当前页码、总页数、每页记录数、总记录数等）和查询结果。

总页数、总记录数等的统计是在PaginationInnerInterceptor的willDoQuery方法中实现的，具体大家自行看下代码就可以了，比较容易理解，这里就不展开了。
