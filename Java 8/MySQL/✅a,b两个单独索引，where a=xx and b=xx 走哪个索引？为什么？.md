# 典型回答

首先，**正常情况下，如果没有 a,b 的联合索引的话，这条 SQL 应该会在 a 和 b 这两个索引之间选择其中一个**，选择的时候由优化器根据区分度、选择性这样进行选择一个效率更高的。

具体选择索引会看那些信息参考：<br />[✅为什么MySQL会选错索引，如何解决？](https://www.yuque.com/hollis666/fo22bm/ghy5i20ie717exee?view=doc_embed)

另外，**还需要考虑索引失效的情况，如果发生索引失效的，那么比如 a 失效了，那么就只会走 b 的索引**，索引失效的可能的情况有很多，比如函数、比如类型转换等等。

[✅索引失效的问题是如何排查的，有那些种情况？](https://www.yuque.com/hollis666/fo22bm/sgkrtodriyoliden?view=doc_embed)

另外，除了上面说的情况外，**还有一种特殊情况是，可能会同时走a,b 两个索引。**

因为在 MySQL 5.1中推出了**索引合并**，这个功能可以通过使用同时使用两个单独索引来提升查询效率：

[✅什么是索引合并，原理是什么？](https://www.yuque.com/hollis666/fo22bm/cn34kd6tlw54ulmi?view=doc_embed)
