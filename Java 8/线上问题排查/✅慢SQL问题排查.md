### 问题发现

线上有一个反欺诈相关的定时任务执行连续多次失败：

![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1682304911735-e83f4e3c-c29c-4b9c-a795-f0ac770af6e7.png#averageHue=%23f7f6f6&clientId=ucfdf1f37-91c4-4&from=paste&height=233&id=u3d19e0a2&originHeight=233&originWidth=1483&originalType=binary&ratio=1&rotation=0&showTitle=false&size=147283&status=done&style=none&taskId=u825e3064-e5e6-430b-b081-feb5997cd5c&title=&width=1483)

于是紧急排查日志，发现在任务执行的时间段，有大量报错：

```
Cause: ERR-CODE: [TDDL-4202][ERR_SQL_QUERY_TIMEOUT] Slow query leads to a timeout exception, please contact DBA to check slow sql. SocketTimout:12000 ms, 

```

在这个日志的上下文不远处就定位到这条慢SQL：

```plsql
select  distinct buyer_id as buyerId, seller_id as sellerId  from fraud_risk_case  WHERE subject_id_enum = 'BUYER_SELLER_BOTH' and (buyer_id = ? or seller_id = ?  ) and product_type_enum = ?   order by id desc limit 100
```

这条SQL的主要目的是找到是否有买卖家之间存在关联关系的数据。

通过执行explain，我们看了一下执行计划：

| id | 1 |
| --- | --- |
| select_type | SIMPLE |
| table | fraud_risk_case |
| partitions | _NULL_ |
| **type** | **index** |
| possible_keys | idx_byr_slr_product |
| **key** | idx_byr_slr_product |
| key_len | 1546 |
| ref | _NULL_ |
| **rows** | **4133627** |
| filtered | 0.19 |
| **Extra** | **Using where; Using index** |


通过这个执行计划可以发现，type = index ，extra = Using where; Using index ，表示SQL因为不符合最左前缀匹配，而扫描了整颗索引树，故而很慢。

于是查看这张表的建表语句，确实存在subject_id_enum和product_type_enum字段的联合索引，但是这个字段并不是前导列：

```plsql
idx_subject_product(subject_id,subject_id_enum,product_type)
```

### 问题解决

定位到问题之后，那解决起来就很简单了，只需要增加正确的索引或者修改SQL就行了。于是修改表结构，增加新的索引：<br />	
```plsql
ALTER TABLE `fraud_risk_case`
	ADD KEY `idx_subject_type_product_user` (`subject_id_enum`,`product_type_enum`,`buyer_id`,`seller_id`);
```

经过修改后，再执行以下执行计划：

| id | 1 |
| --- | --- |
| select_type | SIMPLE |
| table | fraud_risk_case |
| partitions | _NULL_ |
| **type** | **ref** |
| possible_keys | idx_byr_slr_product,idx_subject_type_product_user |
| key | **idx_subject_type_product_user** |
| key_len | 516 |
| ref | const,const |
| rows | 1 |
| filtered | 19.00 |
| Extra | **Using where; Using index; Using temporary; Using filesort** |


可以看到，type=ref，说明用到了普通索引，你并且rows也变少了，整个SQL大大提升了查询速度，任务失败的问题得到解决。

![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1682305893774-e6a8ad32-4687-4a5c-8925-491cb931e06d.png#averageHue=%23fafafa&clientId=ucfdf1f37-91c4-4&from=paste&height=248&id=ub05e2f44&originHeight=248&originWidth=1779&originalType=binary&ratio=1&rotation=0&showTitle=false&size=174262&status=done&style=none&taskId=ua0a6f6ad-d94f-4ec9-9181-f5aaf102ee2&title=&width=1779)



参考：

[✅SQL执行计划分析的时候，要关注哪些信息？](https://www.yuque.com/hollis666/fo22bm/fho0bamf4qpcril5?view=doc_embed)
