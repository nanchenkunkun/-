**1、BitMap是什么**

​	就是通过一个bit位来表示某个元素对应的值或者状态,其中的key就是对应元素本身。我们知道8个bit可以组成一个Byte，所以bitmap本身会极大的节省储存空间。

**2、Redis中的BitMap**

Redis从2.2.0版本开始新增了`setbit`,`getbit`,`bitcount`等几个bitmap相关命令。虽然是新命令，但是并没有新增新的数据类型，因为`setbit`等命令只不过是在`set`上的扩展。

3、**setbit命令介绍**

指令 `SETBIT key offset value`
设置或者清空key的value(字符串)在offset处的bit值(只能只0或者1)。

**4、使用场景一：用户签到**

很多网站都提供了签到功能(这里不考虑数据落地事宜)，并且需要展示最近一个月的签到情况，如果使用bitmap我们怎么做？一言不合亮代码！
根据日期 offset =hash % 365  ； key = 年份#用户id

```
127.0.0.1:6379> setbit 2020#zkk 1 1
(integer) 0
127.0.0.1:6379> setbit 2020#zkk 2 1
(integer) 0
127.0.0.1:6379> bitcount 2020#zkk
(integer) 2
```

**6、使用场景二：统计活跃用户**

使用时间作为cacheKey，然后用户ID为offset，如果当日活跃过就设置为1
那么我该如果计算某几天/月/年的活跃用户呢(暂且约定，统计时间内只有有一天在线就称为活跃)，有请下一个redis的命令
命令 `BITOP operation destkey key [key ...]`
说明：对一个或多个保存二进制位的字符串 key 进行位元操作，并将结果保存到 destkey 上。
说明：BITOP 命令支持 AND 、 OR 、 NOT 、 XOR 这四种操作中的任意一种参数

20190216 活跃用户 【1，2】
20190217 活跃用户 【1】
统计20190216~20190217 总活跃用户数: 1

```
127.0.0.1:6379> setbit 20190216 1 1
(integer) 1
127.0.0.1:6379> setbit 20190216 2 1
(integer) 1
127.0.0.1:6379> setbit 20190217 1 1
(integer) 0
127.0.0.1:6379> bitop and desk1 20190216 20190217
(integer) 1
127.0.0.1:6379> bitcount desk1
(integer) 1
127.0.0.1:6379> bitop or desk2 20190216 20190217
(integer) 1
127.0.0.1:6379> bitcount desk2
(integer) 2
127.0.0.1:6379>
```

**7、使用场景三：用户在线状态**

开发一个查询当前用户是否在线的接口。使用bitmap是一个节约空间效率又高的一种方法，只需要一个key，然后用户ID为offset，如果在线就设置为1，不在线就设置为0，和上面的场景一样，5000W用户只需要6MB的空间。