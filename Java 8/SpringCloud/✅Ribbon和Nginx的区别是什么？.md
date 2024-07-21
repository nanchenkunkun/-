# 典型回答

当我们在在对比Ribbon和Nginx的时候，主要对比的是他们的负载均衡方面的区别。

**这两者最主要的区别是Nginx是一种服务端负载均衡的解决方案，而Ribbon是一种客户端负载均衡的解决方案。**

服务端负载均衡指的是将负载均衡的逻辑集成到服务提供端，通过在服务端对请求进行转发，实现负载均衡。

![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1681550255675-ae1143cb-93b0-415c-98d5-e41c1577165e.png#averageHue=%23fbe6d9&clientId=ud2e5fdab-47c7-4&from=paste&height=689&id=u3d6afec7&originHeight=689&originWidth=1421&originalType=binary&ratio=1&rotation=0&showTitle=false&size=494856&status=done&style=none&taskId=ua4ec64a5-6a15-4e3c-a11c-38f7a2dc47a&title=&width=1421)

客户端负载均衡指的是将负载均衡的逻辑集成到服务消费端的代码中，在客户端直接选择需要调用的服务提供端，并发起请求。这样的好处是可以在客户端直接实现负载均衡、容错等功能，不需要依赖其他组件，使得客户端具有更高的灵活性和可控性。

![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1681550362054-7aa652d7-afdb-4e58-a6de-c5862dc39387.png#averageHue=%23fae5e4&clientId=ud2e5fdab-47c7-4&from=paste&height=603&id=ua35e679d&originHeight=603&originWidth=1078&originalType=binary&ratio=1&rotation=0&showTitle=false&size=57660&status=done&style=none&taskId=u0f304506-89be-4fb1-9456-d33bf86f990&title=&width=1078)

Nginx是需要单独部署一个Nginx服务的，这样他才能做好服务端负载均衡，而Ribbon是需要在服务消费端的机器代码中引入，和应用部署在一起，这样他才能实现客户端的负载均衡。

 
