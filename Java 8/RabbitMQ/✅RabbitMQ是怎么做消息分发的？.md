# 典型回答

rabbitMQ一共有6种工作模式（消息分发方式）分别是简单模式、工作队列模式、发布订阅模式、路由模式、主题模式以及RPC模式。

简单模式是最基本的工作模式，也是最简单的消息传递模式。在简单模式中，一个生产者将消息发送到一个队列中，一个消费者从队列中获取并处理消息。这种模式适用于单个生产者和单个消费者的简单场景，消息的处理是同步的。

![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1690624284380-66eada88-4d89-4ed9-93cd-627e3f93f56e.png#averageHue=%23fcfbf8&clientId=uf0bf3a53-2bf1-4&from=paste&height=280&id=ua652a2d7&originHeight=280&originWidth=1287&originalType=binary&ratio=1&rotation=0&showTitle=false&size=92574&status=done&style=none&taskId=u7e7cad9a-a548-4cba-8f78-830d7fff7c3&title=&width=1287)

工作队列模式用于实现一个任务在多个消费者之间的并发处理。在工作队列模式中，一个生产者将消息发送到一个队列中，多个消费者从队列中获取并处理消息。每个消息只能被一个消费者处理。这种模式适用于多个消费者并发处理消息的情况，提高了系统的处理能力和吞吐量。

![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1690624355141-ff0dc06e-2a75-4962-9c5c-35ba680eeaea.png#averageHue=%23fcfbf6&clientId=uf0bf3a53-2bf1-4&from=paste&height=404&id=uf614af8a&originHeight=404&originWidth=1344&originalType=binary&ratio=1&rotation=0&showTitle=false&size=103839&status=done&style=none&taskId=u94189b44-9bb0-4047-99b9-ab03d103e54&title=&width=1344)

发布/订阅模式用于实现一条消息被多个消费者同时接收和处理。在发布/订阅模式中，一个生产者将消息发送到交换器（Exchange）中，交换器将消息广播到所有绑定的队列，每个队列对应一个消费者。这种模式适用于消息需要被多个消费者同时接收和处理的广播场景，如日志订阅和事件通知等。

![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1691761653923-73c27264-2080-4a26-a3c8-513971f6b941.png#averageHue=%23fbf9f3&clientId=u2b75201e-89b0-4&from=paste&height=254&id=u1e7ac692&originHeight=381&originWidth=1642&originalType=binary&ratio=1.5&rotation=0&showTitle=false&size=193882&status=done&style=none&taskId=u2d56af19-e453-445d-b355-9c6a043b35c&title=&width=1094.6666666666667)

路由模式用于实现根据消息的路由键（Routing Key）将消息路由到不同的队列中。在路由模式中，一个生产者将消息发送到交换器中，并指定消息的路由键，交换器根据路由键将消息路由到与之匹配的队列中。这种模式适用于根据不同的条件将消息发送到不同的队列中，以实现消息的筛选和分发。<br />![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1690624635994-00378b54-2577-4dc5-b1de-f634f4a0a0fd.png#averageHue=%23fbf9f3&clientId=uf0bf3a53-2bf1-4&from=paste&height=407&id=u5da7bbe4&originHeight=407&originWidth=1600&originalType=binary&ratio=1&rotation=0&showTitle=false&size=197851&status=done&style=none&taskId=u7a90a09b-ed7e-40a4-bc02-3ce9777b90d&title=&width=1600)


主题模式是一种更灵活的消息路由模式，它使用通配符匹配路由键，将消息路由到多个队列中。在主题模式中，一个生产者将消息发送到交换器中，并指定主题（Topic）作为路由键，交换器根据通配符匹配将消息路由到与之匹配的队列中。这种模式适用于消息的复杂路由需求，可以实现高度灵活的消息筛选和分发。

![image.png](https://cdn.nlark.com/yuque/0/2023/png/5378072/1690624641506-e406d7f9-3fba-43b5-ad66-10e23f05a8ae.png#averageHue=%23fbf8f2&clientId=uf0bf3a53-2bf1-4&from=paste&height=359&id=u12756636&originHeight=359&originWidth=1682&originalType=binary&ratio=1&rotation=0&showTitle=false&size=195096&status=done&style=none&taskId=ub491f7a6-8321-430a-af8e-a5f7378f0f7&title=&width=1682)

RPC模式是一种用于实现分布式系统中远程调用的工作模式。指的是通过rabbitMQ来实现一种RPC的能力。


这几种模式，根据不同的场景可以用不同的模式，每种模式的发送方及接收方的代码都不太一样，有的简单，有的复杂， 具体实现可以参考：[https://www.rabbitmq.com/getstarted.html](https://www.rabbitmq.com/getstarted.html)
