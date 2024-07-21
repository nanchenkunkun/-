# 典型回答

![](https://cdn.nlark.com/yuque/0/2023/png/5378072/1679215465250-69585686-78e5-4dba-a4b8-517d2e887bb3.png#averageHue=%235abb53&clientId=u8f30234d-c318-4&from=paste&id=u5b4efba4&originHeight=532&originWidth=982&originalType=url&ratio=1&rotation=0&showTitle=false&status=done&style=none&taskId=u6206a3b3-5805-4639-a570-f7c8bd883c4&title=)


OSI（Open System Interconnection，开放式系统互联）七层模型是计算机网络中一种通信协议的分类方式，分为以下七个层次：

1. 物理层（Physical Layer）：主要规定传输介质的传输方式，包括电信号、电压、光脉冲等。该层的主要协议是物理媒介相关协议，如RS232、V.35、以太网等。
2. 数据链路层（Data Link Layer）：在物理层上建立数据链路，对数据进行分帧、差错校验等处理，确保数据可靠地传输。该层的主要协议有点对点协议PPP（Point-to-Point Protocol）、高级数据链路控制协议HDLC（High-Level Data Link Control）、以太网协议等。
3. 网络层（Network Layer）：主要解决数据在网络中的传输问题，包括寻址、路由选择等。该层的主要协议有IP协议、网关协议（ARP）、路由协议（RIP、OSPF、BGP等）等。
4. 传输层（Transport Layer）：提供端到端的可靠传输服务，包括数据传输控制、流量控制等。该层的主要协议有TCP协议、UDP协议、SCTP协议等。
5. 会话层（Session Layer）：提供会话管理功能，负责建立、维护和结束会话。该层主要实现了不同计算机之间的会话控制，为高层协议提供一个传输数据的会话环境，该层的主要协议有NetBIOS等。
6. 表示层（Presentation Layer）：负责数据格式的转换，确保应用层数据的格式一致。该层主要实现了数据格式的转换和数据加密解密等功能，如JPEG、MPEG等。
7. 应用层（Application Layer）：提供应用程序之间的交互，包括文件传输、电子邮件、远程登录等。该层的主要协议有HTTP、FTP、SMTP、DNS、TELNET、SNMP等。

<br /> 
# 扩展知识

## TCP/IP 五层模型和四层模型

TCP/IP五层模型是互联网标准体系结构，它将网络通信协议分为五个层次，分别是：

1. 物理层（Physical Layer）：定义物理设备标准，如网线、光纤、网卡等。
2. 数据链路层（Data Link Layer）：在物理层的基础上，定义了数据的格式、传输速率等，如以太网协议、Wi-Fi协议等。
3. 网络层（Network Layer）：处理数据包的传输，确定网络地址，以及路由选择等，如IP协议、ICMP协议等。
4. 传输层（Transport Layer）：建立端到端的连接，保证数据的完整性和可靠性，如TCP协议、UDP协议等。
5. 应用层（Application Layer）：处理特定的应用程序，如HTTP协议、FTP协议等。


![](https://cdn.nlark.com/yuque/0/2023/png/5378072/1679215512409-97a70e95-730c-4e33-9aa9-b32958e39a92.png#averageHue=%235abb53&clientId=u8f30234d-c318-4&from=paste&id=u6dbb3632&originHeight=531&originWidth=689&originalType=url&ratio=1&rotation=0&showTitle=false&status=done&style=none&taskId=uf0bff398-4f63-40a8-a871-be73aefa481&title=)

**还有四层模型，就是在五层的基础上，将数据链路层和物理层合并成了一个层次，叫做网络接口层**

![](https://cdn.nlark.com/yuque/0/2023/png/5378072/1679215841660-1221d7ed-9d65-4237-9b41-a8f35fc5f540.png#averageHue=%23f5b544&clientId=uae8f49e4-5985-4&from=paste&id=ud67e0c17&originHeight=625&originWidth=873&originalType=url&ratio=1&rotation=0&showTitle=false&status=done&style=none&taskId=u0e026cc2-b25c-42aa-9cdc-9fff2adae45&title=)
