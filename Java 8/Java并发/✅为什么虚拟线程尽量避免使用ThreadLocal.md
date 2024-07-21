# 典型回答
在使用虚拟线程的时候，不建议使用ThreadLocal，这是JDK官网文档中提到的。

![image.png](https://cdn.nlark.com/yuque/0/2024/png/5378072/1710568672755-885bc7ca-fb04-48b8-ae8d-9be7a50ad4bd.png#averageHue=%23d7b584&clientId=u76e63cf9-459f-4&from=paste&height=285&id=u5106e26e&originHeight=285&originWidth=798&originalType=binary&ratio=1&rotation=0&showTitle=false&size=68598&status=done&style=none&taskId=ub319076a-16d4-49b1-855f-15453f137f6&title=&width=798)

虚拟线程是支持ThreadLocal的，但由于可能创建的虚拟线程数量巨大，不当使用ThreadLocal可能导致内存泄漏等问题。如果需要，可以考虑使用作用域局部变量（Scope-local variables）作为替代方案。

[Scoped Values](https://openjdk.org/jeps/429)，是JEP429中带来的一个特性，在JDK 21中已经推出了预览版。是Java中一种提供在特定执行范围内共享变量的机制，这种机制旨在为虚拟线程和结构化并发提供更好的支持。**与ThreadLocal变量不同，作用域局部变量不是绑定到线程上的，而是绑定到特定的执行范围或上下文。**这使得在使用虚拟线程时，管理跨任务共享的状态变得更加方便和安全。

与 Thread Local 不同的是，Scoped Value的值是不可变的。他的用法和ThreadLocal比较像：

```javascript
class Server {
    final static ScopedValue<Principal> PRINCIPAL =  ScopedValue.newInstance(); 

    void serve(Request request, Response response) {
        var level     = (request.isAdmin() ? ADMIN : GUEST);
        var principal = new Principal(level);
        ScopedValue.where(PRINCIPAL, principal)                           
                   .run(() -> Application.handle(request, response));
    }
}

class DBAccess {
    DBConnection open() {
        var principal = Server.PRINCIPAL.get();                            
        if (!principal.canOpen()) throw new  InvalidPrincipalException();
        return newConnection(...);
    }
}
```

因为这个特性目前还是预览版，并没有正式推出（截止24.3），所以暂时不做过多介绍了，后续推出正式版了，特性语法确定了再展开吧。


