# 典型回答

[✅如何理解AQS？](https://www.yuque.com/hollis666/fo22bm/qka9yt?view=doc_embed)

同步队列和条件队列是AQS中的两种不同队列，**同步队列主要用于实现锁机制，而条件队列用于线程间的协调和通信。（本文代码为经典的**[**JDK 1.8**](https://github.com/zxiaofan/JDK/blob/master/JDK1.8/src/java/util/concurrent/locks/AbstractQueuedSynchronizer.java#L1830)**）**

### 同步队列

**同步队列主要用于实现锁的获取和释放**。如我们常用的ReentrantLock，就是基于同步队列来实现的。

我们在介绍AQS的时候介绍过，它是一个**FIFO队列**，节点类型为AQS内部的**Node类**。当一个线程尝试获取锁失败时，它会被封装成一个Node节点加入到队列的尾部（每个节点（Node）代表一个等待的线程）。当锁被释放时，头节点的线程会被唤醒，尝试再次获取锁。

```java
static final class Node {
    // 前驱和后继节点，构成双向链表
    Node prev;
    Node next;
    // 线程本身
    Thread thread;
    // 状态信息，表示节点在同步队列中的等待状态
    int waitStatus;
    // ...
}
```

同步的队列的实现原理比较简单：

- 当一个线程尝试获取锁并失败时，AQS会将该线程包装成一个节点（Node）并加入到队列的尾部。

```java
private Node addWaiter(Node mode) {
    Node node = new Node(Thread.currentThread(), mode);
    // 尝试快速路径：直接尝试在尾部插入节点
    Node pred = tail;
    if (pred != null) {
        node.prev = pred;
        if (compareAndSetTail(pred, node)) {
            pred.next = node;
            return node;
        }
    }
    // 快速路径失败时，进入完整的入队操作
    enq(node);
    return node;
}

private Node enq(final Node node) {
    for (;;) {
        Node t = tail;
        if (t == null) { // 队列为空，初始化
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

- 这个节点会处于等待状态，直到锁被其他线程释放。
- 当锁被释放时，头节点（持有锁的线程）会通知其后继节点（如果存在的话），后继节点尝试获取锁。
- 这个过程会一直持续，直到有线程成功获取锁或者队列为空。

### 条件队列

条件队列用于实现条件变量，允许线程在特定条件不满足时挂起，直到其他线程改变了条件并显式唤醒等待在该条件上的线程。比较典型的一个条件队列的使用场景就是ReentrantLock的Condition。

条件队列与同步队列不同，它是基于Condition接口实现的，用于管理那些因为某些条件未满足而等待的线程。当条件满足时，这些线程可以被唤醒。每个Condition对象都有自己的一个条件队列。

![](https://cdn.nlark.com/yuque/0/2024/png/5378072/1704527213375-e9cfffe7-0819-4de8-bd05-1f80449a6876.png#averageHue=%23565656&from=url&height=681&id=dMyop&originHeight=844&originWidth=1400&originalType=binary&ratio=1&rotation=0&showTitle=false&status=done&style=none&title=&width=1130)

ConditionObject是AQS的一个内部类，用于实现条件变量。条件变量是并发编程中一种用于线程间通信的机制，它允许一个或多个线程在特定条件成立之前等待，同时释放相关的锁。这在某种程度上类似于对象监视器模式中的wait()和notify()方法，但提供了更灵活和更强大的控制。

```java
public class ConditionObject implements Condition, java.io.Serializable {
    // 条件队列的首尾节点
    private transient Node firstWaiter;
    private transient Node lastWaiter;
    // ...
}

```

他的主要原理如下：

- 当线程调用了Condition的await()方法后，它会释放当前持有的锁，并且该线程会被加入到条件队列中等待。

> await()：使当前线程释放锁并进入等待队列，直到被另一个线程的signal()或signalAll()方法唤醒，或被中断。


```latex
public final void await() throws InterruptedException {
    // 如果当前线程在进入此方法之前已经被中断了，则直接抛出InterruptedException异常。
    if (Thread.interrupted())
        throw new InterruptedException();
    
    // 将当前线程加入到等待队列中。
    Node node = addConditionWaiter();
    
    // 释放当前线程所持有的锁，并返回释放前的状态，以便以后可以重新获取到相同数量的锁。
    int savedState = fullyRelease(node);
    
    // 中断模式，用于记录线程在等待过程中是否被中断。
    int interruptMode = 0;
    
    // 如果当前节点不在同步队列中，则表示线程应该继续等待。
    while (!isOnSyncQueue(node)) {
        // 阻塞当前线程，直到被唤醒或中断。
        LockSupport.park(this);
        
        // 检查线程在等待过程中是否被中断，并更新interruptMode状态。
        if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
            break;
    }
    
    // 当节点成功加入到同步队列后，尝试以中断模式获取锁。
    // 如果在此过程中线程被中断，且不是在signal之后，则设置中断模式为REINTERRUPT。
    if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
        interruptMode = REINTERRUPT;
    
    // 如果节点后面还有等待的节点，从等待队列中清理掉被取消的节点。
    if (node.nextWaiter != null) // clean up if cancelled
        unlinkCancelledWaiters();
    
    // 根据中断模式处理中断。
    if (interruptMode != 0)
        reportInterruptAfterWait(interruptMode);
}

```

- 然后线程会处于等待状态，直到其他线程调用signal()或signalAll()方法来通知条件可能已经满足。

> signal()：唤醒等待队列中的头节点对应的线程。
> signalAll()：唤醒所有等待的线程。


```latex
public final void signal() {
    if (!isHeldExclusively())
        throw new IllegalMonitorStateException();
    Node first = firstWaiter;
    if (first != null)
        doSignal(first);
}

private void doSignal(Node first) {
    do {
        if ( (firstWaiter = first.nextWaiter) == null)
            lastWaiter = null;
        first.nextWaiter = null;
    } while (!transferForSignal(first) &&
             (first = firstWaiter) != null);
}
```

# 扩展知识

## 二者区别

条件队列和同步队列，主要有以下区别：

- **目的不同**：同步队列主要用于管理锁的获取和释放，而条件队列用于等待特定条件的满足。
- **使用方式不同**：同步队列是AQS自动管理的，开发者通常不需要直接与之交互；而条件队列是通过Condition接口暴露给开发者的，需要显式地调用等待（await）和通知（signal/signalAll）方法。
- **队列类型不同**：虽然它们都是队列结构，但同步队列是所有基于AQS同步器共享的，每个同步器实例只有一个同步队列；条件队列是每个Condition实例特有的，一个同步器可以有多个Condition对象，因此也就有多个条件队列。
