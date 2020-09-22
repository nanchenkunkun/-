```
redis最开始使用主从模式做集群，若master宕机需要手动配置slave转为master；后来为了高可用提出来哨兵模式，该模式下有一个哨兵监视master和slave，若master宕机可自动将slave转为master，但它也有一个问题，就是不能动态扩充；所以在3.x提出cluster集群模式。
```

## **一、redis-cluster设计**

从redis 3.0之后版本支持redis-cluster集群，Redis-Cluster采用无中心结构，每个节点保存数据和整个集群状态,每个节点都和其他所有节点连接。其redis-cluster架构图如下：

![](assets/12185313-0f55e1cc574cae70.webp)

其结构特点：
 1、所有的redis节点彼此互联(PING-PONG机制),内部使用二进制协议优化传输速度和带宽。
 2、节点的fail是通过集群中超过半数的节点检测失效时才生效。
 3、客户端与redis节点直连,不需要中间proxy层.客户端不需要连接集群所有节点,连接集群中任何一个可用节点即可。
 4、redis-cluster把所有的物理节点映射到[0-16383]slot上（不一定是平均分配）,cluster 负责维护node<->slot<->value。
 5、Redis集群预分好16384个桶，当需要在 Redis 集群中放置一个 key-value 时，根据 CRC16(key) mod 16384的值，决定将一个key放到哪个桶中。

**a.redis cluster节点分配**
 现在我们是三个主节点分别是：A, B, C 三个节点，它们可以是一台机器上的三个端口，也可以是三台不同的服务器。那么，采用哈希槽 (hash slot)的方式来分配16384个slot 的话，它们三个节点分别承担的slot 区间是：

- 节点A覆盖0－5460;

- 节点B覆盖5461－10922;

- 节点C覆盖10923－16383.

  获取数据:
   如果存入一个值，按照redis cluster哈希槽的[算法](http://lib.csdn.net/base/datastructure)： CRC16('key')384 = 6782。 那么就会把这个key 的存储分配到 B 上了。同样，当我连接(A,B,C)任何一个节点想获取'key'这个key时，也会这样的算法，然后内部跳转到B节点上获取数据

  新增一个主节点:
   新增一个节点D，redis cluster的这种做法是从各个节点的前面各拿取一部分slot到D上，我会在接下来的实践中实验。大致就会变成这样：

- 节点A覆盖1365-5460

- 节点B覆盖6827-10922

- 节点C覆盖12288-16383

- 节点D覆盖0-1364,5461-6826,10923-12287

同样删除一个节点也是类似，移动完成后就可以删除这个节点了。

**b.Redis Cluster主从模式**
 redis cluster 为了保证数据的高可用性，加入了主从模式，一个主节点对应一个或多个从节点，主节点提供数据存取，从节点则是从主节点拉取数据备份，当这个主节点挂掉后，就会有这个从节点选取一个来充当主节点，从而保证集群不会挂掉

上面那个例子里, 集群有ABC三个主节点, 如果这3个节点都没有加入从节点，如果B挂掉了，我们就无法访问整个集群了。A和C的slot也无法访问。

所以我们在集群建立的时候，一定要为每个主节点都添加了从节点, 比如像这样, 集群包含主节点A、B、C, 以及从节点A1、B1、C1, 那么即使B挂掉系统也可以继续正确工作。

B1节点替代了B节点，所以Redis集群将会选择B1节点作为新的主节点，集群将会继续正确地提供服务。 当B重新开启后，它就会变成B1的从节点。

不过需要注意，如果节点B和B1同时挂了，Redis集群就无法继续正确地提供服务了。

**二、redis集群的搭建**
集群中至少应该有奇数个节点，所以至少有三个节点，每个节点至少有一个备份节点，所以下面使用6节点（主节点、备份节点由redis-cluster集群确定）
**1、安装redis节点指定端口**
解压redis压缩包，编译安装

```
[root@localhost redis-3.2.0]# tar xzf redis-3.2.0.tar.gz
[root@localhost redis-3.2.0]# cd redis-3.2.0
[root@localhost redis-3.2.0]# make
[root@localhost redis01]# make install PREFIX=/usr/andy/redis-cluster
```

在redis-cluster下 修改bin文件夹为redis01,复制redis.conf配置文件
 创建目录redis-cluster并在此目录下再创建7000 7001 7002 7003 7004 7005共6个目录，在7000中创建配置文件redis.conf，内容如下：

```
        daemonize yes #后台启动
        port 7001 #修改端口号，从7001到7006
        cluster-enabled yes #开启cluster，去掉注释
        cluster-config-file nodes.conf #自动生成
        cluster-node-timeout 15000 #节点通信时间
        appendonly yes #持久化方式
```

同时把redis.conf复制到其它目录中

**2、安装redis-trib所需的 ruby脚本**
注意：centos7默认的ruby版本太低(2.0)，要卸载重装(最低2.2)

```
yum remove ruby
yum install ruby
yum install rubygems
```

复制redis解压文件src下的redis-trib.rb文件到redis-cluster目录并安装gem

```
gem install redis-3.x.x.gem
```

若不想安装src目录下的gem，也可以直接`gem install redis`。

注意，gem install可能会报错
 Unable to require openssl,install OpenSSL and rebuild ruby (preferred) or use ....
 解决步骤：

1. yum install openssl-devel -y
2. 在ruby安装包/root/ruby-x.x.x/ext/openssl，执行ruby ./extconf.rb
3. 执行make,若出现make: *** No rule to make target `/include/ruby.h', needed by`ossl.o'.  Stop.;在Makefile顶部中的增加`top_srcdir = ../..` 
4. 执行make install

**3、启动所有的redis节点**
 可以写一个命令脚本start-all.sh

```
cd 7000
redis-server redis.conf
cd ..
cd 7001
redis-server redis.conf
cd ..
cd 7002
redis-server redis.conf
cd ..
cd 7003
redis-server redis.conf
cd ..
cd 7004
redis-server redis.conf
cd ..
cd 7005
redis-server redis.conf
cd ..
```

设置权限启动

```
[root@localhost redis-cluster]# chmod 777 start-all.sh 
[root@localhost redis-cluster]# ./start-all.sh 
```

查看redis进程启动状态

```
[root@localhost redis-4.0.2]# ps -ef|grep cluster
root      54956      1  0 19:17 ?        00:00:00 redis-server *:7000 [cluster]
root      54961      1  0 19:17 ?        00:00:00 redis-server *:7001 [cluster]
root      54966      1  0 19:17 ?        00:00:00 redis-server *:7002 [cluster]
root      54971      1  0 19:17 ?        00:00:00 redis-server *:7003 [cluster]
root      54976      1  0 19:17 ?        00:00:00 redis-server *:7004 [cluster]
root      54981      1  0 19:17 ?        00:00:00 redis-server *:7005 [cluster]
root      55071  24089  0 19:24 pts/0    00:00:00 grep --color=auto cluster
```

可以看到redis的6个节点已经启动成功
**注意：这里并没有创建集群**

**4、使用redis-trib.rb创建集群**
注意：redis-trib.rb在redis/src目录下。

```
./redis-trib.rb create --replicas 1 127.0.0.1:7001 127.0.0.1:7002 127.0.0.1:7003 127.0.0.1:7004 127.0.0.1:7005 127.0.0.1:7000
```

使用create命令 --replicas 1 参数表示为每个主节点创建一个从节点，其他参数是实例的地址集合。

```
[root@localhost redis]# ./src/redis-trib.rb create --replicas 1 127.0.0.1:7001 127.0.0.1:7002 127.0.0.1:7003 127.0.0.1:7004 127.0.0.1:7005 127.0.0.1:7000
>>> Creating cluster
>>> Performing hash slots allocation on 6 nodes...
Using 3 masters:
127.0.0.1:7001
127.0.0.1:7002
127.0.0.1:7003
Adding replica 127.0.0.1:7004 to 127.0.0.1:7001
Adding replica 127.0.0.1:7005 to 127.0.0.1:7002
Adding replica 127.0.0.1:7000 to 127.0.0.1:7003
M: f4ee0a501f9aaf11351787a46ffb4659d45b7bd7 127.0.0.1:7001
   slots:0-5460 (5461 slots) master
M: 671a0524a616da8b2f50f3d11a74aaf563578e41 127.0.0.1:7002
   slots:5461-10922 (5462 slots) master
M: 18948dab5b07e3726afd1b6a42d5bf6e2f411ba1 127.0.0.1:7003
   slots:10923-16383 (5461 slots) master
S: 34e322ca50a2842e9f3664442cb11c897defba06 127.0.0.1:7004
   replicates f4ee0a501f9aaf11351787a46ffb4659d45b7bd7
S: 62a00566233fbff4467c4031345b1db13cf12b46 127.0.0.1:7005
   replicates 671a0524a616da8b2f50f3d11a74aaf563578e41
S: 2cb649ad3584370c960e2036fb01db834a546114 127.0.0.1:7000
   replicates 18948dab5b07e3726afd1b6a42d5bf6e2f411ba1
Can I set the above configuration? (type 'yes' to accept): yes
>>> Nodes configuration updated
>>> Assign a different config epoch to each node
>>> Sending CLUSTER MEET messages to join the cluster
Waiting for the cluster to join...
>>> Performing Cluster Check (using node 127.0.0.1:7001)
M: f4ee0a501f9aaf11351787a46ffb4659d45b7bd7 127.0.0.1:7001
   slots:0-5460 (5461 slots) master
   1 additional replica(s)
M: 671a0524a616da8b2f50f3d11a74aaf563578e41 127.0.0.1:7002
   slots:5461-10922 (5462 slots) master
   1 additional replica(s)
S: 2cb649ad3584370c960e2036fb01db834a546114 127.0.0.1:7000
   slots: (0 slots) slave
   replicates 18948dab5b07e3726afd1b6a42d5bf6e2f411ba1
S: 34e322ca50a2842e9f3664442cb11c897defba06 127.0.0.1:7004
   slots: (0 slots) slave
   replicates f4ee0a501f9aaf11351787a46ffb4659d45b7bd7
M: 18948dab5b07e3726afd1b6a42d5bf6e2f411ba1 127.0.0.1:7003
   slots:10923-16383 (5461 slots) master
   1 additional replica(s)
S: 62a00566233fbff4467c4031345b1db13cf12b46 127.0.0.1:7005
   slots: (0 slots) slave
   replicates 671a0524a616da8b2f50f3d11a74aaf563578e41
[OK] All nodes agree about slots configuration.
>>> Check for open slots...
>>> Check slots coverage...
[OK] All 16384 slots covered.
```

上面显示创建成功，有3个主节点，3个从节点，每个节点都是成功连接状态。

**三、redis集群的测试**
测试存取值，客户端连接集群redis-cli需要带上 -c ，redis-cli -c -p 端口号

```ruby
[root@localhost redis]# ./redis-cli -c -p 7001
127.0.0.1:7001> set name andy
-> Redirected to slot [5798] located at 127.0.0.1:7002
OK
127.0.0.1:7002> get name
"andy"
127.0.0.1:7002> 
```

根据redis-cluster的key值分配，name应该分配到节点7002[5461-10922]上，上面显示redis cluster自动从7001跳转到了7002节点。

测试一下7000从节点获取name值

```
[root@localhost redis]# ./redis-cli -c -p 7000
127.0.0.1:7000> get name
-> Redirected to slot [5798] located at 127.0.0.1:7002
"andy"
127.0.0.1:7002> 
```

**四、集群节点选举**
现在模拟将7002节点挂掉，按照redis-cluster原理会选举会将 7002的从节点7005选举为主节点

```
[root@localhost redis-cluster]# ps -ef | grep redis
root       7966      1  0 12:50 ?        00:00:29 ./redis-server 127.0.0.1:7000 [cluster]
root       7950      1  0 12:50 ?        00:00:28 ./redis-server 127.0.0.1:7001 [cluster]
root       7952      1  0 12:50 ?        00:00:29 ./redis-server 127.0.0.1:7002 [cluster]
root       7956      1  0 12:50 ?        00:00:29 ./redis-server 127.0.0.1:7003 [cluster]
root       7960      1  0 12:50 ?        00:00:29 ./redis-server 127.0.0.1:7004 [cluster]
root       7964      1  0 12:50 ?        00:00:29 ./redis-server 127.0.0.1:7005 [cluster]
root      11346  10581  0 14:57 pts/2    00:00:00 grep --color=auto redis
[root@localhost redis-cluster]# kill 7952
```

在查看集群中的7002节点

```
[root@localhost src]# ./redis-trib.rb check 127.0.0.1:7002
>>> Performing Cluster Check (using node 127.0.0.1:7002)
S: 671a0524a616da8b2f50f3d11a74aaf563578e41 127.0.0.1:7002
   slots: (0 slots) slave
   replicates 62a00566233fbff4467c4031345b1db13cf12b46
M: 18948dab5b07e3726afd1b6a42d5bf6e2f411ba1 127.0.0.1:7003
   slots:10923-16383 (5461 slots) master
   1 additional replica(s)
M: 62a00566233fbff4467c4031345b1db13cf12b46 127.0.0.1:7005
   slots:5461-10922 (5462 slots) master
   1 additional replica(s)
M: f4ee0a501f9aaf11351787a46ffb4659d45b7bd7 127.0.0.1:7001
   slots:0-5460 (5461 slots) master
   1 additional replica(s)
S: 34e322ca50a2842e9f3664442cb11c897defba06 127.0.0.1:7004
   slots: (0 slots) slave
   replicates f4ee0a501f9aaf11351787a46ffb4659d45b7bd7
S: 2cb649ad3584370c960e2036fb01db834a546114 127.0.0.1:7000
   slots: (0 slots) slave
   replicates 18948dab5b07e3726afd1b6a42d5bf6e2f411ba1
[OK] All nodes agree about slots configuration.
>>> Check for open slots...
>>> Check slots coverage...
[OK] All 16384 slots covered.
```

可以看到集群连接不了7002节点，而7005有原来的S转换为M节点，代替了原来的7002节点。我们可以获取name值：

```
[root@localhost redis]# ./redis-cli -c -p 7001
127.0.0.1:7001> get name
-> Redirected to slot [5798] located at 127.0.0.1:7005
"andy"
127.0.0.1:7005> 
127.0.0.1:7005> 
```

从7001节点连入，自动跳转到7005节点，并且获取name值。

现在我们将7002节点恢复，看是否会自动加入集群中以及充当的M还是S节点。

```csharp
[root@localhost redis-cluster]# cd 7002
[root@localhost 7002]# ./redis-server redis.conf 
[root@localhost 7002]# 
```

再check一下7002节点，可以看到7002节点变成了7005的从节点。