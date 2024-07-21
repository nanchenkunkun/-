# 典型回答

很多人认为，**用了函数就不能走索引了。**

主要是因为索引是按照列值的原始顺序进行组织和存储的。当对列应用函数时（如进行数学运算、字符串操作或日期函数等），函数操作的结果会改变原始数据的值或格式，这使得数据库无法直接在索引树中定位到这些经过函数转换后的值。因此，数据库不得不回退到全表扫描，以确保能够评估所有行上的函数操作，从而导致查询性能下降。

**但是在MySQL 8.0之后就不一定了，因为有了函数索引，他就是用来优化函数的。**

MySQL 8.0 引入“功能索引”（Functional Indexes）的新特性，也别叫做函数索引。功能索引允许在创建索引时包含列上的表达式，这意味着你可以对数据进行某种计算或转换，并对结果建立索引。这样，即使查询条件中使用了函数操作，仍然可以利用这些索引来优化查询性能。

**函数索引不是直接在表的列上创建的，而是基于列的某个表达式创建的。**这个表达式可以是简单的数学运算，也可以是字符串函数、日期函数等。创建了函数索引后，MySQL 可以在执行涉及该表达式的查询时使用这个索引，从而提高查询效率。

### 使用方式

假设我们有一个employees表，里面有first_name和last_name两个字段，我们希望能够快速查询基于这两个字段合并后的全名。在 MySQL 8.0 中，我们可以创建一个基于first_name和last_name合并后的表达式的函数索引，如下所示：

```
CREATE TABLE employees (
    id INT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(50),
    last_name VARCHAR(50)
);

--创建函数索引
CREATE INDEX idx_full_name ON employees ((CONCAT(first_name, ' ', last_name)));

--插入一条记录
insert into employees(first_name,last_name) values ('Hollis','Chuang');
```

在上述示例中，idx_full_name就是一个函数索引，它基于first_name和last_name字段的组合（即全名）。

这意味着如果你有一个查询是基于员工的全名进行的，这个查询就可以利用idx_full_name索引：

```
SELECT * FROM employees WHERE CONCAT(first_name, ' ', last_name) = 'Hollis Chuang';
```

![image.png](https://cdn.nlark.com/yuque/0/2024/png/5378072/1711774432866-6fb0ec38-023f-4cc2-a22e-a9f6d0a7c174.png#averageHue=%233c3c3c&clientId=u1904606b-c950-4&from=paste&height=179&id=lHl2T&originHeight=179&originWidth=1053&originalType=binary&ratio=1&rotation=0&showTitle=false&size=9134&status=done&style=none&taskId=ua126b44b-84c8-4838-b56e-1d83149cce9&title=&width=1053)

如上图，是执行计划，可以看到，这里就用到了idx_full_name这个索引！

那么也就是说，在这个查询中，即使WHERE子句中使用了CONCAT函数，查询仍然能够利用到idx_full_name函数索引，从而提高查询效率。

### 注意事项

函数索引虽然可以提升查询消息量，但是他的创建可能会增加数据插入、更新和删除时的开销，因为数据库需要维护更多的索引数据。所以也不能无脑创建。

函数索引可以显著提高涉及索引表达式的查询性能，但使用时需要仔细考虑和测试，以确保性能提升符合预期。

另外，在创建函数索引时，需要确保表达式是确定的，即对于给定的输入值总是产生相同的输出值。非确定性表达式不能用于函数索引。

# 扩展知识

## 常见函数索引用法

给大家列举一些常见的函数索引的使用。
### 字符串处理

当你经常需要根据某个字符串列的某部分进行查询时，可以使用函数索引。例如，如果你想根据邮箱的域名部分查询用户，可以创建如下的函数索引：

```sql
CREATE INDEX idx_email_domain ON users ((SUBSTRING_INDEX(email, '@', -1)));
```

这样，当你查询特定域名的邮箱时（如qq邮箱），可以利用这个索引：

```sql
SELECT * FROM users WHERE SUBSTRING_INDEX(email, '@', -1) = 'qq.com';
```

### 日期和时间处理

对于涉及日期和时间处理的查询，函数索引也非常有用。假设你需要频繁查询基于订单日期的年份或月份，可以创建如下索引：

```sql
CREATE INDEX idx_order_year ON orders ((YEAR(order_date)));
CREATE INDEX idx_order_month ON orders ((MONTH(order_date)));
```

这允许你高效地查询特定年份或月份的订单：

```sql
SELECT * FROM orders WHERE YEAR(order_date) = 2022;
SELECT * FROM orders WHERE MONTH(order_date) = 12;
```

### 数学运算

如果查询条件中经常包含对数值列的数学运算，可以针对这些运算创建函数索引。例如，如果你想根据价格的折扣价进行查询，可以创建一个索引：

```sql
CREATE INDEX idx_discounted_price ON products ((price * (1 - discount_rate)));
```

然后，你可以高效地查询特定范围的折扣价格：

```sql
SELECT * FROM products WHERE price * (1 - discount_rate) BETWEEN 50 AND 100;
```

### 使用 JSON 函数

如果你在 MySQL 中使用 JSON 数据类型，并且需要基于 JSON 属性进行查询，可以创建基于 JSON 函数的索引。例如，如果你有一个存储 JSON 数据的列，你可以针对 JSON 文档中的某个键创建索引：

```sql
CREATE INDEX idx_json_key ON orders ((JSON_UNQUOTE(JSON_EXTRACT(order_info, '$.status'))));
```

这样，你可以高效地查询具有特定状态的订单：

```sql
SELECT * FROM orders WHERE JSON_UNQUOTE(JSON_EXTRACT(order_info, '$.status')) = 'shipped';
```

### 大小写不敏感的搜索

如果你需要执行大小写不敏感的字符串搜索，可以创建一个基于`LOWER()`或`UPPER()`函数的索引：

```sql
CREATE INDEX idx_lower_case_name ON customers ((LOWER(name)));
```

这允许你执行大小写不敏感的搜索，而不影响性能：

```sql
SELECT * FROM customers WHERE LOWER(name) = LOWER('John Doe');
```

在使用函数索引时，需要考虑索引的维护成本和性能提升之间的权衡。虽然函数索引可以显著提高特定查询的性能，但它们也会增加插入、更新和删除操作的成本，因为数据库需要维护更多的索引数据。因此，在实际应用中，建议仅对那些经常作为查询条件的列和表达式创建函数索引。
