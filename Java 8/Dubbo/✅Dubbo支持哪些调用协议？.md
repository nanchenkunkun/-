# 典型回答

dubbo支持多种协议，主要由以下几个：

- 1、dubbo 协议 (默认)
   - 默认就是走dubbo协议的，基于hessian作为序列化协议，单一长连接，TCP协议传输，NIO异步通信，适合大并发小数据量的服务调用，以及消费者远大于提供者，传输数据量很小（每次请求在100kb以内），但是并发量很高。

- 2、rmi 协议
   - 采用JDK标准的rmi协议实现，传输参数和返回参数对象需要实现Serializable接口，使用java标准序列化机制，使用阻塞式短连接，传输数据包大小混合，消费者和提供者个数差不多，可传文件，传输协议TCP。

- 3、hessian 协议
   - 集成Hessian服务，基于HTTP通讯，采用Servlet暴露服务，Dubbo内嵌Jetty作为服务器时默认实现，提供与Hession服务互操作。
   - hessian序列化协议，多个短连接，同步HTTP传输，传入参数较大，提供者大于消费者，提供者压力较大，适用于文件的传输，一般较少用；

- 4、http 协议
   - 基于Http表单提交的远程调用协议，使用Spring的HttpInvoke实现。

- 5、webservice 协议
   - 基于WebService的远程调用协议，集成CXF实现，提供和原生WebService的互操作。

- 6、thrift 协议
   - 当前 dubbo 支持的 thrift 协议是对 thrift 原生协议 的扩展，在原生协议的基础上添加了一些额外的头信息，比如 service name，magic number 等。

- 7、memcached 协议
   - 基于 memcached实现的 RPC 协议。

- 8、redis 协议
   - 基于 Redis实现的 RPC 协议。

- 9、restful
   - 基于标准的Java REST API——JAX-RS 2.0（Java API for RESTful Web Services的简写）实现的REST调用支持

