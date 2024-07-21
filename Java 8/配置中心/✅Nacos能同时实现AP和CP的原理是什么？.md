# 典型回答

Nacos在单个集群中同时支持AP和CP两种模式，之所以这么设计是因为Nacos目前在业内主要有两种应用，分别是注册中心和配置中心。

对于注册中心来说，他要提供服务的注册和发现能力，如果使用一个强一致性算法，那么就会对可用性造成一定的影响。而注册中心一旦可用性不能满足了，那么就会影响所有服务的互相调用。而如果一致性没办法做到强一致性的话，最多是可能某个服务不在了，但是还会调用过去，理论上来说会失败，然后重试也是可以接受的。

对于配置中心来说，他的主要职责就是提供统一的配置，一致性是他的一个重点考量，即使损失一点可用性（晚一点推送）也是可以接受的，但是不同的机器接收到配置不一样，这个是不能接受的。

所以，Nacos就同时支持这两种模式了，他在CP方面，采用了JRaft（1.0是Raft），在AP方面，采用了Distro

也就是说，**Nacos，为了同时支持注册中心和配置中心，他通过JRaft协议实现了一个CP的模式，又通过Distro协议实现了一个AP的模式，可以在这两者模式之间进行切换。**

![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1688466255985-e602796f-b454-4ff0-8a1e-86040f80b958.png#averageHue=%23e9e6da&clientId=ubbb7faa6-1f5b-4&from=paste&height=665&id=uee166274&originHeight=715&originWidth=866&originalType=binary&ratio=2&rotation=0&showTitle=false&size=724793&status=done&style=none&taskId=u681e1ee2-e8b4-4ddf-a8d6-79b3e4da97c&title=&width=805)

<br />JRaft是一个纯 Java 的 Raft 算法实现库, 基于百度 braft 实现而来, 使用 Java 重写了所有功能。通过 RAFT 提供的一致性状态机，可以解决复制、修复、节点管理等问题，极大的简化当前分布式系统的设计与实现，让开发者只关注于业务逻辑，将其抽象实现成对应的状态机即可。Raft 可以解决分布式理论中的 CP，即一致性和分区容忍性，并不能解决 Available 的问题。（[https://www.sofastack.tech/projects/sofa-jraft/overview/](https://www.sofastack.tech/projects/sofa-jraft/overview/) ）

Distro是Nacos自研AP分布式协议，是面向临时实例设计的一种分布式协议，保证了在某些Nacos节点宕机后，整个临时处理系统依旧可以正常工作。Distro协议的设计思想：

- Nacos 每个节点是平等的都可以处理写请求，同时把新数据同步到其他节点。
- 每个节点只负责部分数据，定时发送自己负责数据的校验值到其他节点来保持数据一致性。
- 每个节点独立处理读请求，及时从本地发出响应。

 <br />参考：[https://blog.csdn.net/zcrzcrzcrzcrzcr/article/details/122260705](https://blog.csdn.net/zcrzcrzcrzcrzcr/article/details/122260705)
