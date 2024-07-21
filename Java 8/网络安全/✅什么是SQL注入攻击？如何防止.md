# 典型回答

SQL注入是一种常见的网络安全漏洞，攻击者通过在应用程序的用户输入中插入恶意的SQL代码，试图欺骗数据库执行非预期的查询。

SQL注入导致对数据库的未经授权的访问、数据泄露、数据破坏，甚至完整的数据库被攻陷。

攻击者常常通过在用户输入中注入SQL代码，改变应用程序对数据库的查询语句，以实现他们的恶意目的。

假设有一个简单的登录系统，根据用户提供的用户名和密码进行身份验证。应用程序可能会使用以下类似的SQL查询来检查用户是否存在：

```java
String query = "SELECT * FROM users 
WHERE username='" + userInputUsername + "' 
AND password='" + userInputPassword + "'";
```

在这个查询中，`userInputUsername` 和 `userInputPassword` 是从用户输入中获取的值。如果应用程序不正确处理这些输入，它可能容易受到SQL注入攻击。

考虑以下情况，攻击者在用户名和密码字段中输入了恶意的字符串：

```
Username: ' OR '1'='1' --
Password: ' OR '1'='1' --
```

将这些值代入原始的SQL查询，得到的查询变成了：

```sql
SELECT * FROM users WHERE username='' OR '1'='1' --' AND password='' OR '1'='1' --
```

在注入的SQL中，使用`--` 来注释掉他后面的代码，那么我们原来的查询，就会返回用户表中的所有记录，因为 `'1'='1'` 是一个始终为true的条件。

如上，攻击者可以通过注入这样的恶意字符串绕过身份验证，获得对应用程序中所有用户的访问权限，甚至执行其他恶意操作。

如果还只是查询的话影响还不大，万一是一个delete操作被注入了，就可能会导致数据被攻击而导致删除。如以下被注入后的SQL：

```java
DELETE FROM users WHERE username='' OR 1=1; --'
```

# 扩展知识
## 如何防止被SQL注入

**使用预编译语句：** 使用预编译的语句或参数化的语句，而不是通过字符串拼接构建SQL查询。这样可以防止攻击者通过在用户输入中插入恶意代码来改变SQL查询的结构。 

[✅为什么预编译可以避免SQL注入？](https://www.yuque.com/hollis666/fo22bm/dqhumwe8iuvw7eka?view=doc_embed)

如使用JDBC的时候，使用PreparedStatement而不是Statement

```java
// 错误的例子（容易受到SQL注入攻击）：
String userInput = "admin'; DROP TABLE users;--";
String query = "SELECT * FROM users WHERE username='" + userInput + "'";

// 正确的例子（使用预编译语句）：
String userInput = "admin'; DROP TABLE users;--";
String query = "SELECT * FROM users WHERE username=?";
PreparedStatement preparedStatement = connection.prepareStatement(query);
preparedStatement.setString(1, userInput);
```
 <br />**使用ORM框架：** 除了JDBC以外，我们基本都是使用Hibernate或MyBatis这种ORM框架，他们都可以自动处理SQL查询，减少手动拼接SQL的机会。 

在MyBatis中优先使用 #{} 语法而非${}语法，在 MyBatis 中，`#{}`语法会进行预编译，而`${}`语法是直接将参数的值拼接到 SQL 中，容易受到 SQL 注入攻击。因此，尽可能的使用 `#{}`语法。

[✅#和$的区别是什么？什么情况必须用$](https://www.yuque.com/hollis666/fo22bm/idozw647yfbqtkig?view=doc_embed)

**用户输入校验：**永远不要相信用户的输入，我们需要对用户输入进行验证和过滤，确保只有预期的数据被传递给数据库。使用正则表达式或其他合适的方法来检查输入的合法性。 

```java
// 例子：使用正则表达式验证输入是否为合法的用户名
String userInput = request.getParameter("username");

if (userInput.matches("^[a-zA-Z0-9]+$")) {
    // 输入合法，继续处理
} else {
    // 输入非法，拒绝处理
}
```
 <br />**最小权限原则：** 为数据库用户分配最小必要的权限，以限制潜在的损害。不要使用具有过高权限的数据库账户连接数据库。 

**错误消息处理：** 避免向用户泄露敏感信息，例如详细的数据库错误消息。应该是封装成错误码返回到前端，而不是直接把报错的细节返回回去。比如那种唯一性约束冲突之类的，这些系统的内部设计不要报漏出来。<br />综合使用这些方法可以显著提高应用程序对SQL注入攻击的防御能力。
