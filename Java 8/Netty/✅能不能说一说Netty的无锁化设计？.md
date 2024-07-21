# 典型问题

在解决多线程的问题的时候，锁是最常用的方案，但是也是开销最大的一种方案，同时也会带来死锁的问题，所以，Netty为了避免这些问题引入了无锁化设计。

那么，不用锁的话，怎么解决并发的问题呢，Netty主要做了以下几个事情：

首先，Netty基于**Reactor线程模式**实现并发请求处理，避免了线程阻塞与锁的竞争。

[✅Netty的线程模型是怎么样的？](https://www.yuque.com/hollis666/fo22bm/ind4ry?view=doc_embed)

其次，Netty实现了**对象池**，用来减少对象的创建和销毁，从而也能避免了锁的竞争。

[✅说说 Netty 的对象池技术？](https://www.yuque.com/hollis666/fo22bm/rt3r0dfeee6tkuh6?view=doc_embed)

而且在Netty中，还有许多组件都被设计为线程安全的，**例如，每个Channel都有一个唯一的EventLoop**，用于处理所有事件。这样可以避免锁竞争和线程切换带来的开销。
