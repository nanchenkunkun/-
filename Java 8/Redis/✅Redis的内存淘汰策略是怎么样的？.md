# 典型回答

Redis 的内存淘汰策略用于在内存满了之后，决定哪些 key 要被删除。Redis 支持多种内存淘汰策略，可以通过配置文件中的 maxmemory-policy 参数来指定。

以下是 Redis 支持的内存淘汰策略：

- noeviction：不会淘汰任何键值对，而是直接返回错误信息。
- allkeys-lru：从所有 key 中选择最近最少使用的那个 key 并删除。
- volatile-lru：从设置了过期时间的 key 中选择最近最少使用的那个 key 并删除。
- allkeys-random：从所有 key 中随机选择一个 key 并删除。
- volatile-random：从设置了过期时间的 key 中随机选择一个 key 并删除。
- volatile-ttl：从设置了过期时间的 key 中选择剩余时间最短的 key 并删除。
-  volatile-lfu：淘汰的对象是带有过期时间的键值对中，访问频率最低的那个。
- allkeys-lfu：淘汰的对象则是所有键值对中，访问频率最低的那个。

[📝介绍下LFU、LRU等缓存失效算法？](https://www.yuque.com/hollis666/fo22bm/gl3fivks74z4d10e?view=doc_embed)
# 扩展知识

## 如何选择

以下是腾讯针对Redis的淘汰策略设置给出的建议：

- **当 Redis 作为缓存使用的时候，推荐使用 allkeys-lru 淘汰策略。该策略会将最近最少使用的 Key 淘汰。默认情况下，使用频率最低则后期命中的概率也最低，所以将其淘汰。**

<br />

- **当 Redis 作为半缓存半持久化使用时，可以使用 volatile-lru。但因为 Redis 本身不建议保存持久化数据，所以只作为备选方案。**

阿里云Redis默认是volatile-lru （[https://www.alibabacloud.com/help/zh/redis/user-guide/how-does-apsaradb-for-redis-evict-data-by-default](https://www.alibabacloud.com/help/zh/redis/user-guide/how-does-apsaradb-for-redis-evict-data-by-default) ）

腾讯云默认是noeviction，即不删除键。在内存占满后会出现 OOM 问题，所以建议创建好实例后修改淘汰策略，减少 OOM 问题的出现。（[https://cloud.tencent.com/document/product/239/90960](https://cloud.tencent.com/document/product/239/90960) ）


