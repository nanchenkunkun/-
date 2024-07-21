# 典型回答

下图，是Dubbo的整体设计的一张图片，网上的很多资料都是基于这个官方给的图片展开的：

![](https://cdn.nlark.com/yuque/0/2024/jpeg/5378072/1707628047335-1ab16694-2c3c-4f4f-8b5a-31dd446ef402.jpeg#averageHue=%23cfe0b9&clientId=ue5687508-e428-4&from=paste&id=u6d543e41&originHeight=674&originWidth=900&originalType=url&ratio=1.5&rotation=0&showTitle=false&status=done&style=none&taskId=u8c9ed0ff-1c13-4481-8513-df795391ce9&title=)

但是这样图片太复杂了，如果大家想要自己看源码，可以结合它来看，但是面试的时候回答问题，就不可能搞的这么细，所以主要还是介绍思想。（这里讲实现方式，具体的调用过程见：[https://www.yuque.com/hollis666/fo22bm/nn5fo1yz2b2f9lgy](https://www.yuque.com/hollis666/fo22bm/nn5fo1yz2b2f9lgy) ）

大家看着这样图，其实是有分层的。一共分成了以下几个层：

- **config 配置层**：对外配置接口，以 ServiceConfig, ReferenceConfig 为中心，可以直接初始化配置类，也可以通过 spring 解析配置生成配置类
- **proxy 服务代理层**：服务接口透明代理，生成服务的客户端 Stub 和服务器端 Skeleton, 以 ServiceProxy 为中心，扩展接口为 ProxyFactory
- **registry 注册中心层**：封装服务地址的注册与发现，以服务 URL 为中心，扩展接口为 RegistryFactory, Registry, RegistryService
- **cluster 路由层**：封装多个提供者的路由及负载均衡，并桥接注册中心，以 Invoker 为中心，扩展接口为 Cluster, Directory, Router, LoadBalance
- **monitor 监控层**：RPC 调用次数和调用时间监控，以 Statistics 为中心，扩展接口为 MonitorFactory, Monitor, MonitorService
- **protocol 远程调用层**：封装 RPC 调用，以 Invocation, Result 为中心，扩展接口为 Protocol, Invoker, Exporter
- **exchange 信息交换层**：封装请求响应模式，同步转异步，以 Request, Response 为中心，扩展接口为 Exchanger, ExchangeChannel, ExchangeClient, ExchangeServer
- **transport 网络传输层**：抽象 mina 和 netty 为统一接口，以 Message 为中心，扩展接口为 Channel, Transporter, Client, Server, Codec
- **serialize 数据序列化层**：可复用的一些工具，扩展接口为 Serialization, ObjectInput, ObjectOutput, ThreadPool

我把这里面比较重要的部分单独拿出来，试着简化一下整个调用过程，让大家更容易理解dubbo实现远程调用的主要原理。

Dubbo 实现像本地方法一样调用远程方法的核心技术是**动态代理**。Dubbo 使用 JDK 动态代理或者字节码增强技术，生成一个代理类，该代理类实现了本地接口，具有本地接口的所有方法。在调用本地接口方法时，会通过代理类的 invoke 方法将请求转发到远程服务提供者上。<br />![image.png](https://cdn.nlark.com/yuque/0/2024/png/5378072/1707627365469-5b08fb9e-1d79-40f4-bcb3-ea53ddefb490.png#averageHue=%23fefdf6&clientId=u4831a29f-4d6c-4&from=paste&height=460&id=u4e82f74d&originHeight=690&originWidth=1263&originalType=binary&ratio=1&rotation=0&showTitle=false&size=68111&status=done&style=none&taskId=u77da2812-6283-4325-835d-46bfb026544&title=&width=842)

**生成代理类**，Dubbo 在启动时会扫描配置文件（注解）中指定的服务接口，并根据服务接口生成一个**代理类**。这个代理类实现了服务接口，并且在调用服务接口的方法时，会将参数封装成请求消息，然后通过网络传输给服务提供方。

**序列化与反序列化**，为了在网络上发送和接收数据，Dubbo将方法调用的参数和返回值进行序列化（转换成字节序列）和反序列化（从字节序列还原数据）。Dubbo目前支持多种序列化协议：Dubbo支持多种序列化协议，如Hessian、Java自带的序列化、JSON等，以适应不同的性能和兼容性需求。

**网络通信**，Dubbo 支持多种通信协议，包括 Dubbo 协议、HTTP 协议、Hessian 协议等。在配置文件中指定了要使用的通信协议后，Dubbo 会根据协议的不同，选择不同的序列化方式，将请求消息序列化成二进制流并发送给服务提供方。

**服务注册与发现，**Dubbo使用注册中心（如Zookeeper、Nacos等）管理服务的提供者和消费者信息。服务提供者在启动时将自己提供的服务注册到注册中心，服务消费者通过注册中心查找所需的服务并获取服务提供者的地址。

**负载均衡**，Dubbo 支持多种负载均衡算法，包括轮询、随机、加权随机、最小活跃数等。在客户端发起调用时，Dubbo 会根据负载均衡算法选择一台服务提供方进行调用。

**远程服务执行**，当客户端发起远程调用后，服务提供方接收到请求后，会根据请求中的服务接口名和方法名，找到对应的实现类和方法，并将请求消息反序列化成参数列表，最终调用服务实现类的方法，并将执行结果序列化成响应消息返回给客户端。
