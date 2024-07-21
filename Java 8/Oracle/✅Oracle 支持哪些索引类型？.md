# 典型回答

Oracle 比MySQL 在索引上的支持更加全面，在数据结构上，主要包括了 B+树索引、位图索引、R 树索引以及 Quad 树索引。

### B+树索引（重要）

这个大家都不陌生，在 MySQL 中主要用的就是这个索引。

[✅InnoDB为什么使用B+树实现索引？](https://www.yuque.com/hollis666/fo22bm/uh3cy1?view=doc_embed)

使用B+树实现索引，有以下几个优点：

1. **支持范围查询**，B+树在进行范围查找时，只需要从根节点一直遍历到叶子节点，因为数据都存储在叶子节点上，而且叶子节点之间有指针连接，可以很方便地进行范围查找。
2. **支持排序**，B+树的叶子节点按照关键字顺序存储，可以快速支持排序操作，提高排序效率；
3. **有利于磁盘预读**。由于B+树的节点大小是固定的，因此可以很好地利用磁盘预读特性，一次性读取多个节点到内存中，这样可以减少IO操作次数，提高查询效率。
4. **有利于缓存**。B+树的非叶子节点只存储指向子节点的指针，而不存储数据，这样可以使得缓存能够容纳更多的索引数据，从而提高缓存的命中率，加快查询速度。

### 位图索引（重要）

位图索引使用一系列bit来表示数据记录是否存在。

[✅什么是BitMap？有什么用？](https://www.yuque.com/hollis666/fo22bm/ntqpq5vzps1bs55z?view=doc_embed)

bitmap 索引将每个被索引的列的值作为 KEY，使用每个 bit 表示一行，当这行中包含这个值时，设置为 1，否则设置为 0。

如下图：<br />![image.png](https://cdn.nlark.com/yuque/0/2024/png/5378072/1717822245471-f9e62e6a-91e7-42b3-a6dd-375efda16a07.png#averageHue=%23eff0f2&clientId=u42d02b9b-52b8-4&from=paste&height=896&id=u9b037123&originHeight=896&originWidth=2560&originalType=binary&ratio=1&rotation=0&showTitle=false&size=82498&status=done&style=none&taskId=u41b729f9-1ea7-424a-942c-20dca955659&title=&width=2560)

按照性别来看，编号从0到4分别是 M F F M F ，那么如果 M 作为索引的 key的话，那么他的结果就是 1 0 0 1 0，而 F 的结果结果就是0 1 1 0 1。

**位图索引和 B+树索引刚好相反，他比较适合建立在重复度比较高的字段上**，比如上面的性别字段，这样只需要两个 bitmap 就可以完成存储了。

同时，位图索引适合于那种变化不频繁的数据，比如上面的性别字段（哈哈哈哈哈。。。。）。而对于经常变化的字段，不适合用位图索引。

在创建索引时，指定CREATE BITMAP INDEX就是创建位图索引了。
```
CREATE BITMAP INDEX index_name ON table_name (column_name);
```
### 
### R树索引（了解即可）

Oracle 中一个专为存储、管理和检索位置数据设计的高级功能，它支持多种空间数据类型和相关操作，这就是Spatial。

Oracle Spatial使用R树索引来加速对**空间数据**的查询，特别是涉及空间位置和区域的查询。这种索引允许数据库有效地搜索大量空间对象，找到那些与指定空间区域相交或包含在内的对象。

因为R树非常适合于那些需要处理和分析地理空间数据的应用，如地图服务、位置搜索、城市规划和资源管理等领域。通过使用R树索引，Oracle能够提供强大的空间分析功能，支持复杂的空间查询，如距离计算、区域重叠和路径查找等。

![image.png](https://cdn.nlark.com/yuque/0/2024/png/5378072/1717823175633-55264274-c5e0-4e58-b722-f8d584e4501a.png#averageHue=%23070000&clientId=u42d02b9b-52b8-4&from=paste&height=400&id=ub7b9f7e8&originHeight=400&originWidth=400&originalType=binary&ratio=1&rotation=0&showTitle=false&size=40560&status=done&style=none&taskId=ueaf4c192-38ae-46fb-9dff-57c92261ccc&title=&width=400)

在Oracle Spatial中，创建R树索引的基本命令如下：
```
CREATE INDEX index_name ON table_name(column_name) 
INDEXTYPE IS MDSYS.SPATIAL_INDEX 
PARAMETERS ('sdo_indx_dims=2');
```

### Quad 树索引（了解即可）

Quad 树索引就是四叉树索引，在Oracle Spatial中，四叉树被用来优化大规模空间数据集的存储和查询，特别是那些需要频繁访问和更新的应用场景。例如，它可以用于快速定位地理信息系统（GIS）中的点、线和多边形数据。

![image.png](https://cdn.nlark.com/yuque/0/2024/png/5378072/1717823350977-1c806d64-7442-4aed-94b2-275cf76f58f6.png#averageHue=%23070505&clientId=u42d02b9b-52b8-4&from=paste&height=293&id=ub9c36c02&originHeight=860&originWidth=1258&originalType=binary&ratio=1&rotation=0&showTitle=false&size=67148&status=done&style=none&taskId=ub65a4522-591f-4b43-975b-1e86ffbf374&title=&width=428)

