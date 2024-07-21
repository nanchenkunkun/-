# 典型回答

**Redis 使用自己设计的一种文本协议进行客户端与服务端之间的通信——RESP（REdis Serialization Protocol），**这种协议简单、高效，易于解析，被广泛使用。

**RESP 协议基于 TCP 协议**，采用请求/响应模式，每条请求由多个参数组成，以命令名称作为第一个参数。请求和响应都以行结束符（\r\n）作为分隔符，具体格式如下：

```java
*<number of arguments>\r\n
$<length of argument 1>\r\n
<argument data>\r\n
...
$<length of argument N>\r\n
<argument data>\r\n
```

其中，<number of arguments> 表示参数个数，<length of argument> 表示参数数据的长度，<argument data> 表示参数数据。参数可以是字符串、整数、数组等数据类型。

例如，以下是一个 Redis 协议的示例请求和响应：<br />请求：

```java
*3\r\n
$3\r\n
SET\r\n
$5\r\n
mykey\r\n
$7\r\n
myvalue\r\n
```

响应：
```
+OK\r\n
```

上面的请求表示向 Redis 服务器设置一个名为 "mykey" 的键，值为 "myvalue"。响应返回 "+OK" 表示操作成功。

> "$3\r\n"表示参数长度为3，即下一个参数是一个3个字符的字符串。它表示要执行的命令是"SET"，即设置键值对。
> 
> "$5\r\n"表示参数长度为5，即下一个参数是一个5个字符的字符串。它表示要设置的键是"mykey"。
> 
> "$7\r\n"表示参数长度为7，即下一个参数是一个7个字符的字符串。它表示要设置的值是"myvalue"。


除了基本的 GET、SET 操作，Redis 还支持事务、Lua 脚本、管道等高级功能，这些功能都是通过 Redis 协议来实现的。
