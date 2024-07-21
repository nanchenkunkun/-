# 典型回答

[✅什么是最左前缀匹配？为什么要遵守？](https://www.yuque.com/hollis666/fo22bm/cc9mglopp4nigg59?view=doc_embed)

因为索引底层是一个B+树，如果是联合索引的话，在构造B+树的时候，会先按照左边的key进行排序，左边的key相同时再依次按照右边的key排序。

所以，在通过索引查询的时候，也需要遵守最左前缀匹配的原则，也就是需要从联合索引的最左边开始进行匹配，这时候就要求查询语句的where条件中，包含最左边的索引的值。这也就是最左前缀匹配。

**MySQL一定是遵循最左前缀匹配的，这句话在以前是正确的，没有任何毛病。但是在MySQL 8.0中，就不一定了。因为8.0.13中引入了索引跳跃扫描。**

# 扩展知识

## 索引跳跃扫描

MySQL 8.0.13 版本中，对于range查询（什么是range后面会提到)，引入了索引跳跃扫描（Index Skip Scan）优化，支持不符合组合索引最左前缀原则条件下的SQL，依然能够使用组合索引，减少不必要的扫描。

通过一个例子给大家解释一下，首先有下面这样一张表（参考了MySQL官网的例子，但是我做了些改动和优化）：

```
CREATE TABLE t1 (f1 INT NOT NULL, f2 INT NOT NULL);
CREATE INDEX idx_t on t1(f1,f2);
INSERT INTO t1 VALUES
  (1,1), (1,2), (1,3), (1,4), (1,5),
  (2,1), (2,2), (2,3), (2,4), (2,5);
INSERT INTO t1 SELECT f1, f2 + 5 FROM t1;
INSERT INTO t1 SELECT f1, f2 + 10 FROM t1;
INSERT INTO t1 SELECT f1, f2 + 20 FROM t1;
INSERT INTO t1 SELECT f1, f2 + 40 FROM t1;
```

通过上面的SQL，先创建一张t1表，并把f1,f2两个字段设置为联合索引。之后再向其中插入一些记录。

分别在MySQL 5.7.9和MySQL 8.0.30上执行`EXPLAIN SELECT f1, f2 FROM t1 WHERE f2 = 40;`

![](https://www.hollischuang.com/wp-content/uploads/2022/12/16701385370311.jpg#id=G6o3y&originHeight=296&originWidth=1221&originalType=binary&ratio=1&rotation=0&showTitle=false&status=done&style=none&title=)

可以看到，主要有以下几个区别：

> MySQL 5.7中，type = index，rows=160，extra=Using where;Using index
>  
> MySQL 8.0中，type = range，rows=16，extra=Using where;Using index for skip scan


这里面的type指的是扫描方式，range表示的是范围扫描，index表示的是索引树扫描，通常情况下，range要比index快得多。

从rows上也能看得出来，使用index的扫描方式共扫描了160行，而使用range的扫描方式只扫描了16行。

接着，重点来了，那就是为啥MySQL 8.0中的扫描方式可以更快呢？主要是因为`Using index for skip scan` 表示他用到了索引跳跃扫描的技术。

也就是说，虽然我们的SQL中，没有遵循最左前缀原则，只使用了f2作为查询条件，但是经过MySQL 8.0的优化以后，还是通过索引跳跃扫描的方式用到了索引了。

### 优化原理

**那么他是怎么优化的呢？**

在MySQL 8.0.13 及以后的版本中，`SELECT f1, f2 FROM t1 WHERE f2 = 40;`SQL执行过程如下：

- 获取f1字段第一个唯一值，也就是f1=1
- 构造f1=1 and f2 = 40，进行范围查询
- 获取f1字段第二个唯一值，也就是f1=2
- 构造f1=2 and f2 = 40，进行范围查询
- 一直扫描完f1字段所有的唯一值，最后将结果合并返回

也就是说，最终执行的SQL语句是**像**下面这样的：

```
SELECT f1, f2 FROM t1 WHERE f1 =1 and f2 = 40
UNION
SELECT f1, f2 FROM t1 WHERE f1 =2 and f2 = 40;
```

即，MySQL的优化器帮我们把联合索引中的f1字段作为查询条件进行查询了。

### 限制条件

在知道了索引跳跃扫描的执行过程之后，很多聪明的读者其实就会发现，**这种查询优化比较适合于f1的取值范围比较少，区分度不高的情况（如性别），一旦f1的区分度特别高的话（如出生年月日），这种查询可能会更慢。**

所以，**真正要不要走索引跳跃扫描，还是要经过MySQL的优化器进行成本预估之后做决定的。**

所以，这种优化一般用于那种联合索引中第一个字段区分度不高的情况。但是话又说回来了，我们一般不太会把区分度不高的字段放在联合索引的左边，不过事无绝对，既然MySQL给了一个优化的方案，就说明还是有这样的诉求的。

**但是，我们不能依赖他这个优化，建立索引的时候，还是优先把区分度高的，查询频繁的字段放到联合索引的左边。**

除此之外，在MySQL官网中，还提到索引跳跃扫描还有一些其他的限制条件：

- 表T至少有一个联合索引，但是对于联合索引(A,B,C,D)来说，A和D可以是空的，但B和C必须是非空的。
- 查询必须只能依赖一张表，不能多表join
- 查询中不能使用GROUP BY或DISTINCT语句
- 查询的字段必须是索引中的列
