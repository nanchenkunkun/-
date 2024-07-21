# 典型回答
[什么是TCP的粘包、拆包问题？](https://www.yuque.com/hollis666/fo22bm/qotkxkmmhwo0i5os?view=doc_embed)

针对TCP的粘包、拆包问题，Netty有自己的解决方式。

Netty通过预先指定的数据流编解码器，按照预先约定好的规则进行数据的解析，即可解决对应的粘包、拆包问题。

具体到代码层面，主要有以下几种解码器：

- **定长解码器 FixedLengthFrameDecoder**
   - 这个解码器按照指定的长度对接收到的数据进行分割。如果接收到的数据长度小于指定长度，它会等待直到收集到足够的数据。
- **行分隔符解码器 LineBasedFrameDecoder**
   - 它根据行分隔符（如 \n 或 \r\n）来处理粘包和拆包问题。这个解码器会按行分隔数据。
- **分隔符解码器 DelimiterBasedFrameDecoder**
   - 这个解码器允许指定一个或多个分隔符。当遇到分隔符时，它将数据分割成一个个完整的帧。
- **长度字段解码器 LengthFieldBasedFrameDecoder**
   - 这是一种更复杂的解决方案，适用于更复杂的协议。在这种方式中，消息中包含表示消息长度的字段。解码器根据这个长度字段来确定每个消息的边界。
- **自定义解码器**
   - 如果预定义的解码器不满足需求，Netty 还允许开发者实现自定义的解码器。通过扩展 **ByteToMessageDecoder **类，可以处理任何复杂的粘包和拆包场景。

除了解码器之外，Netty 也提供了编码器，用于在发送数据前按照特定规则格式化消息。例如，对于 LengthFieldBasedFrameDecoder，可以使用 LengthFieldPrepender 编码器来在消息前添加长度字段。

这些解决方案全被封装到了handler中，我们可以基于Netty的责任链模式，进行如下调用即可：

```java
serverBootstrap.group(bossGroup, workerGroup)
    // channel fact
    .channel(NioServerSocketChannel.class)
    .childHandler(new ChannelInitializer<SocketChannel>() {
        @Override
        public void initChannel(SocketChannel ch) {
            ch.pipeline.addLast(new FixedLenghtFrameDecoder());
        }
    }
);
```
