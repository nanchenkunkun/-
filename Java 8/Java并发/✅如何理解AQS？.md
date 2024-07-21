# 典型回答

AbstractQueuedSynchronizer （抽象队列同步器，以下简称 AQS）出现在 JDK 1.5 中。AQS 是很多同步器的基础框架，比如 ReentrantLock、CountDownLatch 和 Semaphore 等都是基于 AQS 实现的。除此之外，我们还可以基于 AQS，定制出我们所需要的同步器。

```latex
public abstract class AbstractQueuedSynchronizer
    extends AbstractOwnableSynchronizer
    implements java.io.Serializable {
}
```

![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1696139602112-c0b9e1dc-1e54-43b7-a5f8-bda51ba28b68.png#averageHue=%23f6f6f6&clientId=ud08479a3-f12c-4&from=paste&height=338&id=kXwqw&originHeight=507&originWidth=785&originalType=binary&ratio=1.5&rotation=0&showTitle=false&size=33342&status=done&style=none&taskId=u44a0a4fd-a5c8-48a1-8f9d-d37749b0b37&title=&width=523.3333333333334)

**在AQS内部，维护了一个FIFO队列和一个volatile的int类型的state变量。在state=1的时候表示当前对象锁已经被占有了，state变量的值修改的动作通过CAS来完成。**

[✅有了CAS为啥还需要volatile？](https://www.yuque.com/hollis666/fo22bm/brargpgpdizkgkog?view=doc_embed)

**FIFO队列用来实现多线程的排队工作，当线程加锁失败时，该线程会被封装成一个Node节点来置于队列尾部。**

![image.png](https://cdn.nlark.com/yuque/0/2024/png/5378072/1704529191910-44e2d93f-794a-455e-be4f-0943a7e49f3f.png#averageHue=%23fafaf9&clientId=u75681587-75e5-4&from=paste&height=645&id=u481fb251&originHeight=645&originWidth=1402&originalType=binary&ratio=1&rotation=0&showTitle=false&size=40385&status=done&style=none&taskId=uc4f0bc0a-ca75-4d78-82a7-3871efd4435&title=&width=1402)<br />**当持有锁的线程释放锁时，AQS会将等待队列中的第一个线程唤醒，并让其重新尝试获取锁。**<br />![image.png](https://cdn.nlark.com/yuque/0/2024/png/5378072/1704529305749-dc2c1532-0a87-46b5-9aa6-d21522be542e.png#averageHue=%23f9f8f7&clientId=u75681587-75e5-4&from=paste&height=695&id=uc00c3887&originHeight=695&originWidth=1084&originalType=binary&ratio=1&rotation=0&showTitle=false&size=51167&status=done&style=none&taskId=u57e346ce-d5ed-4fa6-bbcf-f431a4a899d&title=&width=1084)
> 上图展示的是一个非公平锁，如果是公平锁则第一步只进行判断队列中是否有前序节点，如果有的话，直接入队列，不会进行第一次的CAS。

<br />
#### 同步状态——state

AQS使用一个volatile int类型的成员变量来表示同步状态，在state=1的时候表示当前对象锁已经被占有了。它提供了三个基本方法来操作同步状态：getState(), setState(int newState), 和 compareAndSetState(int expect, int update)。这些方法允许在不同的同步实现中自定义资源的共享和独占方式。

```latex
// 同步状态
private volatile int state;

// 获取状态
protected final int getState() {
    return state;
}

// 设置状态
protected final void setState(int newState) {
    state = newState;
}

// CAS更新状态
protected final boolean compareAndSetState(int expect, int update) {
    // See below for intrinsics setup to support this
    return unsafe.compareAndSwapInt(this, stateOffset, expect, update);
}
```

#### FIFO队列——Node

AQS内部通过一个内部类——Node，AQS就是借助他来实现同步队列的功能的。

![image.png](https://cdn.nlark.com/yuque/0/2024/png/5378072/1704527213375-e9cfffe7-0819-4de8-bd05-1f80449a6876.png#averageHue=%23565656&clientId=u75681587-75e5-4&from=paste&height=680&id=ulUr5&originHeight=844&originWidth=1400&originalType=url&ratio=1&rotation=0&showTitle=false&size=97654&status=done&style=none&taskId=u3f664dbf-ecdf-423b-8538-102d95c241b&title=&width=1128)

当线程尝试获取资源失败时，AQS 会将该线程包装成一个 Node 节点，并将其插入同步队列的尾部。在资源可用时，队列头部的节点会尝试再次获取资源。（在 AQS 中，Node 也用于构建条件队列。当线程需要等待某个条件时，它会被加入到条件队列中。当条件满足时，线程会被转移回同步队列。）

```latex
// Node类用于构建队列
static final class Node {
    // 标记节点状态。常见状态有 CANCELLED（表示线程取消）、SIGNAL（表示后继节点需要运行）、CONDITION（表示节点在条件队列中）等。
    volatile int waitStatus;
    // 前驱节点
    volatile Node prev;
    // 后继节点
    volatile Node next;
    // 节点中的线程，存储线程引用，指向当前节点所代表的线程。
    volatile Thread thread;
}

// 队列头节点，延迟初始化。只在setHead时修改
private transient volatile Node head;
// 队列尾节点，延迟初始化。
private transient volatile Node tail;

// 入队操作
private Node enq(final Node node) {
    for (;;) {
        Node t = tail;
        if (t == null) { // 必须先初始化
            if (compareAndSetHead(new Node()))
                tail = head;
        } else {
            node.prev = t;
            if (compareAndSetTail(t, node)) {
                t.next = node;
                return t;
            }
        }
    }
}
```

就这样，一个又一个Node被连接在一起，就成为了一个FIFO的队列。

![](https://cdn.nlark.com/yuque/0/2024/png/5378072/1704527607626-fcffec73-dfa7-47f3-bf82-99b16ef0688d.png#averageHue=%23f3f3f3&clientId=u75681587-75e5-4&from=paste&id=OOWIw&originHeight=440&originWidth=1408&originalType=url&ratio=1&rotation=0&showTitle=false&status=done&style=none&taskId=u3a6a46e7-32d8-4568-9252-35d725979ed&title=)

> AQS中的阻塞队列是一个CLH队列。CLH（Craig, Landin, and Hagersten）队列是一种用于实现自旋锁的有效数据结构。它是由Craig, Landin和Hagersten首次提出的，因此得名。


# 扩展知识
## AQS有哪些实现？

![image.png](https://cdn.nlark.com/yuque/0/2022/png/5378072/1671360897218-54f84a55-c6ca-4276-96d5-78863803a6be.png#averageHue=%23e9f0e4&clientId=u7a0c9d63-0d42-4&from=paste&id=u89052826&originHeight=538&originWidth=667&originalType=url&ratio=1&rotation=0&showTitle=false&size=205570&status=done&style=none&taskId=u5b9be69b-8fd7-4cb2-a9c5-2ab723b267e&title=)

AQS全称为AbstractQueuedSynchronizer，它提供了一个FIFO队列，可以看成是一个用来实现同步锁以及其他涉及到同步功能的核心组件，常见的有：ReentrantLock、CountDownLatch、Semaphore等。

从本质上来说，AQS提供了两种锁机制，分别是排它锁和共享锁。

排它锁就是存在多线程竞争同一共享资源时，同一时刻只允许一个线程访问该共享资源，也就是多个线程中只能有一个线程获得锁资源，比如Lock中的ReentrantLock重入锁实现就是用到了AQS中的排它锁功能。

共享锁也称为读锁，就是在同一时刻允许多个线程同时获得锁资源，比如CountDownLatch和Semaphore都是用到了AQS中的共享锁功能。

### **CountDownLatch、CyclicBarrier、Semaphore**

[✅CountDownLatch、CyclicBarrier、Semaphore区别？](https://www.yuque.com/hollis666/fo22bm/bkx0d6?view=doc_embed)
### ReentrantLock

[✅synchronized和reentrantLock区别？](https://www.yuque.com/hollis666/fo22bm/bitupp?view=doc_embed)

## 同步队列&条件队列

AQS 总共有两种队列，一种是**同步队列**，用于实现锁的获取和释放。还有一种是**条件队列**，条件队列也是一个FIFO队列，用于在特定条件下管理线程的等待和唤醒。

[✅AQS的同步队列和条件队列原理？](https://www.yuque.com/hollis666/fo22bm/xc3fs6mny7pgeh0p?view=doc_embed)
## 独占模式&共享模式

AQS提供了两种模式来支持不同类型的同步器：独占模式和共享模式。

[✅什么是AQS的独占模式和共享模式？](https://www.yuque.com/hollis666/fo22bm/wk1gxv6xgqk0folv?view=doc_embed)
