# 典型回答

在操作系统中，零拷贝指的是避免在用户态（User-space）与内核态（Kernel-space）之间来回拷贝数据。

[什么是零拷贝？](https://www.yuque.com/hollis666/fo22bm/edxez2ggicn8thzq?view=doc_embed)

而Netty的零拷贝模型和操作系统中的零拷贝模型并不完全一样。他主要指的是在操作数据时, 不需要将数据 buffer从 一个内存区域拷贝到另一个内存区域。少了一次内存的拷贝，CPU 效率就得到的提升。

Netty的零拷贝主要体现在以下 5 个方面：

- **直接使用堆外内存**，避免 JVM 堆内存到堆外内存的数据拷贝。
- **CompositeByteBuf 类**，可以组合多个 Buffer 对象合并成一个逻辑上的对象，避免通过传统内存拷贝的方式将几个 Buffer 合并成一个大的 Buffer。
- 通过** Unpooled.wrappedBuffer** 可以将 byte 数组包装成 ByteBuf 对象，包装过程中不会产生内存拷贝。
- **ByteBuf.slice** 操作与 Unpooled.wrappedBuffer 相反，slice 操作可以将一个 ByteBuf 对象切分成多个 ByteBuf 对象，切分过程中不会产生内存拷贝，底层共享一个 byte 数组的存储空间。
- **使用 FileRegion 实现文件传输**，FileRegion 底层封装了 FileChannel#transferTo() 方法，可以将文件缓冲区的数据直接传输到目标 Channel，避免内核缓冲区和用户态缓冲区之间的数据拷贝，这属于操作系统级别的零拷贝。

# 扩展知识
## 堆外内存

[✅什么是堆外内存？如何使用堆外内存？](https://www.yuque.com/hollis666/fo22bm/roit5c9y04z6fqae?view=doc_embed)

我们都知道。Java在将数据发送出去的时候，会先将数据从堆内存拷贝到堆外内存，然后才会将堆外内存再拷贝到内核态，进行消息的收发，代码如下：

```java
static int write(FileDescriptor paramFileDescriptor, 
                 ByteBuffer paramByteBuffer, long paramLong, 
                 NativeDispatcher paramNativeDispatcher) throws IOException{
	// 如果是直接内存，则直接写入
    if((paramByteBuffer instanceof DirectBuffer)) {
        return writeFromNativeBuffer(paramFileDescriptor, paramByteBuffer, paramLong, paramNativeDispatcher);
    }
    // ...否则，先把数据拷贝到直接内存中
    ByteBuffer localByteBuffer = Util.getTemporaryDirectBuffer(k);
    try {
        localByteBuffer.put(paramByteBuffer);
        localByteBuffer.filp();
        paramByteBuffer.position(i);
        int m = writeFromNativeBuffer(paramFileDescriptor, localByteBuffer, paramLong, paramNativeDispatcher);
    }
}
```

所以，我们发现，假如我们在收发报文的时候使用直接内存，那么就可以减少一次内存拷贝，Netty就是这么做的。

Netty在通信层进行字节流的接收和发送的时候，如果应用允许Unsafe访问，则会采用DirectByteBuf进行转换，也就是堆外的直接内存，代码如下：

```java
public ByteBuf ioBuffer(int initialCapacity) {
    if (PlatformDependent.hasUnsafe() || isDirectBufferPooled()) {
    return directBuffer(initialCapacity);
}
return heapBuffer(initialCapacity);
}
```

## CompositeByteBuf

考虑一种场景，当一个数据包被拆成了两个字节流通过TCP传输过来后，那么对于接收者的机器来说，为了方便解析，它需要新建一个ByteBuf将这两个字节流重组成一个新的数据包，如下图所示：<br />![image.png](https://cdn.nlark.com/yuque/0/2023/png/719664/1673186575978-cb175194-a745-4ce2-a617-1e1beeb75e3e.png#averageHue=%23f5f5f5&clientId=u70018443-294e-4&from=paste&height=168&id=u91ca6a01&originHeight=336&originWidth=410&originalType=binary&ratio=1&rotation=0&showTitle=false&size=12921&status=done&style=none&taskId=u25f56448-367f-4d3c-8b4c-b5b88a8715b&title=&width=205)<br />那么在这种情况下，我们如果直接将两个字节流拷贝到一个新的字节流中，显然会浪费空间和时间，所以Netty推出了CompositeByteBuf，专门用来拷贝ByteBuf<br />![image.png](https://cdn.nlark.com/yuque/0/2023/png/719664/1673186297380-7ba2e802-0d0d-444c-b9af-f987e330ca9e.png#averageHue=%23eaeaea&clientId=u70018443-294e-4&from=paste&height=225&id=uafe35a04&originHeight=450&originWidth=997&originalType=binary&ratio=1&rotation=0&showTitle=false&size=52443&status=done&style=none&taskId=u6c75c9ed-5a47-4fff-b305-508a3d7942a&title=&width=499)<br />从图中可以看到，实际的Buf还是两个，只不过Netty通过CompositeByteBuf将老的buf通过指针组合映射到新的Buf中，减少了一次拷贝过程。

## Unpooled.wrappedBuffer

Unpooled.wrappedBuffer 是创建 CompositeByteBuf 对象的另一种推荐做法。

Unpooled.wrappedBuffer 方法可以将不同的数据源的一个或者多个数据包装成一个大的 ByteBuf 对象，其中数据源的类型包括 byte[]、ByteBuf、ByteBuffer。包装的过程中不会发生数据拷贝操作，包装后生成的 ByteBuf 对象和原始 ByteBuf 对象是共享底层的 byte 数组。

## ByteBuf.slice
ByteBuf.slice 和 Unpooled.wrappedBuffer 的逻辑正好相反，ByteBuf.slice 是将一个 ByteBuf 对象切分成多个共享同一个底层存储的 ByteBuf 对象。

## FileRegion文件传输
Netty 使用 FileRegion 实现文件传输的零拷贝，而FileRegion其实是基于Java底层的FileChannel#tranferTo方法实现的。它可以根据操作系统直接将文件缓冲区的数据发送到目标channel，底层借助了sendFile能力避免了传统通过循环write方式导致的内存拷贝问题。所以 FileRegion 是操作系统级别的零拷贝。

JavaDoc的注释如下：
> This method is potentially much more efficient than a simple loop that reads from this channel and writes to the target channel. **Many operating systems can transfer bytes directly from the filesystem cache to the target channel without actually copying them.**
> <br />

### sendFile
JDK原生的FileChannel#tranferTo方法其实是基于了Linux的sendFile方法，通过该方法，数据可以直接在内核空间内部进行 I/O 传输，从而省去了数据在用户空间和内核空间之间的来回拷贝。工作原理如下图：

![image.png](https://cdn.nlark.com/yuque/0/2023/png/719664/1673266150227-a16e0d98-9fda-47c4-ad81-c795d391dcf6.png#averageHue=%23f5f4f4&clientId=uddca59bf-923c-4&from=paste&height=295&id=u90b1500a&originHeight=590&originWidth=948&originalType=binary&ratio=1&rotation=0&showTitle=false&size=43894&status=done&style=none&taskId=uadf52e5a-83bf-4b34-ab83-8dd3f22e90b&title=&width=474)

