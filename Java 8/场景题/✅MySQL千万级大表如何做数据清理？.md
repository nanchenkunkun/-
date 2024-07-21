# 典型回答

当我们要清理表中的历史数据时，一般都是通过时间来进行判断的，执行delete的语句如下：

```java
DELETE FROM table_hollis WHERE `gmt_create` < SUBDATE(CURDATE(),INTERVAL 300 DAY);
```

如上SQL，就是删除300天之前的数据，如果是小表的话，执行这个SQL没啥问题，但是如果是大表，如果表中的数据量达到千万级别的话，就会有问题了。

像以上这样的SQL，如果没有在gmt_create字段上创建索引，那么**delete操作就会进行全表扫描，进行大范围的加锁**，甚至效果相当于锁表，而锁表给业务带来的影响就是业务都无法进行写操作了，这肯定是无法接受的。

而且，即使业务说我可以允许锁表，上面的操作也有可能会失败，因为**数据库会对单条SQL产生的bin_log有大小是有限制的**，删除这么大量的数据，产生的日志大小如果超过该阈值，最终还是会失败！

> max_binlog_cache_size参数指定了单个事务最大允许使用的Binlog，当超出这个值时，会出现报错：Multi-statement transaction required more than 'max_binlog_cache_size' bytes of storage; increase this mysqld variable and try again


而且，删除操作还涉及到磁盘IO，如果要删除的数据太多，就会导致频繁的IO，对数据也会造成一定的压力。

还有就是，数据的删除过程，也会伴随着索引更新，大量的数据删除操作，会因为频繁的索引重建而导致业务无法进行写操作。

那么，怎么解决呢？如何实现高效、安全的大表的批量删除呢？

这里可以参考阿里云DMS的数据清理功能的方案（我司内部的数据库用的就是你这个方案——[https://help.aliyun.com/document_detail/162507.html](https://help.aliyun.com/document_detail/162507.html) ）

他的做法总结一句话就是：DMS在清理数据时会扫描全表，根据主键或非空唯一键分批执行。

1、获取要做数据清理的表的主键，或者非空唯一键的最大值和最小值。

如：
```java
select min(id) as min_id,max(id) as max_id from table_hollis;
```

假如我们得到min_id = 100，max_id = 100000；

2、分段取出第一个区间的所有数据，默认区间可能是1000，也可以根据binlog配置等进行调整。

```sql
select id,( //查出符合条件的数据
            select 1 from (
              //查出第一个区间的所有gmt_create
              select gmt_create from table_hollis
              where id >= 100 and id <= 100000 
              order by id asc limit 1000
            ) t where gmt_create < SUBDATE(CURDATE(),INTERVAL 300 DAY) limit 1
          )  as hasNeedDelItem
from table_hollis
where id >= 100 and id <= 100000 
order by id asc limit 1000;
```
这段 SQL 代码的主要目的是查询出表 `table_hollis` 中 `id` 值在 100 到 100000 之间的记录，并为每条记录增加一个额外的字段 `hasNeedDelItem`。这个字段用于标识是否存在一个条件满足的记录。以下是详细的逐步解析：

1.  **外层查询**： 
```sql
select id, (...) as hasNeedDelItem
from table_hollis
where id >= 100 and id <= 100000 
order by id asc limit 1000;
```
<br />这部分查询 `table_hollis` 表中 `id` 在 100 到 100000 范围内的前 1000 条记录。查询的结果包括每条记录的 `id` 和一个名为 `hasNeedDelItem` 的计算字段。 

2.  **内层查询**（计算字段 `hasNeedDelItem`）： 
```sql
(
  select 1 from (
    select gmt_create from table_hollis
    where id >= 100 and id <= 100000 
    order by id asc limit 1000
  ) t where gmt_create < SUBDATE(CURDATE(), INTERVAL 300 DAY) limit 1
) as hasNeedDelItem
```
 

   -  **子查询**： 
```sql
select gmt_create from table_hollis
where id >= 100 and id <= 100000 
order by id asc limit 1000
```
<br />这个子查询从同样的 `table_hollis` 表中选取 `id` 在同一范围内的记录，并提取这些记录的 `gmt_create` 字段。结果集限制为前 1000 条记录。 

   -  **条件检查**： 
```sql
where gmt_create < SUBDATE(CURDATE(), INTERVAL 300 DAY)
```
<br />在子查询结果的基础上，这个条件进一步检查 `gmt_create` 是否小于当前日期向前推算 300 天的日期。这里使用的 `SUBDATE` 函数用于日期的计算。 

   -  **最终选择**： 
```sql
select 1 (...) limit 1
```
<br />如果找到任何 `gmt_create` 满足上述条件的记录，这个查询就返回数字 `1`。由于使用了 `limit 1`，查询最多返回一条记录。如果没有找到任何符合条件的记录，则不返回任何结果。 

3.  **结果解释**：

<br />`hasNeedDelItem` 字段对于每个符合外层查询条件的 `id`，都会检查是否存在 `gmt_create` 日期小于当前日期 300 天的记录。如果存在，`hasNeedDelItem` 为 1（即记录需要被删除的标识），否则为 NULL（因为没有记录返回 `1`）。 

这样的 SQL 逻辑通常用于标记或检索需要基于某些时间条件进行处理或删除的数据记录。

这样，在按照ID删除的时候，就可以用到主键索引，进行删除，而且因为做了分批，也不会一次性删除大量数据。

在阿里云MDS的数据清理功能中，还可以设置开始执行时间和结束执行时间，只有在这个时间范围内才会执行，如果超过了这个时间，就不再执行了。也可以避免数据清理导致线上数据库不可用！
