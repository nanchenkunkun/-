
GEO就是Geolocation的简写形式，代表地理坐标，Redis GEO 主要用于存储地理位置信息的，帮助我们根据经纬度来检索数据。

它主要支持如下命令：

- GEOADD：添加一个地理空间信息，包含：经度（longitude）、纬度（latitude）、值（member）
- GEODIST：计算指定的两个点之间的距离并返回
- GEOHASH：将指定member的坐标转为hash字符串形式并返回
- GEOPOS：返回指定member的坐标
- GEORADIUS：指定圆心、半径，找到该圆内包含的所有member，并按照与圆心之间的距离排序后返回。
- GEOSEARCH：在指定范围内搜索member，并按照与指定点之间的距离排序后返回。范围可以是圆形或矩形。
- GEOSEARCHSTORE：与GEOSEARCH功能一致，不过可以把结果存储到一个指定的key。


# 扩展知识

## 使用例子
假设我们想要使用Redis存储和查询几个地点的位置。首先，我们将地点添加到Redis的地理空间集合中，然后我们可以根据位置查询附近的地点。

```java
GEOADD locations 13.361389 38.115556 "Palermo" 15.087269 37.502669 "Catania"
```

这个命令将“Palermo”和“Catania”两个地点添加到名为**locations**的地理空间集合中。

```java
GEORADIUS locations 15 37 100 km WITHDIST
```

这个命令会查找locations集合中距离经度15、纬度37、100公里范围内的所有地点，并返回它们的名称和距离。

> WITHDIST：同时返回找到的项距离指定中心的距离。距离以命令的半径参数指定的单位返回。
> WITHCOORD：还会返回匹配项的经度、纬度坐标。
> WITHHASH：还会以52位无符号整数的形式返回项的原始geohash编码排序集分数。这只对调试有用，对一般用户来说用处不大。


## Redis GEO实现查找附近的人


[✅如何实现"查找附近的人"功能？](https://www.yuque.com/hollis666/fo22bm/ow77mcr961n4z7mg?view=doc_embed)
