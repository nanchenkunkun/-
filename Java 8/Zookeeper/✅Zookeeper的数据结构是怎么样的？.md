## 数据模型

**ZK中数据是以目录结构的形式存储的**。其中的每一个存储数据的节点都叫做Znode，每个Znode都有一个唯一的路径标识。和目录结构类似，每一个节点都可以可有子节点（临时节点除外）。节点中可以存储数据和状态信息，每个Znode上可以配置监视器（watcher），用于监听节点中的数据变化。节点不支持部分读写，而是一次性完整读写。

## 节点

Znode有四种类型，PERSISTENT（持久节点）、PERSISTENT_SEQUENTIAL（持久的连续节点）、EPHEMERAL（临时节点）、EPHEMERAL_SEQUENTIAL（临时的连续节点）

Znode的类型在创建时确定并且之后不能再修改

### 临时节点

临时节点的生命周期和客户端会话绑定。也就是说，如果客户端会话失效，那么这个节点就会自动被清除掉。

```
String root = "/ephemeral";
String createdPath = zk.create(root, root.getBytes(),
          Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
System.out.println("createdPath = " + createdPath);

String path = "/ephemeral/test01" ; 
createdPath = zk.create(path, path.getBytes(),
            Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
System.out.println("createdPath = " + createdPath);
Thread.sleep(1000 * 20); // 等待20秒关闭ZooKeeper连接
zk.close(); // 关闭连接后创建的临时节点将自动删除
```

> 临时节点不能有子节点


### 持久节点

所谓持久节点，是指在节点创建后，就一直存在，直到有删除操作来主动清除这个节点——不会因为创建该节点的客户端会话失效而消失。

```
String root = "/computer";
String createdPath = zk.create(root, root.getBytes(),
       Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
System.out.println("createdPath = " + createdPath);
```

### 临时顺序节点

临时节点的生命周期和客户端会话绑定。也就是说，如果客户端会话失效，那么这个节点就会自动被清除掉。注意创建的节点会自动加上编号。

```
String root = "/ephemeral";
String createdPath = zk.create(root, root.getBytes(),
          Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
System.out.println("createdPath = " + createdPath);

String path = "/ephemeral/test01" ; 
createdPath = zk.create(path, path.getBytes(),
            Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
System.out.println("createdPath = " + createdPath);
Thread.sleep(1000 * 20); // 等待20秒关闭ZooKeeper连接
zk.close(); // 关闭连接后创建的临时节点将自动删除
```

输出结果：

```
type = None
createdPath = /ephemeral/test0000000003
createdPath = /ephemeral/test0000000004
createdPath = /ephemeral/test0000000005
createdPath = /ephemeral/test0000000006
```

### 持久顺序节点

这类节点的基本特性和持久节点类型是一致的。额外的特性是，在ZooKeeper中，每个父节点会为他的第一级子节点维护一份时序，会记录每个子节点创建的先后顺序。基于这个特性，在创建子节点的时候，可以设置这个属性，那么在创建节点过程中，ZooKeeper会自动为给定节点名加上一个数字后缀，作为新的节点名。这个数字后缀的范围是整型的最大值。

```
String root = "/computer";
String createdPath = zk.create(root, root.getBytes(),
       Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
System.out.println("createdPath = " + createdPath);
for (int i=0; i<5; i++) {
   String path = "/computer/node";
   String createdPath = zk.create(path, path.getBytes(),
       Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT_SEQUENTIAL);
   System.out.println("createdPath = " + createdPath);
}
zk.close();
```

运行结果：

```
createdPath = /computer
createdPath = /computer/node0000000000
createdPath = /computer/node0000000001
createdPath = /computer/node0000000002
createdPath = /computer/node0000000003
createdPath = /computer/node0000000004
结果中的0000000000~0000000004都是自动添加的序列号
```

节点中除了可以存储数据，还包含状态信息。

## ACL

每个znode被创建时都会带有一个ACL列表，用于决定谁可以对它执行何种操作。

## 观察（watcher）

Watcher 在 ZooKeeper 是一个核心功能，Watcher 可以监控目录节点的数据变化以及子目录的变化，一旦这些状态发生变化，服务器就会通知所有设置在这个目录节点上的 Watcher，从而每个客户端都很快知道它所关注的目录节点的状态发生变化，而做出相应的反应

可以设置观察的操作：`exists`,`getChildren`,`getData` 可以触发观察的操作：`create`,`delete`,`setData`

znode以某种方式发生变化时，“观察”（watch）机制可以让客户端得到通知。可以针对ZooKeeper服务的“操作”来设置观察，该服务的其他 操作可以触发观察。比如，客户端可以对某个客户端调用exists操作，同时在它上面设置一个观察，如果此时这个znode不存在，则exists返回 false，如果一段时间之后，这个znode被其他客户端创建，则这个观察会被触发，之前的那个客户端就会得到通知。

