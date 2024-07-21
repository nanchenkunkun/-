# 典型回答

Mybatis通过ResultSet对象来获取SQL查询返回的结果集，然后将结果集中的每行记录映射到Java对象中。在字段映射过程中，Mybatis提供了以下几种方式：

1. **使用列名映射**：Mybatis默认使用列名来映射查询结果集中的列与Java对象中的属性。如果列名和Java对象属性名不完全一致，可以通过在SQL语句中使用“AS”关键字或使用别名来修改列名。

2. **使用别名映射**：如果查询语句中使用了别名，则Mybatis会优先使用列别名来映射Java对象属性名，而不是列名。

3. **使用ResultMap映射**：ResultMap是Mybatis用来映射查询结果集和Java对象属性的关系。可以在映射文件中定义ResultMap，指定Java对象和列之间的映射关系。通过ResultMap，可以实现复杂的字段映射关系和转换。

4. **自定义TypeHandler映射**：如果默认的字段映射方式无法满足需求，可以通过实现TypeHandler接口来自定义字段映射规则。TypeHandler可以将查询结果集中的列类型转换为Java对象属性类型，并将Java对象属性类型转换为SQL类型。可以通过在映射文件中定义TypeHandler，来实现自定义映射。

总之，Mybatis提供了多种灵活的字段映射方式，可以满足不同场景下的需求。


# 扩展知识

## 字段映射的过程及原理

Mybatis实现字段映射的代码主要在**ResultSetHandler**类中。该类是Mybatis查询结果集处理的核心类，负责将JDBC ResultSet对象转换为Java对象，并进行字段映射。

Mybatis实现字段映射的原理可以简单描述为以下几个步骤：

1. Mybatis通过JDBC API向数据库发送SQL查询语句，并**获得查询结果集**。

2. 查询结果集中的所有数据**封装到一个ResultSet对象中**，Mybatis**遍历ResultSet对象中的数据**。

3. 对于每一行数据，Mybatis根据Java对象属性名和查询结果集中的列名进行匹配。如果匹配成功，则将查询结果集中的该列数据映射到Java对象的相应属性中。

4. 如果Java对象属性名和查询结果集中的列名不完全一致，Mybatis可以通过在SQL语句中使用“AS”关键字或使用别名来修改列名，或者使用ResultMap来定义Java对象属性和列的映射关系。

5. 对于一些复杂的映射关系，例如日期格式的转换、枚举类型的转换等，可以通过自定义TypeHandler来实现。Mybatis将自定义TypeHandler注册到映射配置中，根据Java对象属性类型和查询结果集中的列类型进行转换。

6. 最终，Mybatis将所有映射成功的Java对象封装成一个List集合，返回给用户使用。

总之，Mybatis通过查询结果集中的列名和Java对象属性名之间的映射关系，将查询结果集中的数据映射到Java对象中。Mybatis提供了多种灵活的映射方式，可以满足不同场景下的需求。


