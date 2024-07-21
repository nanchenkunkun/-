# 典型回答
## 三次握手

![](https://cdn.nlark.com/yuque/0/2023/png/5378072/1675138505949-2e21fdde-ec4c-4943-82da-96a85391642c.png#averageHue=%23f8f2ea&clientId=u501f6444-4af5-4&from=paste&id=u1c62a01b&originHeight=563&originWidth=887&originalType=url&ratio=1&rotation=0&showTitle=false&status=done&style=none&taskId=u4d213be1-cf6a-43bf-bfa8-482124fd76c&title=)

1. 你（客户端）给一个朋友（服务器）打电话，告诉他你想开始对话。这就像是发送一个SYN（同步序列编号）信号，表示你想开始建立连接。（client向server发送syn，seq=x，此时client验证client发送能力正常。client置为SYN_SENT状态）
2. 你的朋友接到电话，明白你想开始对话。他回应说“好的，我准备好了”，同时也告诉你他也想说些话。这就相当于服务器发送SYN-ACK（同步和确认）信号，既确认收到了你的请求，也表明它准备好了并想建立连接。（server收到syn，此时server验证client发送能力正常，server接收能力正常。server向client发送ack = x + 1,seq = y，此时server验证server发送能力正常。server置为SYN_RCVD状态）
3. 最后，你回复你的朋友说你收到了他的确认，现在可以开始对话了。这就是发送ACK（确认）信号，确认你已经准备好进行通信。（client收到ack，此时client验证client接收能力正常，server接收发送能力正常。client向server发送ack = y + 1， seq = x + 1，server接收到后验证client接收能力正常。client置为ESTABLISHED状态）
## 四次挥手

![](https://cdn.nlark.com/yuque/0/2023/png/5378072/1675138535863-b4e954fa-ddff-4d1c-b1eb-195c98906c86.png#averageHue=%23f8f3ea&clientId=u501f6444-4af5-4&from=paste&id=u0597f588&originHeight=613&originWidth=925&originalType=url&ratio=1&rotation=0&showTitle=false&status=done&style=none&taskId=u6ac90fdf-137f-4c17-a568-cfd935baeaf&title=)

1. 你（客户端）和朋友（服务器）通话结束后，告诉他你想挂电话了。这就像是发送一个FIN（结束）信号，表示你想结束这次连接。（client向server发送fin。client置为FIN_WAIT_1）
2. 朋友听到你想挂电话了，他回应说“知道了，但我还有点事情要处理”，即使他知道对话即将结束。这就相当于服务器发送ACK（确认）信号，确认收到了你想结束连接的请求，但可能还需要一些时间来处理剩余的数据。（server向client发送ack。server置为CLOSE_WAIT，client置为FIN_WAIT_2）
3. 一段时间后，你的朋友处理完了他的事情，这时他打电话告诉你他也准备好挂电话了。这是服务器端发送第二个FIN信号，表明他现在也准备好结束这次连接。（等server传输数据完毕后，向client发送fin。server置为LAST_ACK）
4. 最后，你回复说你收到了他的消息，并同意现在可以挂电话了。这就是发送最后一个ACK信号，确认收到服务器端的结束请求。（client向server发送ack。client置为TIME_WAIT。之后等待2MSL，client关闭。server接收到后置为CLOSED）

> 其中等待2倍的最大报文段生存时间（2MSL，Maximum Segment Lifetime）是为了确保在网络中的所有剩余数据报文段都被丢弃，以防止旧的数据报文段在之后的连接中引发混淆或冲突。

# 知识扩展

## 为啥三次握手
TCP三次握手验证了client和server的收包和发包能力。

第一次握手：客户端发送网络包，服务端收到了。这样服务端就能得出结论：**客户端的发送能力、服务端的接收能力是正常的。**

第二次握手：服务端发包，客户端收到了。**这样客户端就能得出结论：服务端的接收、发送能力，客户端的接收、发送能力是正常的。不过此时服务器并不能确认客户端的接收能力是否正常。**

第三次握手：客户端发包，服务端收到了。**这样服务端就能得出结论：客户端的接收、发送能力正常，服务器自己的发送、接收能力也正常。**

所以，只有**三次握手才能确认双方的接收与发送能力是否正常。**

如果是两次握手，服务端无法确定客户端是否已经接收到了自己发送的初始序列号，如果第二次握手报文丢失，那么客户端就无法知道服务端的初始序列号，那 TCP 的可靠性就无从谈起。

客户端由于某种原因发送了两个不同序号的 SYN 包，我们知道网络环境是复杂的，旧的数据包有可能先到达服务器。如果是两次握手，服务器收到旧的 SYN 就会立刻建立连接，那么会造成网络异常。

如果是三次握手，服务器需要回复 SYN+ACK 包，客户端会对比应答的序号，如果发现是旧的报文，就会给服务器发 RST 报文，直到正常的 SYN 到达服务器后才正常建立连接。

所以三次握手才有足够的上下文信息来判断当前连接是否是历史连接。
## 为啥四次挥手
其实在 TCP 握手的时候，接收端发送 SYN+ACK 的包是将一个 ACK 和一个 SYN 合并到一个包中，所以减少了一次包的发送，三次完成握手。

对于四次挥手，因为** TCP 是全双工通信**，在主动关闭方发送 FIN 包后，接收端可能还要发送数据，不能立即关闭服务器端到客户端的数据通道，所以也就不能将服务器端的 FIN 包与对客户端的 ACK 包合并发送，只能先确认 ACK，然后服务器待无需发送数据时再发送 FIN 包，所以四次挥手时必须是四次数据包的交互。
