# 典型回答

Eureka 的缓存机制设计主要目的是提高服务发现的效率和减少服务注册中心的压力，尤其是在面对大规模的服务注册和发现请求时。这种多层缓存设计帮助 Eureka 提供快速的响应能力。

也正是因为有了比较复杂的缓存机制，**所以Eureka提供的是AP能力，即可用性有保证，但是一致性是没办法保证的。**

**Eureka共提供了三层缓存，分别是registry、readWriteCacheMap、readOnlyCacheMap；**

![](https://cdn.nlark.com/yuque/0/2024/png/5378072/1713586770381-f32f9eae-28a5-4924-8f3b-6d96e2450f66.png#averageHue=%23f6f6f5&clientId=u6eb62e39-6b8d-4&from=paste&id=u3dffb181&originHeight=598&originWidth=1146&originalType=url&ratio=1&rotation=0&showTitle=false&status=done&style=none&taskId=uedbbec2a-61e0-4bc5-af26-7ebfdea7b3f&title=)<br />（此图来源于网络，出处找不到了，但是画的很好，应用过来了）

**registry**：服务注册表是 Eureka 中最基础的数据结构，存储了所有服务实例的注册信息。这些信息包括服务的名称、IP 地址、端口、健康状态等。服务注册表是所有读写操作的基础数据源。

**readWriteCacheMap** ：readWriteCacheMap 是一个缓存层，主要用于缓存服务实例信息的最新状态，允许快速读取和写入。这个缓存相当于 Registry 的一个近实时的反映，提供了更快的访问速度，并减少了对主 Registry 的直接访问压力。

**readOnlyCacheMap**：readOnlyCacheMap 是对服务注册信息的一个只读视图，主要用于处理对服务实例信息的外部请求。这个缓存层定期从 readWriteCacheMap 更新数据，保证了信息的一定程度的新鲜度和准确性，同时因为是只读操作，可以提供非常高效的访问速度。

|  | **目的** | **实现方式** | **更新机制** |
| --- | --- | --- | --- |
| **Registry** | 提供一个准确的、权威的数据源 | ConcurrentHashMap | 当服务实例注册、续约或者注销时，Registry 会立即更新以反映最新的服务状态。 |
| **readWriteCacheMap** | 提供了一个快速的读写能力，减少了对 Registry 的直接压力 | Guava Cache（LoadingCache） | 当 Registry 中的数据发生变化时，readWriteCacheMap 会同步更新。缓存时间180秒； |
| **readOnlyCacheMap** | 提供了快速且频繁的读取操作，特别适用于大量的客户端查询 | ConcurrentHashMap | 默认30秒定时从 readWriteCacheMap 同步快照数据 |


当服务实例注册、续约或者注销时，Registry 会立即更新以反映最新的服务状态。并且默认每隔90秒把没有续约服务从注册表中剔除。当注册表发生变化时，会立刻同步到readWriteCacheMap中。同时会有一个定时任务，每隔30秒钟从readWriteCacheMap获取最新的快照到readOnlyCacheMap中。	

![](https://cdn.nlark.com/yuque/0/2024/png/5378072/1713586884346-de62698a-70f9-48ff-bf60-df96bc6d30fc.png#averageHue=%23f1f1f0&clientId=u6eb62e39-6b8d-4&from=paste&id=ubd884569&originHeight=1026&originWidth=1788&originalType=url&ratio=1&rotation=0&showTitle=false&status=done&style=none&taskId=u220ad779-d8d0-4408-8961-5be90b6aa29&title=)
