#  典型回答

Dubbo是支持异步进行调用的，有多种方式，大的方面分为Provider异步调用和Consumer异步调用。

### Consumer异步调用
Consumer的异步调用比较容易理解，就是在调用方的地方自己做一个异步的处理。比如使用CompletableFuture来实现。

这种调用中，服务提供者提供的还是一个同步的同步接口，只不过调用方在调用的时候不需要同步等待结果，可以先去做其他事情，在需要用这个结果的时候再获取即可：

```javascript
@DubboReference
private AsyncService asyncService;

@Override
public void run(String... args) throws Exception {
    //consumer异步调用
    CompletableFuture<String> future3 =  CompletableFuture.supplyAsync(() -> {
        return asyncService.invoke("invoke call request");
    });
    future3.whenComplete((v, t) -> {
        if (t != null) {
            t.printStackTrace();
        } else {
            System.out.println("AsyncTask Response: " + v);
        }
    });

    System.out.println("AsyncTask Executed before response return.");
}
```

### Provider异步调用
还有一种方式就是Provider的异步调用，也就是说本身提供的就是一个异步接口。如：

```javascript
@DubboService
public class AsyncServiceImpl implements AsyncService {

    @Override
    public CompletableFuture<String> asyncInvoke(String param) {
        // 建议为supplyAsync提供自定义线程池
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Do something
                long time = ThreadLocalRandom.current().nextLong(1000);
                Thread.sleep(time);
                StringBuilder s = new StringBuilder();
                s.append("AsyncService asyncInvoke param:").append(param).append(",sleep:").append(time);
                return s.toString();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return null;
        });
    }
}
```

asyncInvoke方法的返回值就是一个CompletableFuture，调用者在调用这个方法时拿到的也是一个Future，在需要结果的时候调用Future的whenComplete方法即可：

```javascript
@DubboReference
private AsyncService asyncService;

@Override
public void run(String... args) throws Exception {
    //调用异步接口
    CompletableFuture<String> future1 = asyncService.asyncInvoke("async call request");
    future1.whenComplete((v, t) -> {
        if (t != null) {
            t.printStackTrace();
        } else {
            System.out.println("AsyncTask Response: " + v);
        }
    });
}
```

> Dubbo 2.6.x及之前版本中是使用的Future进行异步调用的，在java 8中引入的CompletableFuture之后，Dubbo 2.7开始使用CompletableFuture。


除了用CompletableFuture之外，Dubbo还提供了一个类似 Servlet 3.0 的异步接口AsyncContext也能用来实现异步调用。

```javascript
public class AsyncServiceImpl implements AsyncService {
    public String sayHello(String name) {
        final AsyncContext asyncContext = RpcContext.startAsync();
        new Thread(() -> {
            // 如果要使用上下文，则必须要放在第一句执行
            asyncContext.signalContextSwitch();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // 写回响应
            asyncContext.write("Hello " + name + ", response from provider.");
        }).start();
        return null;
    }
}
```

这里用了 RpcContext.startAsync()，可以把一个同步接口转为异步调用。
