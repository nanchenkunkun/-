# 扩展知识

在分布式系统中，我们的应用可能是以集群形式对外提供服务的，有可能出现在A服务器登录后，用户下一次访问的时候请求到B服务器，就需要有一个分布式的Sesssion来告诉B服务器用户是登录过的，并且需要拿到用户的登录信息。

在业内，实现分布式Session通常有以下几个方案：

**客户端存储**：用户登录后，将Session信息保存在客户端，用户在每次请求的时候，通过客户端的cookie把session信息带过来。这个方案因为要把session暴露给客户端，**存在安全风险。**

**基于分布式存储（最常用）**：将Session数据保存在分布式存储系统中，如分布式文件系统、分布式数据库等。不同服务器可以共享同一个分布式存储，通过Session ID查找对应的Session数据。唯一的缺点就是**需要依赖第三方存储**，如Redis、数据库等。

**粘性Session**：这个方案指的是把一个用户固定的路由到指定的机器上，这样只需要这台服务器中保存了session即可，不需要做分布式存储。但是这个存在的问题就是**可能存在单点故障的问题**。

**Session复制**：当用户的Session在某个服务器上产生之后，通过复制的机制，将他同步到其他的服务器中。这个方案的缺点是有**可能有延迟**。

Tomcat支持Session复制，配置方式可以参考官方文档：[https://tomcat.apache.org/tomcat-8.0-doc/cluster-howto.html](https://tomcat.apache.org/tomcat-8.0-doc/cluster-howto.html)

Spring中也提供了对Session管理的支持——[Spring Session](https://docs.spring.io/spring-session/reference/index.html)，他集成了很多Session共享的方案，如基于Redis、基于数据库等。




