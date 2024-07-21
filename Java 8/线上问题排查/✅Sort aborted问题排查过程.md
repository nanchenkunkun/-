### 问题发现
我们有一个定时任务，是做扫表的，但是最近经常出现定时任务扫表处理失败的报警，登录到机器上之后，发现有数据库层面的报错：

```
Caused by: com.taobao.tddl.common.exception.TddlRuntimeException: ERR-CODE: [TDDL-4614][ERR_EXECUTE_ON_MYSQL] Error occurs when execute on GROUP 'FIN_RISK_XXX_GROUP' ATOM 'cn-zhangjiakou_i-xxxxx_fin_risk_xxx_3028': Sort aborted: Query execution was interrupted More: [http://xxx.alibaba-inc.com/faq/faqByFaqCode.ht
ml?faqCode=XXX-4614]
```

以上日志我做了简单的脱敏，其中最重要的就是这句：

```
Sort aborted: Query execution was interrupted
```

这是一个数据库查询执行过程中的错误信息，通常在数据库系统中会出现。**这个错误消息表示数据库查询中的排序操作被中断或终止了。**

### 问题排查

这个问题的发生，一般来说有几个原因：

1、慢SQL导致查询超时，这时候就会为了避免数据库链接被长时间占用而中断此次查询

2、查询被手动终止，数据库管理员手动中止正在执行的查询操作也会出现这个异常

3、资源不足，查询排序操作可能需要大量的计算和内存资源，如果数据库服务器的资源不足以执行排序操作，那么查询可能会被中断。这可能发生在高负载或资源不足的环境中。


主要就是以上这三个原因，接下来我们分析了一下导致失败的SQL	语句，这个语句在上面的ERROR日志打印的同时就已经给打印出来了，我隐藏了一些无关紧要的内容，大致SQL如下：

```sql
### The error occurred while setting parameters### SQL: 
select
  business_type_enum,
  product_type_enum,
  subject_id,
  subject_id_enum,
  GROUP_CONCAT(distinct (number) SEPARATOR ',') as risk_case_numbers,
  GROUP_CONCAT(distinct (risk_level_enum) SEPARATOR ',') as risk_level_enums,
from
  fraud_risk_case
WHERE
  product_type_enum = ?
  and risk_case_status_enum = 'DRAFT'
  and subject_id like "23%"
group by
  subject_id_enum,
  subject_id
limit
  ?, ?
```

大致就是基于product_type_enum，risk_case_status_enum，subject_id做了条件查询，并且基于 subject_id_enum、subject_id两个字段做了一下分组。

看了一下这条SQL的执行计划：

```sql
+----+-------+------------------------------+---------------------+----------------------------------------------------+                                           
| id | type  | possible_keys                | key                 | Extra                                              |                                           
+----+-------+------------------------------+---------------------+----------------------------------------------------+                                           
|  1 | range | idx_subject_product          | idx_subject_product | Using index condition; Using where; Using filesort |                                           
+----+-------+------------------------------+---------------------+----------------------------------------------------+ 
```


通过这个SQL我们可以分析出来，其实它是走了索引的，命中了idx_subject_product这个索引，其中包含了subject_id和product_type_enum字段。

并且这条SQL因为用到了group by所以需要进行排序，但是并没有用到索引排序，而是基于filesort进行的。

[✅order by 是怎么实现的？](https://www.yuque.com/hollis666/fo22bm/caou56?view=doc_embed)

那么，首先想到的优化方式就是提升这个sort的性能。

### 问题解决

想要使用到索引进行排序，就需要建立一个subject_id_enum, subject_id两个字段的联合索引，但是又要兼顾where条件的查询性能。所以也需要考虑product_type_enum，risk_case_status_enum等字段。

但是因为这里面risk_case_status_enum是一个常量值进行where判断的，并且根据业务分析，risk_case_status_enum = 'DRAFT'的数据量在这张表中占比非常小，所以那么就很容易想到索引该怎么创建了：

即建立一个risk_case_status_enum,subject_id_enum, subject_id三个字段的联合索引，并且顺序按照risk_case_status_enum,subject_id_enum, subject_id这样排列。

如此机能让where判断走索引，也能让排序走到索引。索引建立之后，执行计划如下：

```sql
+----+-------+-------------------------------------------------+---------------------+----------------------------------------------------+                                           
| id | type  | possible_keys                                   | key                 | Extra                                              |                                           
+----+-------+-------------------------------------------------+---------------------+----------------------------------------------------+                                           
|  1 | range | idx_subject_product ,idx_status_subject         | idx_status_subject  | Using index condition;                             |                                           
+----+-------+-------------------------------------------------+---------------------+----------------------------------------------------+ 
```

这样Extra中就只有`Using index condition`了，即基于索引实现了排序。

并且在发布之后，不再有报警发生，问题解决。
