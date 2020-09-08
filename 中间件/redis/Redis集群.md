# Redis集群

redis集群有三种集群模式，分别是：

```
* 主从模式

* Sentinel模式

* Cluster模式

```

### 主从模式

#### 主从模式介绍

主从模式是三种模式中最简单的，在主从复制中，数据库分为两类：主数据库(master)和从数据库(slave)。

其中主从复制有如下特点：

```
* 主数据库可以进行读写操作，当读写操作导致数据变化时会自动将数据同步给从数据库

* 从数据库一般都是只读的，并且接收主数据库同步过来的数据

* 一个master可以拥有多个slave，但是一个slave只能对应一个master

* slave挂了不影响其他slave的读和master的读和写，重新启动后会将数据从master同步过来

* master挂了以后，不影响slave的读，但redis不再提供写服务，master重启后redis将重新对外提供写服务

* master挂了以后，不会在slave节点中重新选一个master

```

工作机制：

当slave启动后，主动向master发送SYNC命令。master接收到SYNC命令后在后台保存快照（RDB持久化）和缓存保存快照这段时间的命令，然后将保存的快照文件和缓存的命令发送给slave。slave接收到快照文件和命令后加载快照文件和缓存的执行命令。

复制初始化后，master每次接收到的写命令都会同步发送给slave，保证主从数据一致性。

安全设置：

当master节点设置密码后，

```
客户端访问master需要密码

启动slave需要密码，在配置文件中配置即可

客户端访问slave不需要密码

```

缺点：

从上面可以看出，master节点在主从模式中唯一，若master挂掉，则redis无法对外提供写服务。

#### 主从模式搭建

- 环境准备：

  

```
master节点                  192.168.30.128

slave节点                   192.168.30.129

slave节点                   192.168.30.130

```

- 修改配置：

  192.168.30.128

```
# mkdir -p /data/redis

# vim /usr/local/redis/redis.conf

bind 192.168.30.128               #监听ip，多个ip用空格分隔
daemonize yes               #允许后台启动
logfile "/usr/local/redis/redis.log"                #日志路径
dir /data/redis                 #数据库备份文件存放目录
masterauth 123456               #slave连接master密码，master可省略
requirepass 123456              #设置master连接密码，slave可省略

appendonly yes                  #在/data/redis/目录生成appendonly.aof文件，将每一次写操作请求都追加到appendonly.aof 文件中

# echo 'vm.overcommit_memory=1' >> /etc/sysctl.conf

# sysctl -p

```



192.168.30.129

```
# mkdir -p /data/redis

# vim /usr/local/redis/redis.conf

bind 192.168.30.129
daemonize yes
logfile "/usr/local/redis/redis.log"
dir /data/redis
replicaof 192.168.30.128 6379
masterauth 123456
requirepass 123456
appendonly yes

# echo 'vm.overcommit_memory=1' >> /etc/sysctl.conf

# sysctl -p

```



192.168.30.130

```
# mkdir -p /data/redis

# vim /usr/local/redis/redis.conf

bind 192.168.30.130
daemonize yes
logfile "/usr/local/redis/redis.log"
dir /data/redis
replicaof 192.168.30.128 6379
masterauth 123456
requirepass 123456
appendonly yes

# echo 'vm.overcommit_memory=1' >> /etc/sysctl.conf

# sysctl -p

```

在master节点写入的数据，很快就同步到slave节点上，而且在slave节点上无法写入数据。

### Sentinel模式

#### Sentinel模式介绍

主从模式的弊端就是不具备高可用性，当master挂掉以后，Redis将不能再对外提供写入操作，因此sentinel应运而生。

sentinel中文含义为哨兵，顾名思义，它的作用就是监控redis集群的运行状况，特点如下：

```
* sentinel模式是建立在主从模式的基础上，如果只有一个Redis节点，sentinel就没有任何意义

* 当master挂了以后，sentinel会在slave中选择一个做为master，并修改它们的配置文件，其他slave的配置文件也会被修改，比如slaveof属性会指向新的master

* 当master重新启动后，它将不再是master而是做为slave接收新的master的同步数据

* sentinel因为也是一个进程有挂掉的可能，所以sentinel也会启动多个形成一个sentinel集群

* 多sentinel配置的时候，sentinel之间也会自动监控

* 当主从模式配置密码时，sentinel也会同步将配置信息修改到配置文件中，不需要担心

* 一个sentinel或sentinel集群可以管理多个主从Redis，多个sentinel也可以监控同一个redis

* sentinel最好不要和Redis部署在同一台机器，不然Redis的服务器挂了以后，sentinel也挂了

```

工作机制：

```
* 每个sentinel以每秒钟一次的频率向它所知的master，slave以及其他sentinel实例发送一个 PING 命令 

* 如果一个实例距离最后一次有效回复 PING 命令的时间超过 down-after-milliseconds 选项所指定的值， 则这个实例会被sentinel标记为主观下线。 

* 如果一个master被标记为主观下线，则正在监视这个master的所有sentinel要以每秒一次的频率确认master的确进入了主观下线状态

* 当有足够数量的sentinel（大于等于配置文件指定的值）在指定的时间范围内确认master的确进入了主观下线状态， 则master会被标记为客观下线 

* 在一般情况下， 每个sentinel会以每 10 秒一次的频率向它已知的所有master，slave发送 INFO 命令 

* 当master被sentinel标记为客观下线时，sentinel向下线的master的所有slave发送 INFO 命令的频率会从 10 秒一次改为 1 秒一次 

* 若没有足够数量的sentinel同意master已经下线，master的客观下线状态就会被移除；
  若master重新向sentinel的 PING 命令返回有效回复，master的主观下线状态就会被移除

```

当使用sentinel模式的时候，客户端就不要直接连接Redis，而是连接sentinel的ip和port，由sentinel来提供具体的可提供服务的Redis实现，这样当master节点挂掉以后，sentinel就会感知并将新的master节点提供给使用者。



#### Sentinel模式搭建

- 环境准备：

```
master节点              192.168.30.128          sentinel端口：26379

slave节点               192.168.30.129          sentinel端口：26379

slave节点               192.168.30.130          sentinel端口：26379

```



- 修改配置：

前面已经下载安装了redis，这里省略，直接修改sentinel配置文件。

192.168.30.128

```
# vim /usr/local/redis/sentinel.conf

daemonize yes
logfile "/usr/local/redis/sentinel.log"
dir "/usr/local/redis/sentinel"                 #sentinel工作目录
sentinel monitor mymaster 192.168.30.128 6379 2                 #判断master失效至少需要2个sentinel同意，建议设置为n/2+1，n为sentinel个数
sentinel auth-pass mymaster 123456
sentinel down-after-milliseconds mymaster 30000                 #判断master主观下线时间，默认30s

```

这里需要注意，`sentinel auth-pass mymaster 123456`需要配置在`sentinel monitor mymaster 192.168.30.128 6379 2`下面，否则启动报错：

Sentinel模式下的几个事件：

```
·       +reset-master ：主服务器已被重置。

·       +slave ：一个新的从服务器已经被 Sentinel 识别并关联。

·       +failover-state-reconf-slaves ：故障转移状态切换到了 reconf-slaves 状态。

·       +failover-detected ：另一个 Sentinel 开始了一次故障转移操作，或者一个从服务器转换成了主服务器。

·       +slave-reconf-sent ：领头（leader）的 Sentinel 向实例发送了 [SLAVEOF](/commands/slaveof.html) 命令，为实例设置新的主服务器。

·       +slave-reconf-inprog ：实例正在将自己设置为指定主服务器的从服务器，但相应的同步过程仍未完成。

·       +slave-reconf-done ：从服务器已经成功完成对新主服务器的同步。

·       -dup-sentinel ：对给定主服务器进行监视的一个或多个 Sentinel 已经因为重复出现而被移除 —— 当 Sentinel 实例重启的时候，就会出现这种情况。

·       +sentinel ：一个监视给定主服务器的新 Sentinel 已经被识别并添加。

·       +sdown ：给定的实例现在处于主观下线状态。

·       -sdown ：给定的实例已经不再处于主观下线状态。

·       +odown ：给定的实例现在处于客观下线状态。

·       -odown ：给定的实例已经不再处于客观下线状态。

·       +new-epoch ：当前的纪元（epoch）已经被更新。

·       +try-failover ：一个新的故障迁移操作正在执行中，等待被大多数 Sentinel 选中（waiting to be elected by the majority）。

·       +elected-leader ：赢得指定纪元的选举，可以进行故障迁移操作了。

·       +failover-state-select-slave ：故障转移操作现在处于 select-slave 状态 —— Sentinel 正在寻找可以升级为主服务器的从服务器。

·       no-good-slave ：Sentinel 操作未能找到适合进行升级的从服务器。Sentinel 会在一段时间之后再次尝试寻找合适的从服务器来进行升级，又或者直接放弃执行故障转移操作。

·       selected-slave ：Sentinel 顺利找到适合进行升级的从服务器。

·       failover-state-send-slaveof-noone ：Sentinel 正在将指定的从服务器升级为主服务器，等待升级功能完成。

·       failover-end-for-timeout ：故障转移因为超时而中止，不过最终所有从服务器都会开始复制新的主服务器（slaves will eventually be configured to replicate with the new master anyway）。

·       failover-end ：故障转移操作顺利完成。所有从服务器都开始复制新的主服务器了。

·       +switch-master ：配置变更，主服务器的 IP 和地址已经改变。 这是绝大多数外部用户都关心的信息。

·       +tilt ：进入 tilt 模式。

·       -tilt ：退出 tilt 模式。

```



### Cluster模式

#### Cluster模式介绍

sentinel模式基本可以满足一般生产的需求，具备高可用性。但是当数据量过大到一台服务器存放不下的情况时，主从模式或sentinel模式就不能满足需求了，这个时候需要对存储的数据进行分片，将数据存储到多个Redis实例中。cluster模式的出现就是为了解决单机Redis容量有限的问题，将Redis的数据根据一定的规则分配到多台机器。

cluster可以说是sentinel和主从模式的结合体，通过cluster可以实现主从和master重选功能，所以如果配置两个副本三个分片的话，就需要六个Redis实例。因为Redis的数据是根据一定规则分配到cluster的不同机器的，当数据量过大时，可以新增机器进行扩容。

使用集群，只需要将redis配置文件中的`cluster-enable`配置打开即可。每个集群中至少需要三个主数据库才能正常运行，新增节点非常方便。

cluster集群特点：

```
* 多个redis节点网络互联，数据共享

* 所有的节点都是一主一从（也可以是一主多从），其中从不提供服务，仅作为备用

* 不支持同时处理多个key（如MSET/MGET），因为redis需要把key均匀分布在各个节点上，
  并发量很高的情况下同时创建key-value会降低性能并导致不可预测的行为
  
* 支持在线增加、删除节点

* 客户端可以连接任何一个主节点进行读写

```

#### Cluster模式搭建

- 环境准备：

- ```
  三台机器，分别开启两个redis服务（端口）
  
  192.168.30.128              端口：7001,7002
  
  192.168.30.129              端口：7003,7004
  
  192.168.30.130              端口：7005,7006
  
  ```

  - 修改配置文件：

    

192.168.30.128

```


```

```
# vim /usr/local/redis/cluster/redis_7001.conf

bind 192.168.30.128
port 7001
daemonize yes
pidfile "/var/run/redis_7001.pid"
logfile "/usr/local/redis/cluster/redis_7001.log"
dir "/data/redis/cluster/redis_7001"
#replicaof 192.168.30.129 6379
masterauth 123456
requirepass 123456
appendonly yes
cluster-enabled yes
cluster-config-file nodes_7001.conf
cluster-node-timeout 15000

```

```
# vim /usr/local/redis/cluster/redis_7002.conf

bind 192.168.30.128
port 7002
daemonize yes
pidfile "/var/run/redis_7002.pid"
logfile "/usr/local/redis/cluster/redis_7002.log"
dir "/data/redis/cluster/redis_7002"
#replicaof 192.168.30.129 6379
masterauth "123456"
requirepass "123456"
appendonly yes
cluster-enabled yes
cluster-config-file nodes_7002.conf
cluster-node-timeout 15000

```

