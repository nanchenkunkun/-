# 典型回答

Seata 是一个开源的分布式事务解决方案，旨在提供高性能和简单的事务管理服务，用于微服务架构。其中，AT 模式是 Seata 支持的一种事务模式，全称为 Auto-Commit Transaction，也就是自动提交事务模式。

它是一种无侵入性的事务模式，对于业务开发来说，不需要做代码上面的改造，就可以实现分布式事务。

Seata中包含了三个组件：

- **TC (Transaction Coordinator)**：事务协调器，负责管理全局事务的生命周期，包括开始事务、提交事务和回滚事务。
- **TM (Transaction Manager)**：事务管理器，定义事务的范围，负责开启和结束全局事务。
- **RM (Resource Manager)**：资源管理器，管理资源对象，如数据库连接，负责资源的注册与释放。

详见：<br />[✅什么是Seata？他有哪几种模式？](https://www.yuque.com/hollis666/fo22bm/qro9fl9lsiinx1tu?view=doc_embed)

**AT 模式基于两阶段提交（2PC）协议进行工作，通过代理数据源的方式，使得本地事务（如数据库事务）与全局事务（跨服务的事务）能够统一管理。**

所谓代理数据源，其实就是在应用自己的Datasource之上做了一层代理，是的原本的JDBC Datasource变成Seate DatasourceProxy，这样就可以在这层代理当中控制SQL语句的提交、回滚等操作。

![](https://www.hollischuang.com/wp-content/uploads/2024/06/17189582311169.jpg#id=mdlwH&originHeight=678&originWidth=714&originalType=binary&ratio=1&rotation=0&showTitle=false&status=done&style=none&title=)

在Seata的AT模式中，事务的提交也是分成了2阶段的。

### 一阶段

1、RM针对本次要执行的本地事务的SQL进行解析，得到SQL的类型、修改的表以及where条件等信息。

2、RM 根据 SQL 解析的结果，先进行一次查询，根据查询结果生成相应的 before image（变更前数据快照）。

3、执行SQL语句进行数据库变更。

4、再查询一次变更后的记录，作为after image（变更后的数据快照）

5、把before/after image 以及业务 SQL 相关的信息组成一条回滚日志记录，插入到 UNDO_LOG 表中。

6、提交前，向 TC 注册分支，并申请表中本次需要修改的所有记录的排他锁。

7、将业务数据的更新和前面生成的 UNDO LOG 一并提交。

8、讲本地事务的执行结果上报给TC。

![](https://www.hollischuang.com/wp-content/uploads/2024/06/17189586176886.jpg#id=xEMqk&originHeight=354&originWidth=720&originalType=binary&ratio=1&rotation=0&showTitle=false&status=done&style=none&title=)

这里依赖的完全是本地事务，基于本地事务的ACID的特性，可以保证业务数据和回滚日志（before/after image）可以在同一个本地事务中被提交。

### 二阶段

在一阶段，业务操作完成后，TM 向 TC 发起提交请求。TC 会发起投票请求，询问所有的RM是否可以提交事务。

那么就会出现2种情况：

**提交事务**：如果所有的 RM 都同意提交，说明他们此时他们的本地事务都已经执行成功了，那么TC就可以释放该全局事务的所有锁，然后异步调用RM清理Undo Log

![](https://www.hollischuang.com/wp-content/uploads/2024/06/17189586260885.jpg#id=BjNpF&originHeight=328&originWidth=720&originalType=binary&ratio=1&rotation=0&showTitle=false&status=done&style=none&title=)

**回滚事务**：如果任一 RM 投票否决或者出现故障，那么就要协调事务进行回滚。

1、通过 XID 和 Branch ID 查找到相应的 UNDO LOG 记录。<br />2、会拿当前数据先跟afterImage进行比较，如果一致，执行第三步。如果不一致则在比较一下当前数据是否和beforeImage，如果一致性，说明未提交成功或者已经回滚了，则无需处理。如果不一致，那么说明有脏数据了，需要抛出异常，人工处理。<br />3、根据 UNDO LOG 中的before image和业务 SQL 的相关信息生成并执行回滚的语句。<br />4、执行SQL并提交本地事务。并把本地事务的执行结果上报给 TC。

![](https://www.hollischuang.com/wp-content/uploads/2024/06/17189586626216.jpg#id=CI8OC&originHeight=344&originWidth=720&originalType=binary&ratio=1&rotation=0&showTitle=false&status=done&style=none&title=)

# 扩展知识

## 如何使用AT模式

AT模式的使用非常简单，就是把Spring中的@Transactional  替换成 @GlobalTransactional，如下面的例子： 

```java

@GlobalTransactional
public void purchase(String userId, String commodityCode, int count, int money) {
    jdbcTemplateA.update("update stock_tbl set count = count - ? where commodity_code = ?", new Object[] {count, commodityCode});
    jdbcTemplateB.update("update account_tbl set money = money - ? where user_id = ?", new Object[] {money, userId});
}
```

这里面的jdbcTemplateA和jdbcTemplateB是两个不同的数据源，通过Seata的分布式事务来协调ACID。

## undolog格式

undolog的格式，以下内容来自Seata官网中的例子：

```
{
	"branchId": 641789253,
	"undoItems": [{
		"afterImage": {
			"rows": [{
				"fields": [{
					"name": "id",
					"type": 4,
					"value": 1
				}, {
					"name": "name",
					"type": 12,
					"value": "GTS"
				}, {
					"name": "since",
					"type": 12,
					"value": "2014"
				}]
			}],
			"tableName": "product"
		},
		"beforeImage": {
			"rows": [{
				"fields": [{
					"name": "id",
					"type": 4,
					"value": 1
				}, {
					"name": "name",
					"type": 12,
					"value": "TXC"
				}, {
					"name": "since",
					"type": 12,
					"value": "2014"
				}]
			}],
			"tableName": "product"
		},
		"sqlType": "UPDATE"
	}],
	"xid": "xid:xxx"
}
```


