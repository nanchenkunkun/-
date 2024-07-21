### 背景
<br />我的项目中，有这样一个场景，我们要发起一个用户主动还款之后，需要把在途的扣款单全部暂停，但是因为用户选择还款单据可能比较多，而且暂停是调用外部资金打款系统的接口实现的，这个地方如果一条一条执行就很慢，但是如果起多线程执行的话，我就没办法知道他们每一个暂定的返回结果。

我想可以实现一个这样的功能：多线程去执行暂停动作，如果都成功了，那么就推进我的主动扣款后续流程，如果暂停有任何一个接口调用失败了，那么先不推进后续流程，等下次重试。
### <br />技术选型

为了实现这个功能，我了解到CompletableFuture，他是Java 8中引入的一个新特性，它提供了一种简单的方法来实现异步编程和任务组合。

他的多线程编排的能力刚好可以用在我这个场景中，而且他底层是基于ForkJoinPool实现的，所以他的性能也比较高效，所以最终我选择了这个方案。

### 具体实现

```java
//异步暂停扣款
CompletableFuture<Void> allFutures = CompletableFuture.allOf(noticeDetails.stream()
        .map(detail -> CompletableFuture.supplyAsync(() -> {
            pause(detail);
            return null;
        })).toArray(CompletableFuture[]::new));

//所有暂停扣款成功后，更新代还通知单
allFutures.whenComplete((v, e) -> {
    if (e == null) {
        //执行后续的还款操作
        //...
        //...
    } else {
        log.error("notice failed", e);
    }
});
```

### 得到结果

在用了CompletableFuture做编排之前，原来50笔订单的暂停扣款，需要大概10s左右，但是用了CompletableFuture之后，50笔订单的暂停扣款只需要1秒钟左右。

### 学习资料

[✅CompletableFuture的底层是如何实现的？](https://www.yuque.com/hollis666/fo22bm/qgrygdsu04a6vfzw?view=doc_embed)

[✅ForkJoinPool和ExecutorService区别是什么？](https://www.yuque.com/hollis666/fo22bm/wl8s1swvh7g841be?view=doc_embed)

