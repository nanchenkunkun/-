# 典型回答

InnoDB存储引擎支持两种常见的索引数据结构：**B+树索引、Hash索引**，其中B+树索引是目前关系型数据库系统中最常见、最有效的索引。

数据库中的B+树索引分为**聚集索引**和**非聚集索引**。聚集索引就是按照每张表的主键构造一个B+树，B+树的叶子节点中记录着表中一行记录的所有值。只要找到这个叶子节点也就得到了这条记录的所有值。非聚簇索引的叶节点中不包含行记录的所有值。只包含索引值和主键的值。

根据索引的唯一性，有可以把索引分为**唯一索引**和**普通索引**。唯一索引要求索引的列值必须唯一，不能重复。

另外，在MySQL 5.6中还增加了**全文索引**，5.7版本之后通过使用ngram插件开始支持中文。

# 扩展知识
## B+树索引和Hash索引
[InnoDB为什么使用B+树实现索引？](https://www.yuque.com/hollis666/fo22bm/uh3cy1?view=doc_embed)
## 聚簇索引和非聚簇索引

[什么是聚簇索引和非聚簇索引？](https://www.yuque.com/hollis666/fo22bm/le8gbo472cpxv63z?view=doc_embed)

## 唯一性索引

[MySQL是如何保证唯一性索引的唯一性的？](https://www.yuque.com/hollis666/fo22bm/gliyvzp21uq8uakh?view=doc_embed)

[✅唯一索引和主键索引的区别？](https://www.yuque.com/hollis666/fo22bm/mot9do3w6rh5u03t?view=doc_embed)


