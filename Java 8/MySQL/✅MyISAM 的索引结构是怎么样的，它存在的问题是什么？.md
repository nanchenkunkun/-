# 典型回答

和InnoDB最大的不同，MyISAM是采用了一种索引和数据分离的存储方式，也就是说，MyISAM中索引文件和数据文件是独立的。<br />![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1697716353442-c70b80e4-be96-427a-b7d3-5ece0bdb58ef.png#averageHue=%23f6f6f6&clientId=ucb6dc526-58cb-4&from=paste&height=1578&id=u68f9847d&originHeight=1578&originWidth=2572&originalType=binary&ratio=1&rotation=0&showTitle=false&size=1675974&status=done&style=none&taskId=u0c9895a0-f0a4-4875-a5a0-4f06f982370&title=&width=2572)

因为文件独立，所以在MyISAM的索引树中，叶子节点上存储的并不是数据，而是数据所在的地址。所以，MyISAM 存储引擎实际上不支持聚簇索引的概念。在 MyISAM 中，所有索引都是非聚簇索引。

[✅什么是聚簇索引和非聚簇索引？](https://www.yuque.com/hollis666/fo22bm/le8gbo472cpxv63z?view=doc_embed)

也就是说，在MyISAM中，根据索引查询的过程中，必然需要先查到数据所在的地址，然后再查询真正的数据，那么就需要有两次查询的过程。而在InnoDB中，如果基于聚簇索引查询，则不需要回表，因为叶子节点上就已经包含数据的内容了。

因为MyISAM是先出的，正式因为存在这个问题，所以后来的InnoDB 引入了聚簇索引的概念提高了数据检索的效率，特别是对于主键检索。
