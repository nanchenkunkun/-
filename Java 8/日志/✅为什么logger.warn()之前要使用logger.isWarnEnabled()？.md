# 典型回答

很多人在读源码的时候，经常可以看到以下代码：

Spring中：<br />![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1699169900674-ea85e8aa-ae76-4fda-b516-ff6350b1be64.png#averageHue=%23fefefd&clientId=uaada719a-9cb9-4&from=paste&height=740&id=u74e4d4f1&originHeight=740&originWidth=2055&originalType=binary&ratio=1&rotation=0&showTitle=false&size=108242&status=done&style=none&taskId=u297925d1-9e97-4b53-9707-d5f40520aa1&title=&width=2055)

Dubbo中：<br />![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1699169943116-7eb05894-f704-4b33-ac40-f5973844595a.png#averageHue=%23fefefd&clientId=uaada719a-9cb9-4&from=paste&height=694&id=u42ce52f7&originHeight=694&originWidth=2056&originalType=binary&ratio=1&rotation=0&showTitle=false&size=106998&status=done&style=none&taskId=ubfe25b89-8778-44bc-9039-da567324420&title=&width=2056)

也就是说，很多框架，在执行warn()、debug()等方法前，都会额外的调用一下isWarnEnabled()和isDebugEnabled()等方法，这是为什么呢？

isWarnEnabled、isDebugEnabled等方法，其实是判断当前日志级别是否开启的，如果开启则返回true，否则返回false。

其实，之所以要提前调用一次isWarnEnabled，主要是为了提升性能的，因为在记录日志时，生成日志消息的过程可能会涉及方法的执行、字符串的拼接、对象的序列化等操作。而这些操作都是比较耗费时间的。

如果一段日志，不需要输出，那么这些步骤其实是可以省略的。

比如我们有以下日志要输出：

```java
public void login(LoginRequest loginRequest){
    if (logger.isWarnEnabled()) {
        logger.warn("This is a message with: " + JSON.toJSONString(loginRequest));
    }
}
```

通过`logger.isWarnEnabled`做一次前置判断，那么就可以在warn级别不生效时，能避免 `JSON.toJSONString(loginRequest)`方法的执行，也能避免`"This is a message with: " + JSON.toJSONString(loginRequest)`这个字符串拼接操作的执行。

所以，使用`logger.isXxxEnabled()`用于检查日志Xxx级别是否启用，可以在需要时避免不必要的开销，提高应用程序的性能。这种做法特别在记录频繁的日志消息时，尤为重要。
