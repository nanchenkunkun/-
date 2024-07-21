# 典型回答

在InnoDB存储引擎中，**当执行计划中出现Using filesort时，意味着MySQL需要对结果集进行外部排序**，以满足查询的ORDER BY条件。

如以下执行计划中的Extra中，出现的`Using filesort`表明了本次需要进行文件排序。

```javascript
+----+-------+----------------------+---------------------+----------------------------------------------------+                                           
| id | type  | possible_keys        | key                 | Extra                                              |                                           
+----+-------+----------------------+---------------------+----------------------------------------------------+                                           
|  1 | range | idx_subject_product  | idx_subject_product | Using index condition; Using where; Using filesort |                                           
+----+-------+----------------------+---------------------+----------------------------------------------------+ 
```

在下面这篇文章中，我们介绍过了order by的实现原理。`**Using filesort**`**通常发生在无法直接利用索引完成排序的情况下**，而是需要额外的排序步骤，可能会导致查询性能下降，尤其是在处理大量数据时。优化Using filesort的目的是减少排序所需的资源和时间，提高查询效率。

[✅order by 是怎么实现的？](https://www.yuque.com/hollis666/fo22bm/caou56?view=doc_embed)

所以，**当执行计划中出现**`**Using filesort**`**的是时候，就是一个我们可以优化的方向。**（但是，并不是说一定要优化！要看是否有必要以及收益是否够大）

针对`Using filesort`的优化，可以有以下几个方向**：**

**1、尽量使用索引排序：**

索引是天然有序的，所以当我们在使用order by的时候，如果能借助索引，那么效率一定是最高的。

- 那么我们就可以确保ORDER BY子句中的字段是索引的一部分。
- 并且如果可能，使ORDER BY中的列顺序与索引中的列顺序一致（order by a,b,c ， idx_a_b_c(a,b,c）)。
- 并且考虑使用复合索引。如果ORDER BY子句涉及多个列，创建一个包含这些列的复合索引可能会有助于消除Using filesort。


**2、优化MySQL配置：**

我们还可以调整sort_buffer_size参数。这个参数决定了排序操作可以使用的内存量。增加其值可以提高处理大型排序操作的能力（但设置过大可能会消耗过多内存资源，影响系统性能）

根据`sort_buffer_size`的大小不同，会在不同的地方进行排序操作：

- 如果要排序的数据量小于 `sort_buffer_size`，那么排序就在**内存**中完成。
- 如果排序数据量大于`sort_buffer_size`，则需要利用**磁盘临时文件**辅助排序。

在内存中排序肯定会更快一点的。


# 扩展知识

## filesort优化实战

[✅Sort aborted问题排查过程](https://www.yuque.com/hollis666/fo22bm/sox6oiog8m4xqtli?view=doc_embed)
