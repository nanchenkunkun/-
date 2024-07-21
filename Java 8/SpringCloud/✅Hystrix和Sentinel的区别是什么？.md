# 典型回答

Hystrix和Sentinel都是SpringCloud中可以用来做限流、降级的组件。

Hystrix 的关注点在于以 隔离 和 熔断 为主的容错机制，超时或被熔断的调用将会快速失败，并可以提供 fallback 机制。而 Sentinel 的侧重点在于多样化的流量控制、熔断降级、系统负载保护以及实时监控和控制台。

关于Hystrix和Sentinel的对比，在Sentinel的官网上有一篇文章写的挺详细的： [https://sentinelguard.io/zh-cn/blog/sentinel-vs-hystrix.html](https://sentinelguard.io/zh-cn/blog/sentinel-vs-hystrix.html) 

二者的主要差异如下表：

![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1683888160298-f5d23fd4-4b47-43a8-a6ad-911bff7b1d4e.png#averageHue=%23f1f2f3&clientId=u7556d512-886b-4&from=paste&height=1116&id=u5e750409&originHeight=1116&originWidth=1542&originalType=binary&ratio=1&rotation=0&showTitle=false&size=207613&status=done&style=none&taskId=ubbb14af3-a33e-43b4-89a8-a0755aa1998&title=&width=1542)
