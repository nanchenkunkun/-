# 典型回答

之所以不建议使用join查询，最主要的原因就是join的效率比较低。

MySQL是使用了嵌套循环（Nested-Loop Join）的方式来实现关联查询的，简单点说就是要通过两层循环，用第一张表做外循环，第二张表做内循环，外循环的每一条记录跟内循环中的记录作比较，符合条件的就输出。

而具体到算法实现上主要有simple nested loop，block nested loop和index nested loop这三种。而且这三种的效率都没有特别高。

MySQL是使用了嵌套循环（Nested-Loop Join）的方式来实现关联查询的，如果有2张表join的话，复杂度最高是O(n^2)，3张表则是O(n^3)...随着表越多，表中的数据量越多，JOIN的效率会呈指数级下降。

PS：MySQL 8.0中新增了 hash join算法：

[✅MySQL的Hash Join是什么？](https://www.yuque.com/hollis666/fo22bm/ci3ae75ktzkmz1dw?view=doc_embed)

# 扩展知识

## join

在MySQL 中，可以使用 JOIN 在两个或多个表中进行联合查询，join有三种，分别是inner join、left join 和 right join。

- INNER JOIN（内连接,或等值连接）：获取两个表中字段匹配关系的记录。
   - 取两个表的交集部分
- LEFT JOIN（左连接）：获取左表所有记录，即使右表没有对应匹配的记录。
   - 取两个表的交集部分+左表中的数据
- RIGHT JOIN（右连接）： 与 LEFT JOIN 相反，用于获取右表所有记录，即使左表没有对应匹配的记录。
   - 取两个表的交集部分+右表中的数据

在配合join一起使用的还有on关键字，用来指明关联查询的一些条件。
## 嵌套循环算法

MySQL是使用了嵌套循环（Nested-Loop Join）的方式来实现关联查询的，具体到算法上面主要有simple nested loop join，block nested loop join和index nested loop join这三种。

而这三种的效率都没有特别高。

- simple nested loop，他的做法简单粗暴，就是全量扫描连接两张表进行数据的两两对比，所以他的复杂度可以认为是N*M
> N是驱动表的数量，M是被驱动表的数量

- index nested loop，当Inner Loop的表用到字段有索引的话，可以用到索引进行查询数据，因为索引是B+树的，复杂度可以近似认为是N*logM。
- block nested loop，其实是引入了一个Buffer，会提前把外循环的一部分结果提前放到JOIN BUFFER中，然后内循环的每一行都和整个buffer的数据作比较。虽然比较次数还是N*M，但是因为join buffer是基于内存的，所以效率高很多。

所以，虽然MySQL已经尽可能的在优化了，但是这几种算法复杂度都还是挺高的，这也是为什么不建议在数据库中多表JOIN的原因。随着表越多，表中的数据量越多，JOIN的效率会呈指数级下降。
### 不能用join如何做关联查询

如果不能通过数据库做关联查询，那么需要查询多表的数据的时候要怎么做呢？

主要有两种做法：

1、在内存中自己做关联，即先从数据库中把数据查出来之后，我们在代码中再进行二次查询，然后再进行关联。

2、数据冗余，那就是把一些重要的数据在表中做冗余，这样就可以避免关联查询了。

3、宽表，就是基于一定的join关系，把数据库中多张表的数据打平做一张大宽表，可以同步到ES或者干脆直接在数据库中直接查都可以
