# 典型回答

RabbitMQ可以通过多种方式来实现高可用性，以确保在硬件故障或其他不可预测的情况下，消息队列系统仍然能够正常运行。RabbitMQ有三种模式：单机模式、普通集群模式、镜像集群模式。

其中单机模式一般用于demo搭建，不适合在生产环境中使用。剩下的集群模式和镜像模式都可以帮助我们实现不同程度的高可用。

**普通集群模式**

普通集群模式，就是将 RabbitMQ 实例部署到多台服务器上，多个实例之间协同工作，共享队列和交换机的元数据，并通过内部通信协议来协调消息的传递和管理。

在这种模式下，我们创建的Queue，它的元数据（配置信息）会在集群中的所有实例间进行同步，但是队列中的消息只会存在于一个 RabbitMQ 实例上，而不会同步到其他队列。

![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1693630663836-60ce6ae3-fa52-4d00-a7e4-94a3e651d014.png#averageHue=%23fafafa&clientId=u74bec87c-7f9f-4&from=paste&height=613&id=u619607e4&originHeight=674&originWidth=891&originalType=binary&ratio=1.100000023841858&rotation=0&showTitle=false&size=70610&status=done&style=none&taskId=ue0545897-2e13-4f77-a7a9-4662710eeea&title=&width=809.9999824437232)

当我们消费消息的时候，如果消费者连接到了未保存消息的实例，那么那个实例会通过元数据定位到消息所在的实例，拉取数据过来发送给消费者进行消费。

消息的发送也是一样的，当发送者连接到了一个不保存消息的实例时，也会被转发到保存消息的实例上进行写操作。

这种集群模式下，每一个实例中的元数据是一样的，大家都是完整的数据。但是队列中的消息数据，在不同的实例上保存的是不一样的。这样通过增加实例的方式就可以提升整个集群的消息存储量，以及性能。

这种方式在高可用上有一定的帮助，不至于一个节点挂了就全都挂了。但是也还有缺点，至少这个实例上的数据是没办法被读写了。

**镜像模式**

顾名思义，就是每一台RabbitMQ都像一个镜像一样，存储的内容都是一样的。这种模式下，Queue的元数据和消息数据不再是单独存储在某个实例上，而是集群中的所有实例上都存储一份。

这样每次在消息写入的时候，就需要在集群中的所有实例上都同步一份，这样即使有一台实例发生故障，剩余的实例也可以正常提供完整的数据和服务。

![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1693632006996-be3d598e-1320-4aee-ba0a-07c8667e577c.png#averageHue=%23faf9f9&clientId=u74bec87c-7f9f-4&from=paste&height=565&id=ub6ef7692&originHeight=622&originWidth=856&originalType=binary&ratio=1.100000023841858&rotation=0&showTitle=false&size=73103&status=done&style=none&taskId=u0a859ea4-76e3-4562-8206-2e62859fb91&title=&width=778.1818013151819)

这种模式下，就保障了RabbitMQ的高可用。
