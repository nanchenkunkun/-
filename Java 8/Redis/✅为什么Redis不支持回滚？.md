# 典型回答

我们都知道，Redis是不支持回滚的，即使是Redis的事务和Lua脚本，在执行的过程中，如果出现了错误，也是无法回滚的，可是，为什么呢？

在Redis的官网文档中明确的提到过，不支持回滚：[https://redis.io/docs/interact/transactions/](https://redis.io/docs/interact/transactions/)<br />![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1703999289243-9f43239e-2bd3-44c6-a5a2-590209d64ef8.png#averageHue=%23fefefe&clientId=u93acf546-2e6d-4&from=paste&height=239&id=uef52f915&originHeight=239&originWidth=1048&originalType=binary&ratio=1&rotation=0&showTitle=false&size=33181&status=done&style=none&taskId=u0d072ab8-89fb-4118-80e8-abab70131ea&title=&width=1048)<br />不支持回滚主要的原因是**支持回滚将对 Redis 的简洁性和性能产生重大影响。**

然后，Redis官方博客中还有一篇文章，[You Don’t Need Transaction Rollbacks in Redis](https://redis.com/blog/you-dont-need-transaction-rollbacks-in-redis/) 这里介绍了更多的内容。

我这里试着帮大家总结一下，主要有以下几个原因：

1、**使用场景**：Redis 通常用作缓存和，而不是作为需要复杂事务处理的关系型数据库。因此，它的目标用户通常不需要复杂的事务支持。如果需要的话，直接用数据库就行了。<br />**2、性能优先**：Redis 是一个高性能的K-V存储系统，它优化了速度和效率。引入回滚机制会增加复杂性和开销，从而影响其性能。<br />3、**简化设计**：Redis 的设计哲学倾向于简单和高效。回滚机制会使系统变得复杂，增加了错误处理和状态管理的难度。<br />4、**数据类型和操作**：Redis 支持的数据类型和操作通常不需要复杂的事务支持。大多数命令都是原子性的。<br />5、**单线程模型**：Redis事务是提交后一次性在单线程中执行的，而关系型数据库如MySQL是交互式的多线程模型执行的，所以MySQL需要事务的回滚来确保并发更新结果不出现异常。而Redis不太需要。<br />6、**出错情况**：在Redis中，命令失败的原因比较有限：语法错误、操作的数据的类型不一致、Redis资源不足等。而这几种问题，是应该在编码阶段就发现的，而不应该在Redis执行过程中出现。


**总结一下，以为Redis的设计就是简单、高效等，所以引入事务的回滚机制会让系统更加的复杂，并且影响性能。从使用场景上来说，Redis一般都是被用作缓存的，不太需要很复杂的事务支持，当人们需要复杂的事务时会考虑持久化的关系型数据库。相比于关系型数据库，Redis是通过单线程执行的，在执行过程中，出现错误的概率比较低，并且这些问题一般来编译阶段都应该被发现，所以就不太需要引入回滚机制。**

# 扩展知识

## 不支持回滚如何保证原子性？

[✅并发编程中的原子性和数据库ACID的原子性一样吗？](https://www.yuque.com/hollis666/fo22bm/wsfbu382gg5l9ytx?view=doc_embed)

[✅为什么Lua脚本可以保证原子性？](https://www.yuque.com/hollis666/fo22bm/rwdgnu?view=doc_embed)
