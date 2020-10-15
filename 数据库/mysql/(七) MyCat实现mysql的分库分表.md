# mycat实现mysql分库分表

> **摘要：** myCat是一个开源的分布式数据库系统，是一个实现了MySQL协议的服务器，前端用户可以把它看作是一个数据库代理，用MySQL客户端工具和命令行访问，而其后端可以用MySQL原生协议与多个MySQL服务器通信，也可以用JDBC协议与大多数主流数据库服务器通信，其核心功能是分表分库，即将一个大表水平分割为N个小表，存储在后端MySQL服务器里或者其他数据库里。

**1. mycat介绍**

```text
myCat是一个开源的分布式数据库系统，是一个实现了MySQL协议的服务器，前端用户可以把它看作是一个数据库代理，用MySQL客户端工具和命令行访问，而其后端可以用MySQL原生协议与多个MySQL服务器通信，也可以用JDBC协议与大多数主流数据库服务器通信，其核心功能是分表分库，即将一个大表水平分割为N个小表，存储在后端MySQL服务器里或者其他数据库里。
```

MyCat发展到目前的版本，已经不是一个单纯的MySQL代理了，它的后端可以支持MySQL、SQL Server、Oracle、DB2、PostgreSQL等主流数据库，也支持MongoDB这种新型NoSQL方式的存储，未来还会支持更多类型的存储。而在最终用户看来，无论是那种存储方式，在MyCat里，都是一个传统的数据库表，支持标准的SQL语句进行数据的操作，这样一来，对前端业务系统来说，可以大幅降低开发难度，提升开发速度

**2.使用介绍：**
本次演示的是基于mysql数据库，通过中间件mycat实现分库分表功能。

**3.环境**
3台物理机linux操作系统
其中两台安装Mysql 5.7
另一台安装Mycat

**4.原理图**

![](assets/v2-c09614a981b064fc3e8f6f7030877832_720w.jpg)

**5.搭建过程**

1. 下载mycat
   [http://mycat.sourceforge.net/](https://link.zhihu.com/?target=http%3A//mycat.sourceforge.net/)
2. 安装及修改配置文件
   下载好安装包，解压即可

Mycat分为3个重要的配置文件，分别为 schema.xml server.xml rule.xml
1.schema.xml
此配置文件是设置整体的scheme拆分任务；节点配置信息；表拆分信息；以及底层mysql数据库登录方式。
具体内容如下：

```
<?xml version="1.0"?>
<!DOCTYPE mycat:schema SYSTEM "schema.dtd">
<mycat:schema xmlns:mycat="http://io.mycat/">

	<!--
	name：为mycat逻辑库的名字，对应server<property name="schemas">mydatabase</property>，
	建议设置跟数据库一样的名称
	checkSQLschema：自动检查逻辑库名称并拼接，true会在sql语句中的表名前拼接逻辑库名，
	例如select * from mydatabase.t_user;
	sqlMaxLimit：查询保护、如果没有写limit条件，会自动拼接。只查询100条。
	-->
	<schema name="mydatabase" checkSQLschema="true" sqlMaxLimit="100">
		<!--
		name:为物理数据库的表名，命名与物理数据库的一致 
		dataNode:为dataNode标签(<dataNode name="dn1" dataHost="dtHost1" database="db1" />)里面的name值
		dataNode里面填写的节点数量必须和rule里面的规则数量一致
		例如rule里面只定义了两个0-1M=0  1M-2M=1那么此处只可以指定两个节点,1M=10000，M为单位万
		primaryKey:为表的ID字段，建议和rule.xml里面指定的ID和物理库的ID一致
		rule：分片规则，对应rule.xml中<tableRule name="student_id">的name
		type：表格类型，默认非global，用于全局表定义
		-->
		<table name="t_user" dataNode="dn1,dn2,dn3" primaryKey="id" rule="auto-sharding-long">
			<!--ER分片注意childTable 标签需要放到table标签内，是主外键关联关系，
				name:为物理数据库的表名，命名与物理数据库的一致 
				primaryKey:为表t_loginlog的ID字段，建议和rule.xml里面指定的ID和物理库的ID一致.
				joinKey：从表t_loginlog的外键字段，需要和物理库的字段名称一致
				parentKey：为主表t_user的字段名，依据此字段做关联，进行ER分片
			-->		
			<childTable name="t_loginlog" primaryKey="id" joinKey="user_id" parentKey="id"></childTable>
		</table>
		<table name="t_student" dataNode="dn1,dn3" primaryKey="id" rule="student_id" />
		<table name="t_dictionaries" dataNode="dn1,dn2,dn3" type="global" />
		<table name="t_teacher" dataNode="dn1" />
    </schema>
		
		<!-- name：节点名称，用于在table标签里面调用
		dataHost:dataHost标签name值(<dataHost name="dtHost1">)
		database:物理数据库名，需要提前创建好实际存在的-->
		<dataNode name="dn1" dataHost="dtHost1" database="db1" />
		<dataNode name="dn2" dataHost="dtHost1" database="db2" />
		<dataNode name="dn3" dataHost="dtHost2" database="db3" />
		
	<!--
	name：节点名称，在上方dataNode标签中调用
	maxCon:底层数据库的链接最大数
	minCon:底层数据库的链接最小数
	balance:值可以为0,1,2,3,分别表示对当前datahost中维护的数据库们的读操作逻辑
	0:不开启读写分离，所有的读写操作都在最小的索引号的writeHost(第一个writeHost标签)
	1：全部的readHost和备用writeHost都参与读数据的平衡，如果读的请求过多，负责写的第一个writeHost也分担一部分
	2 ：所有的读操作，都随机的在所有的writeHost和readHost中进行
	3 ：所有的读操作，都到writeHost对应的readHost上进行（备用writeHost不参加了）,在集群中没有配置ReadHost的情况下,读都到第
	一个writeHost完成
	writeType:控制当前datahost维护的数据库集群的写操作
	0：所有的写操作都在第一个writeHost标签的数据库进行
	1：所有的写操作，都随机分配到所有的writeHost（mycat1.5完全不建议配置了）
	dbtype：数据库类型（不同数据库配置不同名称，mysql）
	dbDriver:数据库驱动，native,动态获取
	switchType：切换的逻辑
	-1：故障不切换
	1：故障切换，当前写操作的writeHost故障，进行切换，切换到下一个writeHost；
	slaveThreshold：标签中的<heartbeat>用来检测后端数据库的心跳sql语句;本属性检查从节点与主节点的同步情况(延迟时间数),配合心
	跳语句show slave status; 读写分离时,所有的readHost的数据都可靠
	-->
	<dataHost name="dtHost1" maxCon="1000" minCon="10" balance="1"
			  writeType="0" dbType="mysql" dbDriver="native" switchType="1"  slaveThreshold="100">
		<!--用于验证心跳，这个是mysql主库的配置-->
		<heartbeat>select user()</heartbeat>
		
		<writeHost host="127.0.0.1" url="192.168.199.11:3306" user="root" password="123456">
			<readHost host="127.0.0.1" url="192.168.199.12:3306" user="root" password="123456" />
		</writeHost>
	
	</dataHost>
	<dataHost name="dtHost2" maxCon="1000" minCon="10" balance="1"
			  writeType="0" dbType="mysql" dbDriver="native" switchType="1"  slaveThreshold="100">
		<!--用于验证心跳，这个是mysql主库的配置-->
		<heartbeat>select user()</heartbeat>
		
		<writeHost host="127.0.0.1" url="192.168.199.13:3306" user="root" password="123456">
			<readHost host="127.0.0.1" url="192.168.199.13:3306" user="root" password="123456" />
		</writeHost>
	
	</dataHost>
</mycat:schema>

<?xml version="1.0"?>
<!DOCTYPE mycat:schema SYSTEM "schema.dtd">
<mycat:schema xmlns:mycat="http://io.mycat/">

    <schema name="mycat" checkSQLschema="false" sqlMaxLimit="1000">     --schema任务配置
        <table name="test1" primaryKey="ID" dataNode="dn1,dn3,dn2" rule="auto-sharding-long"/>  --表test1 拆分的节点以及拆分规则
        <table name="test2" primaryKey="ID"  dataNode="dn2,dn4" rule="rule1"/>
    
    </schema>

    <dataNode name="dn1" dataHost="192.168.0.3" database="db1" />   --节点信息配置
    <dataNode name="dn2" dataHost="192.168.0.3" database="db2" />
    <dataNode name="dn3" dataHost="192.168.0.4" database="db3" />
    <dataNode name="dn4" dataHost="192.168.0.4" database="db4" />

    
    <dataHost name="192.168.0.3" maxCon="1000" minCon="10" balance="0"   --底层mysql登录方式
              writeType="0" dbType="mysql" dbDriver="native" switchType="1"  slaveThreshold="100">
        <heartbeat>select user()</heartbeat>

        <writeHost host="mysql1" url="192.168.0.3:3306" user="root"
                   password="123456">

        </writeHost>


        
    </dataHost>
        <dataHost name="192.168.0.4" maxCon="1000" minCon="10" balance="0"
              writeType="0" dbType="mysql" dbDriver="native" switchType="1"  slaveThreshold="100">
        <heartbeat>select user()</heartbeat>

        <writeHost host="mysql2" url="192.168.0.4:3306" user="root"
                   password="123456">

            
        </writeHost>


    </dataHost>

</mycat:schema>
```

2.server.xml

此文件配置全局防火墙信息。

```
<?xml version="1.0" encoding="UTF-8"?>

<!DOCTYPE mycat:server SYSTEM "server.dtd">
<mycat:server xmlns:mycat="http://io.mycat/">
    <system>
    <property name="useSqlStat">0</property>  <!-- 1为开启实时统计、0为关闭 -->
    <property name="useGlobleTableCheck">0</property>  <!-- 1为开启全加班一致性检测、0为关闭 -->

        <property name="sequnceHandlerType">2</property>
  
        <property name="processorBufferPoolType">0</property>m-vy288c43d05418f4 
    
        <property name="handleDistributedTransactions">0</property>
        
            <!--
            off heap for merge/order/group/limit      1开启   0关闭
        -->
        <property name="useOffHeapForMerge">1</property>

        <!--
            单位为m
        -->
        <property name="memoryPageSize">1m</property>

        <!--
            单位为k
        -->
        <property name="spillsFileBufferSize">1k</property>

        <property name="useStreamOutput">0</property>

        <!--
            单位为m
        -->
        <property name="systemReserveMemorySize">384m</property>


        <!--是否采用zookeeper协调切换  -->
        <property name="useZKSwitch">true</property>


    </system>
    
 
    <firewall> 
       <whitehost>
          <host host="127.0.0.1" user="root"/>
          <host host="localhost" user="root"/>
       </whitehost>
       <blacklist check="false">
       </blacklist>
    </firewall>

    
    <user name="root">
        <property name="password">123456</property>
        <property name="schemas">mysql</property>
        
</mycat:server>
```

3.rule.xml
此配置文件配置的是表的拆分规则，以及拆分规则底层文件配置，
具体内容如下：

```
<?xml version="1.0" encoding="UTF-8"?>
c language governing permissions and - limitations 
    under the License. -->
<!DOCTYPE mycat:rule SYSTEM "rule.dtd">
<mycat:rule xmlns:mycat="http://io.mycat/">
    <tableRule name="test1"> --表test1拆分规则配置
        <rule>
            <columns>ID</columns>
            <algorithm>rang-long</algorithm>
        </rule>
    </tableRule>

    <tableRule name="test2">
        <rule>
            <columns>ID</columns>
            <algorithm>func1</algorithm>
        </rule>
    </tableRule>

    

    <function name="murmur"
        class="io.mycat.route.function.PartitionByMurmurHash">
        <property name="seed">0</property>
        <property name="count">2</property>
        <property name="virtualBucketTimes">160</property>
        
    </function>

    <function name="crc32slot"
              class="io.mycat.route.function.PartitionByCRC32PreSlot">
        <property name="count">2</property>
    </function>
    <function name="hash-int"
        class="io.mycat.route.function.PartitionByFileMap">
        <property name="mapFile">partition-hash-int.txt</property>
    </function>
    <function name="rang-long"
        class="io.mycat.route.function.AutoPartitionByLong">
        <property name="mapFile">autopartition-long.txt</property>
    </function>
    <function name="mod-long" class="io.mycat.route.function.PartitionByMod">
        <!-- how many data nodes -->
        <property name="count">3</property>
    </function>

    <function name="func1" class="io.mycat.route.function.PartitionByLong">
        <property name="partitionCount">8</property>
        <property name="partitionLength">128</property>
    </function>
    <function name="latestMonth"
        class="io.mycat.route.function.LatestMonthPartion">
        <property name="splitOneDay">24</property>
    </function>
    <function name="partbymonth"
        class="io.mycat.route.function.PartitionByMonth">
        <property name="dateFormat">yyyy-MM-dd</property>
        <property name="sBeginDate">2015-01-01</property>
    </function>
    
    <function name="rang-mod" class="io.mycat.route.function.PartitionByRangeMod">
            <property name="mapFile">partition-range-mod.txt</property>
    </function>
    
    <function name="jump-consistent-hash" class="io.mycat.route.function.PartitionByJumpConsistentHash">
        <property name="totalBuckets">3</property>
    </function>
</mycat:rule>
```

