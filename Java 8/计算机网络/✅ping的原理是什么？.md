# 典型回答
ping是用来探测本机与网络中另一主机之间是否可达的命令，如果两台主机之间ping不通，则表明这两台主机不能建立起连接。利用ping命令可以检查网络是否通畅或者网络连接速度，很好地分析和判定网络故障。

ping的原理是基于ICMP（Internet Control Messages Protocol）来工作的，即因特网信报控制协议；

ping命令利用了ICMP两种类型的控制消息：“echo request”（回显请求）、“echo reply”（回显应答）。

在主机A上执行ping命令，目标主机是B。在A主机上就会发送“echo request”（回显请求）控制消息，主机B正确接收后即发回“echo reply”（回显应答）控制消息，通过对方回复的数据包来确定两台网络机器是否连接相通，时延是多少。

ping命令本身处于应用层，相当于一个应用程序。它使用的ICMP协议是一个网络层协议，也就是说，ping是一个应用层直接使用网络层协议的例子。像我们常见的HTTP协议是依赖的传输层协议TCP/UDP，而传输层的协议再依赖网络层的IP协议。

# 扩展知识

[✅ping为什么不需要端口？](https://www.yuque.com/hollis666/fo22bm/pfmnefsmxrwhzd81?view=doc_embed)
