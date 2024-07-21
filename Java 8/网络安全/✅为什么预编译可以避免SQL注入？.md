# 典型回答

数据库的预编译（Prepared Statement）是一种数据库查询优化技术，在预编译中，可以先先提交带占位符的 SQL ，MySQL 先将其编译好，然后用户再拿着SQL中定义的占位符对应的参数让 MySQL 去执行。

当一个SQL被预编译之后，预编译的SQL模板中的占位符，比如问号（?），是作为参数的位置标记存在的，而不是作为SQL语句的一部分。无论用户输入什么样的参数，这些参数都会被视为数据，而不会被解释为SQL语句的一部分。

也就是说，用户输入的恶意代码会被当作普通的数据处理，而不会被解释为SQL语句的一部分。如果恶意注入的内容打破了SQL语句的语法结构，数据库可能会在执行阶段产生语法错误，导致查询无法成功执行。这实际上是数据库系统试图解析预编译模板时的结果，因为预编译模板本身的语法是固定的。

举一个简单的例子，加入我们有一个原始SQL：

```
// 原始查询模板
$sql = "SELECT * FROM users WHERE username = ?";
```

当用户输入恶意代码尝试进行SQL注入：

```
$userInput = "hollis'; DROP TABLE users; --";
```

用户期望最终这个SQL变成：

```
SELECT * FROM users WHERE username = 'hollis'; DROP TABLE users; --'
```

会导致数据表被删除。那么，如果用了预编译之后，就不会出现这种问题了。

预编译过程如下：

```
PREPARE stmt FROM 'SELECT * FROM users WHERE username = ?';

EXECUTE stmt USING @userInput;

DEALLOCATE PREPARE stmt;
```
语法参考： [https://dev.mysql.com/doc/refman/8.0/en/sql-prepared-statements.html](https://dev.mysql.com/doc/refman/8.0/en/sql-prepared-statements.html) 

在这种情况下，用户输入包含对SQL注入的尝试（hollis'; DROP TABLE users; --）。然而，由于预处理语句和参数绑定，这个输入被视为字符串而不是可执行的SQL代码。实际执行的查询是：

```
SELECT * FROM users WHERE username ='john_doe\'; DROP TABLE users; --'
```

那么也就是说，用户输入的整个部分，都作为字符串的一部分了，会去数据库中查询username为`john_doe\'; DROPTABLE users; --`的用户，这有效的避免了SQL注入。


# 扩展知识

## Java中使用预编译

在Java中，PreparedStatement是java.sql包中的一个接口，用于执行带有预编译参数的SQL语句。它是Statement接口的子接口，提供了更强大、更安全的SQL执行机制。

以下是使用PreparedStatement的简单用法，可以有效的避免SQL注入：

```
// 用户输入（可能是恶意输入）
String userInput = "hollis'; DROP TABLE users; --";

// 原始查询模板
String sql = "SELECT * FROM users WHERE username = ?";

// 使用PreparedStatement进行预编译
try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
    // 绑定参数
    preparedStatement.setString(1, userInput);

    // 执行查询
    try (ResultSet resultSet = preparedStatement.executeQuery()) {
        // 处理结果（简化起见，只是输出）
        while (resultSet.next()) {
            System.out.println(resultSet.getString("username"));
        }
    }
} catch (SQLException e) {
    e.printStackTrace();
}
```
