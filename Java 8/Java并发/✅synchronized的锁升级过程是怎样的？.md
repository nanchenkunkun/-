# 典型回答

在JDK 1.6及之前的版本中，synchronized锁是通过对象内部的一个叫做监视器锁（也称对象锁）来实现的。当一个线程请求对象锁时，如果该对象没有被锁住，线程就会获取锁并继续执行。如果该对象已经被锁住，线程就会进入阻塞状态，直到锁被释放。这种锁的实现方式称为**“重量级锁”**，因为获取锁和释放锁都需要在操作系统层面上进行线程的阻塞和唤醒，而这些操作会带来很大的开销。

在JDK 1.6之后，synchronized锁的实现发生了一些变化，引入了“偏向锁”、“轻量级锁”和“重量级锁”三种不同的状态，用来适应不同场景下的锁竞争情况。

在JDK 15 中，废弃了偏向锁（[https://openjdk.org/jeps/374](https://openjdk.org/jeps/374) ）

所以，在Java中，锁的状态分为四种，分别是无锁状态、偏向锁状态、轻量级锁状态和重量级锁状态。在Java中，mark word的低两位用于表示锁的状态，分别为“01”（无锁状态）、“01”（偏向锁状态）、“00”（轻量级锁状态）和“10”（重量级锁状态）。但是由于无锁状态和偏向锁都是"01"，所以在低三位引入偏向锁标记位，用"0"表示无锁，"1"表示偏向。

![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1683549141839-20956e27-844b-4594-81f5-fc7bc3647829.png#averageHue=%23f4f8e3&clientId=u3e7bc98d-d5ee-4&from=paste&height=316&id=ub2026fdb&originHeight=631&originWidth=1989&originalType=binary&ratio=2&rotation=0&showTitle=false&size=927082&status=done&style=none&taskId=u5ab27799-1d6b-482f-bb77-586ffd0d500&title=&width=994.5)

以下偏向过程基于OpenJDK 8 源码总结的，源码地址：[https://github.com/openjdk/jdk8u/blob/master/hotspot/src/share/vm/runtime/synchronizer.cpp](https://github.com/openjdk/jdk8u/blob/master/hotspot/src/share/vm/runtime/synchronizer.cpp)

具体的锁升级过程如下，这里为了方便大家理解，暂时不包含自旋相关的内容，锁升级过程中的自旋参考：

[synchronized升级过程中有几次自旋？](https://www.yuque.com/hollis666/fo22bm/dc6vfx4nfvptib2y?view=doc_embed)

### 无锁


当一个线程第一次访问一个对象的同步块时，JVM会在对象头中设置该线程的Thread ID，并将对象头的状态位设置为“偏向锁”。这个过程称为“偏向”，表示对象当前偏向于第一个访问它的线程。


### 偏向锁（Biased Locking）


当一个synchronized块被线程首次进入时，锁对象会进入偏向模式。

在偏向锁模式下，锁会偏向于第一个获取它的线程，JVM 会在对象头中记录该线程的 ID 作为偏向锁的持有者，并将对象头中的 Mark Word 中的一部分作为偏向锁标识。

在这种情况下，如果其他线程访问该对象，会先检查该对象的偏向锁标识，如果和自己的线程 ID 相同，则直接获取锁。如果不同，则该对象的锁状态就会升级到轻量级锁状态。

**触发条件**：首次进入synchronized块时自动开启，假设JVM启动参数没有禁用偏向锁。

但是需要注意，在JDK 15中，偏向锁已被废除：

[✅为什么JDK 15要废弃偏向锁？](https://www.yuque.com/hollis666/fo22bm/kzigekbg6ark71m3?view=doc_embed)

### 轻量级锁（Lightweight Locking）

当有另一个线程尝试获取已被偏向的锁时，偏向锁会被撤销，锁会升级为轻量级锁。

在轻量级锁状态中，JVM 为对象头中的 Mark Word 预留了一部分空间，用于存储指向线程栈中锁记录的指针。

当一个线程尝试获取轻量级锁时，JVM的做法是：

1. 将对象头中的Mark Word复制到线程栈中的锁记录（Lock Record）：每个Java对象头部都有一个Mark Word，它用于存储对象自身的运行时数据，如哈希码、锁状态信息、代年龄等。当线程尝试获取轻量级锁时，JVM会在当前线程的栈帧中创建一个锁记录空间，然后将对象头中的Mark Word复制到这个锁记录中。这个复制的Mark Word被称为“Displaced Mark Word”。

2. 尝试通过CAS操作更新对象头的Mark Word：接下来，JVM尝试使用CAS（Compare-And-Swap）操作，将对象头的Mark Word更新为指向锁记录的指针。如果这个更新操作成功，那么这个线程就成功获取了这个对象的轻量级锁。

如果替换成功，则该线程获取锁成功；如果失败，则表示已经有其他线程获取了锁，则该锁状态就会升级到重量级锁状态。

**触发条件**：当有另一个线程尝试获取已被偏向的锁时，偏向锁会升级为轻量级锁。

#### 为什么需要将对象头中的Mark Word复制到线程栈中？

在做CAS之前，需要将对象头中的Mark Word复制到线程栈中的锁记录（Lock Record），之所以这么做的主要原因原因是为了**保留对象的原始信息**，复制Mark Word到线程栈中是为了在锁释放时能够恢复对象头的原始状态。因为锁的获取与释放是成对出现的，所以在释放锁时，JVM需要使用这份复制的原始Mark Word来恢复对象头，确保对象状态的正确性。

### 重量级锁（Heavyweight Locking）


当轻量级锁的CAS操作失败，即出现了实际的竞争，锁会进一步升级为重量级锁。

当锁状态升级到重量级锁状态时，JVM 会将该对象的锁变成一个重量级锁，并在对象头中记录指向等待队列的指针。

此时，如果一个线程想要获取该对象的锁，则需要先进入等待队列，等待该锁被释放。当锁被释放时，JVM 会从等待队列中选择一个线程唤醒，并将该线程的状态设置为“就绪”状态，然后等待该线程重新获取该对象的锁。

**触发条件**：当轻量级锁的CAS操作失败，轻量级锁升级为重量级锁。


# 扩展知识

## synchronized 能降级么？

[✅synchronized 的锁能降级吗？](https://www.yuque.com/hollis666/fo22bm/ghg8a3skmvxgquvh?view=doc_embed)
