# 典型回答

RabbitMQ中是可以实现延迟消息的，一般有两种方式，分别是通过死信队列以及通过延迟消息插件来实现。

# 扩展知识
## 死信队列

当RabbitMQ中的一条正常的消息，因为过了存活时间（TTL过期）、队列长度超限、被消费者拒绝等原因无法被消费时，就会变成Dead Message，即死信。

当一个消息变成死信之后，他就能被重新发送到死信队列中（其实是交换机-exchange）。

那么基于这样的机制，就可以实现延迟消息了。那就是我们给一个消息设定TTL，但是并不消费这个消息，等他过期，过期后就会进入到死信队列，然后我们再监听死信队列的消息消费就行了。

而且，RabbitMQ中的这个TTL是可以设置任意时长的，这相比于RocketMQ只支持一些固定的时长而显得更加灵活一些。

但是，死信队列的实现方式存在一个问题，那就是可能造成队头阻塞。RabbitMQ会定期扫描队列的头部，检查队首的消息是否过期。如果队首消息过期了，它会被放到死信队列中。然而，RabbitMQ不会逐个检查队列中的所有消息是否过期，而是仅检查队首消息。这样，如果队列的队头消息未过期，而它后面的消息已过期，这些后续消息将无法被单独移除，直到队头的消息被消费或过期。

因为队列是先进先出的，在普通队列中的消息，每次只会判断队头的消息是否过期，那么，如果队头的消息时间很长，一直都不过期，那么就会阻塞整个队列，这时候即使排在他后面的消息过期了，那么也会被一直阻塞。

基于RabbitMQ的死信队列，可以实现延迟消息，非常灵活的实现定时关单，并且借助RabbitMQ的集群扩展性，可以实现高可用，以及处理大并发量。他的缺点第一是可能存在消息阻塞的问题，还有就是方案比较复杂，不仅要依赖RabbitMQ，而且还需要声明很多队列出来，增加系统的复杂度
## 
## RabbitMQ插件
其实，基于RabbitMQ的话，可以不用死信队列也能实现延迟消息，那就是基于rabbitmq_delayed_message_exchange插件，这种方案能够解决通过死信队列实现延迟消息出现的消息阻塞问题。但是该插件从RabbitMQ的3.6.12开始支持的，所以对版本有要求。

![](https://cdn.nlark.com/yuque/0/2022/jpeg/5378072/1671869294254-32000976-4398-458d-b2fa-a5f3ce3e2119.jpeg#averageHue=%23e4efe3&clientId=u376f9cb1-fc82-4&from=paste&id=u82ac1481&originHeight=702&originWidth=2560&originalType=url&ratio=1&rotation=0&showTitle=false&status=done&style=none&taskId=ub65b3347-0bb2-4b46-80cd-64c315d3370&title=)<br />这个插件是官方出的，可以放心使用，安装并启用这个插件之后，就可以创建x-delayed-message类型的交换机了。

前面我们提到的基于死信队列的方式，是消息先会投递到一个正常队列，在TTL过期后进入死信队列。但是基于插件的这种方式，消息并不会立即进入队列，而是先把他们保存在一个基于Erlang开发的Mnesia数据库中，然后通过一个定时器去查询需要被投递的消息，再把他们投递到x-delayed-message交换机中。

基于RabbitMQ插件的方式可以实现延迟消息，并且不存在消息阻塞的问题，但是因为是基于插件的，而这个插件支持的最大延长时间是(2^32)-1 毫秒，大约49天，超过这个时间就会被立即消费。

不过这个方案也有一定的限制，它将延迟消息存在于 Mnesia 表中，并且在当前节点上具有单个磁盘副本，存在丢失的可能。

目前该插件的当前设计并不真正适合包含大量延迟消息（例如数十万或数百万）的场景，详情参见 [#/issues/72](https://link.juejin.cn?target=https%3A%2F%2Fgithub.com%2Frabbitmq%2Frabbitmq-delayed-message-exchange%2Fissues%2F72) 另外该插件的一个可变性来源是依赖于 Erlang 计时器，在系统中使用了一定数量的长时间计时器之后，它们开始争用调度程序资源，并且时间漂移不断累积。（<br />[https://github.com/rabbitmq/rabbitmq-delayed-message-exchange#limitations](https://github.com/rabbitmq/rabbitmq-delayed-message-exchange#limitations) ）

