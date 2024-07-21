缓存失效算法主要是进行缓存失效的，当缓存中的存储的对象过多时，需要通过一定的算法选择出需要被淘汰的对象，一个好的算法对缓存的命中率影响是巨大的。常见的缓存失效算法有FIFO、LRU、LFU，以及Caffeine中的Window TinyLFU算法。

### FIFO

FIFO 算法是一种比较容易实现也最容易理解的算法。它的主要思想就是和队列是一样的，即先进先出（First In First Out）

一般认为一个数据是最先进入的，那么可以认为在将来它被访问的可能性很小。

因为FIFO刚好符合队列的特性，所以通常FIFO的算法都是使用队列来实现的：

![image.png](https://cdn.nlark.com/yuque/0/2022/png/5378072/1672296682476-98ee6a2f-871c-4d2d-bc95-b8b70e8d9f43.png#averageHue=%23fafafa&clientId=ud356578a-635b-4&from=paste&height=656&id=ua9df0e1a&originHeight=656&originWidth=1324&originalType=binary&ratio=1&rotation=0&showTitle=false&size=80186&status=done&style=none&taskId=ubf4721fb-db60-434d-8467-6d49b692595&title=&width=1324)

1. 新数据插入到队列尾部，数据在队列中顺序移动；<br />2. 淘汰队列头部的数据；

### LRU

LRU（The Least Recently Used，最近最少使用）是一种常见的缓存算法，在很多分布式缓存系统（如Redis, Memcached）中都有广泛使用。

LRU算法的思想是：如果一个数据在最近一段时间没有被访问到，那么可以认为在将来它被访问的可能性也很小。因此，当空间满时，最久没有访问的数据最先被淘汰。

最常见的实现是使用一个链表保存缓存数据，详细算法实现如下：

![image.png](https://cdn.nlark.com/yuque/0/2022/png/5378072/1672297308812-4613871f-5f49-45f9-92b6-057d948bc076.png#averageHue=%23fafafa&clientId=ud356578a-635b-4&from=paste&height=1306&id=u07970c73&originHeight=1306&originWidth=1688&originalType=binary&ratio=1&rotation=0&showTitle=false&size=190591&status=done&style=none&taskId=u593eadf3-24f2-4dad-a10f-936c9ae5cf0&title=&width=1688)

1. 新数据插入到链表头部；<br />2. 每当缓存命中，则将数据移到链表头部；<br />3. 当链表满的时候，将链表尾部的数据丢弃。

### LFU
LFU（Least Frequently Used ，最近最不常用）也是一种常见的缓存算法。

顾名思义，LFU算法的思想是：如果一个数据在最近一段时间很少被访问到，那么可以认为在将来它被访问的可能性也很小。因此，当空间满时，最小频率访问的数据最先被淘汰。

LFU的每个数据块都有一个引用计数，所有数据块按照引用计数排序，具有相同引用计数的数据块则按照时间排序。

具体实现如下：

![image.png](https://cdn.nlark.com/yuque/0/2022/png/5378072/1672297971423-6c8a07d7-d86f-484b-bcd0-2db14d5e83ac.png#averageHue=%23f6f6f6&clientId=ud356578a-635b-4&from=paste&height=816&id=u25f987a8&originHeight=816&originWidth=1776&originalType=binary&ratio=1&rotation=0&showTitle=false&size=240551&status=done&style=none&taskId=ue24fb1da-f837-45ae-ae6a-ac6caadce0d&title=&width=1776)

1. 新加入数据插入到队列尾部（因为引用计数为1）；<br />2. 队列中的数据被访问后，引用计数增加，队列重新排序；<br />3. 当需要淘汰数据时，将已经排序的列表最后的数据块删除。

### W-TinyLFU

LFU 通常能带来最佳的缓存命中率，但 LFU 有两个缺点：

1. 它需要给每个记录项维护频率信息，每次访问都需要更新，需要一个巨大的空间记录所有出现过的 key 和其对应的频次；
2. 如果数据访问模式随时间有变，LFU 的频率信息无法随之变化，因此早先频繁访问的记录可能会占据缓存，而后期访问较多的记录则无法被命中；
3. 如果一个刚加入缓存的元素，它的频率并不高，那么它可能会会直接被淘汰。

其中第一点过于致命导致我们通常不会使用 LFU。我们最常用的 LRU 实现简单，内存占用低，但其并不能反馈访问频率。LFU 通常需要较大的空间才能保证较好的缓存命中率。

W-TinyLFU是一种高效的缓存淘汰算法，它是TinyLFU算法的一种改进版本，主要用于处理大规模缓存系统中的淘汰问题。W-TinyLFU的核心思想是基于窗口的近似最少使用算法，即根据数据的访问模式动态地调整缓存中数据的淘汰策略。**W-TinyLFU 综合了LRU和LFU的长处：高命中率、低内存占用。**

W-TinyLFU由多个部分组合而成，包括窗口缓存、过滤器和主缓存。

![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1678521862530-0a19129d-32e9-49bd-b491-01a69ee7ec0c.png#averageHue=%23fcfcfc&clientId=u70206e19-d34c-4&from=paste&height=500&id=lIXOi&originHeight=500&originWidth=2062&originalType=binary&ratio=1&rotation=0&showTitle=false&size=325000&status=done&style=none&taskId=uf7a4b6b0-fa4a-428c-9562-8c26dde275b&title=&width=2062)<br />使用LRU来作为一个窗口缓存，主要是让元素能够有机会在窗口缓存中去积累它的频率，避免因为频率很低而直接被淘汰。

主缓存是使用SLRU，元素刚进入W-TinyLFU会在窗口缓存暂留一会，被挤出窗口缓存时，会在过滤器中和主缓存中最容易被淘汰的元素进行PK，如果频率大于主缓存中这个最容易被淘汰的元素，才能进入主缓存。











