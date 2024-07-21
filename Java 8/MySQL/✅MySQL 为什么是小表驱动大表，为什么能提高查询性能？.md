# 典型回答

[✅MySQL的驱动表是什么？MySQL怎么选的？](https://www.yuque.com/hollis666/fo22bm/vs83kfhxbz19mkcg?view=doc_embed)

我们介绍过，当进行联接查询时，MySQL 通常会选择较小的表作为驱动表，然后在较大的表中查找匹配的记录。这种策略的核心思想是减少扫描和比较的次数，从而提高查询性能。

假设我们有两个表：employees（1000 条记录）和 departments（10 条记录），并且要进行以下查询：

```java
SELECT e.name, d.department_name
FROM employees e
JOIN departments d ON e.department_id = d.id
```

在不考虑hash join等其他链接方式，只考虑nested loop join的情况下，其实执行的次数是笛卡尔积，即：

```java
for(1000){
    for(10)
}

和

for(10){
    for(1000)
}
```
<br />但是，假设employees.department_id和departments.id 都有索引的情况下，就不一样了，因为索引的查询是比较快的，他的复杂度是log(n)。那么：

大表驱动小表，复杂度为：O(1000) * O(log 10)<br />小表驱动大表，复杂度为：O(10) * O(log 1000)

这样一算的话，就非常清楚了，肯定是小表驱动大表的整体的复杂度更低！
