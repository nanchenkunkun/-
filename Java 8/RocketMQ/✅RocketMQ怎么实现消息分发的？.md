# 典型回答

RocketMQ 支持两种消息模式：**广播消费（ Broadcasting ）**和**集群消费（ Clustering ）**。

**广播消费**：当使用广播消费模式时，RocketMQ 会将每条消息推送给集群内所有的消费者，保证消息至少被每个消费者消费一次。

![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1693629100005-d004fc86-c2da-4100-8a47-218cbec2b8be.png#averageHue=%23543512&clientId=u43f4e24e-61ac-4&from=paste&id=ufffe55a6&originHeight=428&originWidth=1294&originalType=url&ratio=1&rotation=0&showTitle=false&size=49782&status=done&style=none&taskId=u69fb127b-801f-46c9-8441-14eb0956b4c&title=)

广播模式下，RocketMQ 保证消息至少被客户端消费一次，但是并不会重投消费失败的消息，因此业务方需要关注消费失败的情况。并且，客户端每一次重启都会从最新消息消费。客户端在被停止期间发送至服务端的消息将会被自动跳过。

**集群消费**：当使用集群消费模式时，RocketMQ 认为任意一条消息只需要被集群内的任意一个消费者处理即可。

![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1693629162403-f7934694-18bb-41e4-9871-26b95c76b8d6.png#averageHue=%23fde7cd&clientId=u43f4e24e-61ac-4&from=paste&id=u87dfe270&originHeight=487&originWidth=1386&originalType=url&ratio=1&rotation=0&showTitle=false&size=44238&status=done&style=none&taskId=ua1b7682c-d362-4838-8e7f-bf6dcfbf876&title=)

集群模式下，每一条消息都只会被分发到一台机器上处理。但是不保证每一次失败重投的消息路由到同一台机器上。一般来说，用集群消费的更多一些。

通过设置MessageModel可以调整消费方式：

```
// MessageModel设置为CLUSTERING（不设置的情况下，默认为集群订阅方式）。
properties.put(PropertyKeyConst.MessageModel, PropertyValueConst.CLUSTERING);


// MessageModel设置为BROADCASTING。
properties.put(PropertyKeyConst.MessageModel, PropertyValueConst.BROADCASTING); 
```
