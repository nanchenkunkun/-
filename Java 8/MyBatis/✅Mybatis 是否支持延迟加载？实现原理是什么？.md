# 典型回答

**MyBaits支持延迟加载，延迟加载允许在需要时按需加载关联对象，而不是在查询主对象时立即加载所有关联对象。这样做可以提高查询性能和减少不必要的数据库访问。**

假设，我们有两张表，分别是订单表和商品项表，一个订单中可以关联多个商品项。

```java
public class Order {
  private int id;
  private String orderNumber;
  private List<Item> items; // 关联的商品项列表

  // 省略构造函数和getter/setter方法
}

public class Item {
  private int id;
  private int orderId;
  private String itemName;
  private BigDecimal price;

  // 省略构造函数和getter/setter方法
}
```

当我们从数据库中查询Order的时候，如果同时把关联的Item都返回，这就不是延迟加载，如果在后面真正要用到Item的时候再查询加载，这就是延迟加载。

延迟加载的主要原理就是当开启了延迟加载功能时，当查询主对象时，MyBatis会生成一个**代理对**象，并将代理对象返回给调用者。

当后面需要访问这些关联对象时，代理对象会检查关联对象是否已加载。如果未加载，则触发额外的查询。

查询结果返回后，MyBatis会将关联对象的数据填充到代理对象中，使代理对象持有关联对象的引用。这样，下次访问关联对象时，就可以直接从代理对象中获取数据，而无需再次查询数据库。


# 扩展知识

## 配置延迟加载
在 MyBatis 中，关于延迟加载的配置可以分为全局配置和局部配置。默认延迟加载是不开启的。
### 全局配置

全局配置影响整个 MyBatis 会话，通常在 `mybatis-config.xml` 中设置。这些设置会应用于所有的 SQL 映射，除非在映射文件中对某些操作进行了覆盖。以下是全局延迟加载的配置示例：

```xml
<settings>
    <!-- 启用延迟加载 -->
    <setting name="lazyLoadingEnabled" value="true"/>
    <!-- 只有当使用属性时，才加载对象的所有延迟加载属性 -->
    <setting name="aggressiveLazyLoading" value="false"/>
</settings>
```

- `lazyLoadingEnabled`：是否启用延迟加载。设置为 `true` 表示启用。
- `aggressiveLazyLoading`：当此选项设置为 `false` 时，只有直接引用的属性才会触发加载。这防止了因访问非延迟加载属性而导致的不必要的加载。

### 局部配置

局部配置是在具体的映射文件中进行的，针对单独的操作或关联定义。你可以在映射文件中针对特定的查询或关联覆盖全局设置，例如在 `<association>` 或 `<collection>` 中指定 `fetchType`。

这里是一个映射文件中的例子，展示了如何配置延迟加载：

```xml
<resultMap id="blogResultMap" type="Blog">
    <association property="author" javaType="Author" select="selectAuthorById" fetchType="lazy"/>
</resultMap>

<select id="selectAuthorById" resultType="Author">
    SELECT * FROM author WHERE id = #{id}
</select>
```

在这个例子中，`<association>` 标签的 `fetchType` 属性设置为 `lazy`，意味着 `author` 属性将按需加载，而不是在加载博客数据时立即加载。

