### 问题现象

线上定时任务执行失败，排查日志发现有慢SQL，分页扫表扫不动了。

![](media/17054563667330/17054564933625.jpg#id=P6eqx&originalType=binary&ratio=1&rotation=0&showTitle=false&status=done&style=none&title=)![image.png](https://cdn.nlark.com/yuque/0/2024/png/5378072/1705463461215-eaf4485e-06e4-4b44-98b5-2fedc059d812.png#averageHue=%23faf8f8&clientId=u7a86050d-1a66-4&from=paste&height=268&id=ucafe4477&originHeight=536&originWidth=1187&originalType=binary&ratio=2&rotation=0&showTitle=false&size=362352&status=done&style=none&taskId=u601698ff-af14-4999-8c54-2eecd1704a8&title=&width=593.5)

具体SQL如下：

```
SELECT
  *
FROM
  `table_name`
WHERE
  `DELETED` = 0
  AND `STATE`  = "INIT"
  AND `ID` >= 474968311
  AND event_type = ""
ORDER BY
  id
LIMIT
  100
```


### 问题分析
这个SQL用到了4个查询条件，分别是DELETED、STATE、event_type和ID。并且使用ID排序。这条SQL的执行耗时大概在16000ms左右。

先看了一下执行计划：

| 字段名 | 值 |
| --- | --- |
| type | range |
| possible_keys | PRIMARY,idx_event,idx_state_next_time,idx_state_event_deleted |
| key | PRIMARY |
| rows | 8121269 |
| Extra | Using where |


通过这个执行计划可以看出，这里其实有多个索引可以选择，其中`idx_state_event_deleted`中包含了DELETED、STATE、event_type三个字段，但是最终优化器还是选择了PRIMARY主键索引。

按理说主键索引查询，不需要回表，那么效率应该是很高的。但是这里却非常慢。其实通过执行计划也能看出一些东西来：

- rows：表示此操作需要扫描的行数，即扫描表中多少行才能得到结果。这里有8121269，即扫描了800多万行记录。
- Extra：Using where，表示无法通过索引直接获取值，还需要查询结果后进行过滤。

下面这个Using where比较好理解，因为用到了主键索引，但是查询的时候还需要根据DELETED、STATE、event_type等进行筛选， 所以就需要进行where过滤查询。

这里rows有800多万，说明这一次查询需要扫描800多万行记录，但是这个数字是一个估计值，不是实际将要处理的行数，可能不一定准，但是他也很大了，为啥会扫描这么多行呢？分析下这条SQL的执行顺序：

- **使用 PRIMARY 索引查询**: 首先，数据库会使用 PRIMARY 索引来快速定位 ID 大于或等于 474968311 的行。
- **全行读取**: 因为查询要求 SELECT *，这意味着需要获取表中的所有列。所以对于满足所有 WHERE 条件的行，数据库需要从磁盘读取完整的行数据。
- **行过滤**: 数据库将根据 WHERE 条件进行数据过滤（DELETED = 0、STATE = 'INIT' 和 event_type NOT IN ('TRADE_CLAIM', 'CASE_CLEAR')）。
- **排序操作**: 对数据进行排序，由于查询中包含 ORDER BY id，所以这个步骤可能会非常快，因为数据已经根据 ID 排序。
- **限制返回条数**: 最后，数据库将限制结果集只包含前 100 行。由于 ORDER BY 和 LIMIT 的组合，一旦找到了前 100 个符合条件的行，查询可以立即停止，而不需要检查和排序更多的行。

也就是说，这里用了主键索引，会先把所有符合索引条件的行都查出来，并且进行过滤。然后再进行排序，并且在排序过程中选择前100条。

所以，用了主键索引之后，如果`id > 474968311`的数据有很多的话，就会导致这条SQL的扫描行数特别多，于是我统计了一下：

```
select  count(*) FROM
  `collection_case_sync_event` 
  where id > 474968311
```

得到的结果是17677355，也就是说一共有1000多万条记录都符合这个条件。这怎么可能查询的快呢。

那么，这个SQL该如何优化呢？如何让他更快速的执行呢？

其实，这里之所以查询慢，主要是因为一方面用了主键索引、但是主键索引的数据过滤又不多导致的。

那么，想要优化他，就是考虑不让他使用主键索引。这里为啥会用主键索引呢？

其实，主要是因为`ORDER BY id`这个子句，在MySQL的ORDER BY优化（[https://dev.mysql.com/doc/refman/8.0/en/order-by-optimization.html](https://dev.mysql.com/doc/refman/8.0/en/order-by-optimization.html) ）中，有这样的描述：

> In some cases, MySQL may use an index to satisfy an ORDER BY clause and avoid the extra sorting involved in performing a filesort operation.
>  
> The index may also be used even if the ORDER BY does not match the index exactly, as long as all unused portions of the index and all extra ORDER BY columns are constants in the WHERE clause. If the index does not contain all columns accessed by the query, the index is used only if index access is cheaper than other access methods.


也就是说，为了避免额外的排序操作，当SQL语句中有ORDER BY时，如果这个字段有索引，那么优化器为了减少file sort，会愿意选择使用这个索引，因为索引天然有序。

### 问题解决

那么，如果不想让以上扫描过程走主键索引，有以下几个优化手段：

1、FORCE INDEX，可以强制指定不使用主键索引，根据查询条件，可以考虑使用idx_state_event_deleted这个索引。

```

SELECT
  *
FROM
  `table_name` force INDEX(idx_state_event_deleted)
WHERE
  `DELETED` = 0
  AND `STATE`  = "INIT"
  AND `ID` >= 474968311
  AND AND event_type = ""
ORDER BY
  id
LIMIT
  100
```

执行计划如下：

| 字段名 | 值 |
| --- | --- |
| type | range |
| possible_keys | idx_state_event_deleted |
| key | idx_state_event_deleted |
| rows | 215081 |
| Extra | Using index condition; Using filesort |


这样可以看到扫描行数只有20多万，降低了几十倍，查询的耗时大概在100ms左右。从16000ms降低到100ms,性能提升了160倍。

2、修改order by 字段

我们这里用order by 主要是为了分页，因为如果没有order by进行limit的话，会出现重复和丢数据的情况。因为用了ID做排序，所以优化器优先选择了ID索引，那么我们可以考虑换一个字段排序，比如创建时间：

```

SELECT
  *
FROM
  `table_name` 
WHERE
  `DELETED` = 0
  AND `STATE`  = "INIT"
  AND `ID` >= 474968311
  AND event_type = ""
ORDER BY
  `gmt_create` 
LIMIT
  100
```

执行计划如下：

| 字段名 | 值 |
| --- | --- |
| type | range |
| possible_keys | PRIMARY,idx_event,idx_state_next_time,idx_state_event_deleted |
| key | idx_state_event_deleted |
| rows | 222855 |
| Extra | Using index condition; Using filesort |


这样可以看到扫描行数也只有20多万，降低了几十倍，查询的耗时大概在500ms左右。从16000ms降低到500ms,性能提升了30倍。

但是需要注意，如果gmt_create相同的话，可能会出现重复或者丢数据的问题，但是我们这个扫表对这个要求还好，偶尔的重复和缺失影响不太大。

这个优化方案中，如果再把gmt_create也加到idx_state_event_deleted这个联合索引中，那么效果会更好。所以最终我们选择的这个方案
