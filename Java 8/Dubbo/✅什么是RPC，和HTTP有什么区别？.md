# 典型回答


RPC 是Remote Procedure Call的缩写，译为远程过程调用。要想实现RPC通常需要包含传输协议和序列化协议的实现。

而我们熟知的HTTP，他的中文名叫超文本传输协议，所以他就是一种传输协议。所以，我们可以认为RPC和HTTP并不是同一个维度的两个概念。只不过他们都是可以作为远程调用的，所以经常拿来对比。

RPC的具体实现上，可以像HTTP一样，基于TCP协议来实现，也可以直接基于HTTP协议实现。

**RPC主要用于公司内部服务之间的互相调用，所以他性能消耗低，传输效率高，服务治理方便。**<br />**而HTTP主要用于对外的异构环境，浏览器调用，APP接口调用，第三方接口调用等等。**

[✅为什么RPC要比HTTP更快一些？](https://www.yuque.com/hollis666/fo22bm/dqg0utz3a025y1oi?view=doc_embed)

# 扩展知识
### 为什么需要远程调用

如今的大型网站都是分布式部署的。拿一个下单流程来说，可能涉及到物流、支付、库存、红包等多个系统后，而多个系统又是分别部署在不同的机器上的，分别由不同的团队负责。而要想实现下单流程，就需要用到远程调用。

```
下单{
    库存->减少库存
    支付->扣款
    红包->红包抵用
    物流->生成物流信息
}
```

### 到底什么是远程过程调用

RPC 是指计算机 A 上的进程，调用另外一台计算机 B 上的进程，其中 A 上的调用进程被挂起，而 B 上的被调用进程开始执行，当值返回给 A 时，A 进程继续执行。调用方可以通过使用参数将信息传送给被调用方，而后可以通过传回的结果得到信息。而这一过程，对于开发人员来说是透明的。

就像后厨的例子一样，服务员把菜单传给后厨，厨师告诉备菜师和洗菜师开始工作，然后他等待他们完成工作。备菜师和洗菜师工作完之后，厨师开始炒菜。这个过程对于服务员来说其实是透明的，他不需要关心到底后厨是怎么做菜的。

![](http://www.hollischuang.com/wp-content/uploads/2018/12/15442669374513.gif#id=kAiwO&originHeight=228&originWidth=385&originalType=binary&ratio=1&rotation=0&showTitle=false&status=done&style=none&title=)￼

由于各服务部署在不同机器上，要想在服务间进行远程调用免不了网络通信过程，服务消费方每调用一个服务都要写一坨网络通信相关的代码，不仅复杂而且极易出错。

如果有一种方式能让我们像调用本地服务一样调用远程服务，而让调用者对网络通信这些细节透明，那么将大大提高生产力，比如服务消费方在执行`orderService.buy("HHKB键盘")`时，实质上调用的是远端的服务。这种方式其实就是RPC。而提供了这种功能的工具我们称之为RPC框架。

在RPC框架中主要有三个角色：Provider、Consumer和Registry。如下图所示：

![](http://www.hollischuang.com/wp-content/uploads/2018/12/15442678994512.jpg#id=N9qUB&originHeight=234&originWidth=513&originalType=binary&ratio=1&rotation=0&showTitle=false&status=done&style=none&title=)

Server: 暴露服务的服务提供方。 <br />Client: 调用远程服务的服务消费方。 <br />Registry: 服务注册与发现的注册中心。

服务提供方和服务消费方都比较好理解，就是后厨的洗菜师和厨师啦。厨师就是服务消费方，洗菜师就是服务提供方。厨师依赖洗菜师提供的服务。

服务注册中心又是个什么东西呢？

其实这个也比较好理解。对于那种很大的饭店来说，厨师可能有很多（集群部署），洗菜师也有很多（集群部署）。而厨师想要洗菜师帮忙洗菜的时候，他不会直接找某个洗菜师，而是通知一个中间人，这个人可能是洗菜师团队的领导，也可能就是一个专门协调后厨的人员。他知道整个厨房有多少洗菜师，也知道哪个洗菜师今天来上班了（需要先进行服务注册）。而且，他还可以根据各个洗菜师的忙碌情况动态分配任务（负载均衡）。

这个中间人就是服务注册中心。

![](http://www.hollischuang.com/wp-content/uploads/2018/12/15442683038459.jpg#id=jJ7rJ&originHeight=330&originWidth=500&originalType=binary&ratio=1&rotation=0&showTitle=false&status=done&style=none&title=)￼

服务提供者启动后主动向注册中心注册机器ip、port以及提供的服务列表； 服务消费者启动时向注册中心获取服务提供方地址列表，可实现软负载均衡和Failover；

### 实现RPC需要用到的技术

一个成熟的RPC框架需要考虑的问题有很多，这里只介绍实现一个远程调用需要用到的基本技术，感兴趣的朋友可以找一些开源的RPC框架代码来看下。

**动态代理**

生成 client stub和server stub需要用到 **Java 动态代理技术 **，我们可以使用JDK原生的动态代理机制，可以使用一些开源字节码工具框架 如：CgLib、Javassist等。

**序列化** 为了能在网络上传输和接收 Java对象，我们需要对它进行序列化和反序列化操作。

可以使用Java原生的序列化机制，但是效率非常低，推荐使用一些开源的、成熟的序列化技术，例如：protobuf、Thrift、hessian、Kryo、Msgpack

**NIO**

当前很多RPC框架都直接基于netty这一IO通信框架，比如阿里巴巴的HSF、dubbo，Hadoop Avro，推荐使用Netty 作为底层通信框架。

**服务注册中心** 可选技术： Redis、Zookeeper、Consul、Etcd

### 开源RPC框架

**Dubbo**

Dubbo 是阿里巴巴公司开源的一个Java高性能优秀的服务框架，使得应用可通过高性能的 RPC 实现服务的输出和输入功能，可以和 Spring框架无缝集成。目前已经进入Apache孵化器。

**Motan**

Motan是新浪微博开源的一个Java RPC框架。2016年5月开源。Motan 在微博平台中已经广泛应用，每天为数百个服务完成近千亿次的调用。

**gRPC**

gRPC是Google开发的高性能、通用的开源RPC框架，其由Google主要面向移动应用开发并基于HTTP/2协议标准而设计，基于ProtoBuf(Protocol Buffers)序列化协议开发，且支持众多开发语言。本身它不是分布式的，所以要实现上面的框架的功能需要进一步的开发。

**thrift**

thrift是Apache的一个跨语言的高性能的服务框架，也得到了广泛的应用。

