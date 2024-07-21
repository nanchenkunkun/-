# 典型回答

MySQL的热点数据更新问题，一直都是行业内的一个难题，对于秒杀场景至关重要。一旦处理不好，就可能会导致数据库被打垮。

那么，如果一定要在MySQL这个层面上，抗住高并发的热点数据并发更新，有什么方案呢？拿库存扣减举例

1、库存拆分，把一个大的库存拆分成多个小库存，拆分后，一次扣减动作就可以分散到不同的库、表中进行，降低锁粒度提升并发。<br />优点：实现较简单<br />缺点：存在碎片问题、库存调控不方便<br />![image.png](https://cdn.nlark.com/yuque/0/2024/png/5378072/1708321335683-9bd38f0a-775f-43e1-82a1-a0efafa88e03.png#averageHue=%23f9f9f9&clientId=u468e6a33-bc7a-4&from=paste&height=583&id=u0661410e&originHeight=1166&originWidth=1626&originalType=binary&ratio=2&rotation=0&showTitle=false&size=857588&status=done&style=none&taskId=u1adc2a9f-eb92-4568-9531-b0216fcadb6&title=&width=813)

2、请求合并，把多个库存扣减请求，合并成一个，进行批量更新。<br />优点：简单<br />缺点：适用于异步场景，或者经过分析后认为可以合并的场景

3、把update转换成insert，直接插入一次占用记录，然后异步统计剩余库存，或者通过SQL统计流水方式计算剩余库存。<br />优点：没有update，无锁冲突<br />缺点：insert时控制不好容易超卖、insert后剩余库存不好统计

除了上面这三个方案外，重点介绍一个我们公司内部在用的，扛了双十一的高并发的秒杀的方案。

那就是**改造MySQL**

主要思路就是，针对于频繁更新或秒杀类业务场景，大幅度优化对于热点行数据的update操作的性能。当开启热点更新自动探测时，系统会自动探测是否有单行的热点更新，如果有，则会让大量的并发 update 排队执行，以减少大量行锁造成的并发性能下降。

也就是说，他们改造了MySQL数据库，让同一个热点行的更新语句，在执行层进行排队。这样的排队相比update的排队，要轻量级很多，因为他不需要自旋，不需要抢锁。

这个方案的好处就是开发不需要做额外的事情，只需要开启热点检测就行了。缺点就是改造MySQL数据库有成本。不过现在很多云上数据库都支持了。如：

腾讯云数据库MySQL热点更新： [https://cloud.tencent.com/document/product/236/63239](https://cloud.tencent.com/document/product/236/63239)<br />阿里云数据库Inventory Hint： [https://www.alibabacloud.com/help/zh/apsaradb-for-rds/latest/inventory-hint](https://www.alibabacloud.com/help/zh/apsaradb-for-rds/latest/inventory-hint)


具体原理见：

[✅阿里的数据库能抗秒杀的原理](https://www.yuque.com/hollis666/fo22bm/gwg64tg0g107wgz3?view=doc_embed)



# 扩展知识

## 批次更新的具体实现

请求合并，把多个库存扣减请求，合并成一个，进行批量更新。

这个方案的本质上是想将实时处理的并发操作转变为批量处理，以减少数据库的压力并提高效率。这种操作通常分为两个步骤：首先是收集和汇总积分，然后是定期更新用户积分表。以下是这两个步骤的示例 SQL 代码：

#### 收集和汇总积分

在这一步中，你可以将需要增加的积分记录在一个临时表或一个专门用于积分变更的表中。假设这个表名为 pending_points，结构如下：

```
CREATE TABLE pending_points (
    user_id INT,        -- 用户ID
    points_to_add INT,  -- 待增加的积分
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP -- 记录创建时间
);
```

每当有积分变更时，你只需要往这个表中插入一条记录：

```
INSERT INTO pending_points (user_id, points_to_add) VALUES (123, 10);  
-- 假设为用户ID 123增加10积分
```

#### 定期更新用户积分表

假设你有一个 users 表来存储用户信息和他们的当前积分，结构如下：

```
CREATE TABLE users (
    user_id INT PRIMARY KEY,
    points INT          -- 用户当前积分
);
```

然后，你可以每隔10分钟（为了避免出现时间差导致数据丢失，可以时间频率更短一点，比如5分钟）运行一个 SQL 脚本或使用计划任务（如使用定时任务，xxl-job等），来汇总 pending_points 表中的积分并更新到 users 表中。之后，清空 pending_points 表中已处理的记录：

```
-- 更新用户积分
UPDATE users
SET points = points + (
    SELECT SUM(points_to_add)
    FROM pending_points
    WHERE users.user_id = pending_points.user_id
    AND pending_points.created_at <= NOW() - INTERVAL '10 MINUTE'  -- 只处理过去10分钟内的记录
)
WHERE EXISTS (
    SELECT 1
    FROM pending_points
    WHERE users.user_id = pending_points.user_id
    AND pending_points.created_at <= NOW() - INTERVAL '10 MINUTE'
);

-- 删除已处理的积分记录
DELETE FROM pending_points WHERE created_at <= NOW() - INTERVAL '10 MINUTE';
```

这里使用 `NOW() - INTERVAL '10 MINUTE' `来确保只处理过去10分钟内的积分记录。这种方式减少了即时更新的操作，通过批量处理来提高效率。

当然，这个方案还需要考虑一些其他的问题，比如某个任务失败了如何补偿之类的问题，大家可以基于这个方案再做延伸即可。
