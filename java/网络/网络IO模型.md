# 网络IO模型总结

  IO模型就是说用什么样的通道进行数据的发送和接收，首先要明确一点：IO是操作系统与其他网络进行数据交互，JDK底层并没有实现IO，而是对操作系统内核函数做的一个封装，IO代码进入底层其实都是native形式的。Java共支持3种网络编程IO模式：BIO，NIO，AIO。



## BIO

BIO（Blocking IO）又称为同步阻塞IO，一个客户端由一个线程来处理，线程模型图下所示

![深入理解BIO、NIO、AIO线程模型_线程模型、netty](E:\gitWork\-\java\网络\img\e183d94a6387229aed326b4f230b7d98.png)

**BIO代码示例：**

```java
    public static void main(String[] args) throws IOException {

        ServerSocket serverSocket = new ServerSocket();
        serverSocket.bind(new InetSocketAddress(8080));

        
        while(true){
            //如果没有请求的话，accept会阻塞当前流程。
            Socket socket = serverSocket.accept();
            new Thread(() -> {
                try {
                    System.out.println(Thread.currentThread().getName());
                    InputStream inputStream = socket.getInputStream();
                    Reader reader = new InputStreamReader(inputStream);
                    BufferedReader bufferedReader = new BufferedReader(reader);
                    StringBuilder stringBuilder = new StringBuilder();
                    while(bufferedReader.ready()){
                        stringBuilder.append(bufferedReader.readLine() + "\n");
                    }
//                    System.out.println(stringBuilder.toString());
                    socket.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }
        
        //使用线程池的方式
        
//        while(true){
//            Socket socket = serverSocket.accept();
//
//            executorService.submit(() -> {
//                try {
//                    System.out.println(Thread.currentThread().getName());
//                    InputStream inputStream = socket.getInputStream();
//                    Reader reader = new InputStreamReader(inputStream);
//                    BufferedReader bufferedReader = new BufferedReader(reader);
//                    StringBuilder stringBuilder = new StringBuilder();
//                    while(bufferedReader.ready()){
//                        stringBuilder.append(bufferedReader.readLine() + "\n");
//                    }
////                    System.out.println(stringBuilder.toString());
//
//                    socket.close();
//
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            });
//        }
        
}
```

1.如果BIO使用单线程接受连接，则会阻塞其他连接，效率较低。
2.如果使用多线程虽然减弱了单线程带来的影响，但当有大并发进来时，会导致服务器线程太多，压力太大而崩溃。就是会开启过多的线程，这样容易导致系统奔溃。
3.就算使用线程池，也只能同时允许有限个数的线程进行连接，如果并发量远大于线程池设置的数量，还是与单线程无异
4.IO代码里read操作是阻塞操作，如果连接不做数据读写操作会导致线程阻塞，就是说只占用连接，不发送数据，则会浪费资源。比如线程池中500个连接，只有100个是频繁读写的连接，其他占着茅坑不拉屎，浪费资源！
5.另外多线程也会有线程切换带来的消耗

综上所述，BIO方式已经不适用于如下的大并发场景，仅适用于连接数目比较小且固定的架构。这种方式对服务器资源要求比较高，但BIO程序简单易理解。



## NIO

NIO（Non blocking IO）又被称为同步非阻塞io，服务器把多个连接放入集合中，只用一个线程可以处理多个请求，也就是多路复用。

1**.同步**：调用的结果会在本次调用后返回，不存在异步线程回调之类的。

2.**非阻塞**：表现为线程不会一直等待，把连接接入集合之后，线程会一直轮询集合中的连接，有就处理，没有就继续接受请求；

NIO的多路复用底层主要是Linux内核（select、poll、epoll）。windows不支持epoll实现，windows底层基于winsock2函数实现的，三种的内核模型的如下所示：

|              | select                                   | poll                                     | epoll                                                        |
| ------------ | ---------------------------------------- | ---------------------------------------- | ------------------------------------------------------------ |
| 操作方式     | 遍历                                     | 遍历                                     | 回调                                                         |
| 底层实现方式 | 数组                                     | 链表                                     | 哈希表                                                       |
| IO效率       | 每次调用的进行线性遍历，时间复杂度为O(n) | 每次调用惊醒线性遍历，时间复杂度为O（n） | 事件通知方式，每当有IO事件就绪，系统注册的回调函数就会被调用，时间复杂度O（1） |
| 最大连接     | 有上限（1024个）                         | 无上限                                   | 无上限                                                       |

NIO的三大组件：

1.Buffer（缓冲区）：buffer底层就是个数组；

2.Channel（通道）：channel类似于流，每个channel对应一个buffer缓冲区；

3.Selector（多路复用器）：channel会注册到selector上，由selector根据channel读写事件的发生将其有某个空闲的线程处理；

![深入理解BIO、NIO、AIO线程模型_线程模型、netty_04](E:\gitWork\-\java\网络\img\6397b9b855f852980d5893e0266f5d3c.png)

应用场景：
NIO方式适用于连接数目多且连接比较短（轻操作） 的架构， 比如聊天服务器， 弹幕系统， 服务器间通讯，编程比较复杂

```java
//用NIO实现网络编程
public static void server() throws IOException {
        Selector serverSelector = Selector.open();
        Selector clientSelector = Selector.open();
        new Thread(() -> {
            while(true){
                try{
                    if(clientSelector.select(1) > 0){
                        Set<SelectionKey> set = clientSelector.selectedKeys();
                        Iterator<SelectionKey> iterator = set.iterator();
                        while(iterator.hasNext()){
                            SelectionKey key = iterator.next();
                            if(key.isReadable()){
                                try{
                                    SocketChannel clientChannel = (SocketChannel) key.channel();
                                    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);

                                    clientChannel.read(byteBuffer);
                                    byteBuffer.flip();
                                    System.out.println(Charset.defaultCharset().newDecoder().decode(byteBuffer));

                                }finally {
                                    iterator.remove();
                                    key.interestOps(SelectionKey.OP_WRITE);
                                }
                            }
                        }
                    }
                }catch (Exception e){

                }


            }
        }).start();

        new Thread(() -> {
            try {
                ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
                serverSocketChannel.socket().bind(new InetSocketAddress(8080));
                serverSocketChannel.configureBlocking(false);
                serverSocketChannel.register(serverSelector, SelectionKey.OP_ACCEPT);
                while(true){
                    if(serverSelector.select(1) > 0){
                        Set<SelectionKey> set = serverSelector.selectedKeys();
                        Iterator<SelectionKey> iterator = set.iterator();
                        while(iterator.hasNext()){
                            SelectionKey key = iterator.next();
                            if(key.isAcceptable()){
                                try{
                                    SocketChannel clientChannel = ((ServerSocketChannel)key.channel()).accept();
                                    clientChannel.configureBlocking(false);
                                    clientChannel.register(clientSelector, SelectionKey.OP_READ);
                                }finally {
                                    iterator.remove();
                                }
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }


        }).start();

    }
```



## select、poll模型

​		NIO底层在JDK1.4版本是用linux的内核函数select()或poll()来实现，selector每次都会轮询所有的sockchannel看下哪个channel有读写事件，有的话就处理，没有就继续遍历，select和poll模型作为NIO的早期实现，存在一定弊端。下面是一段代码，简单表述一下他们的弊端！

```java
public class NioServer {

    // 保存客户端连接
    static List<SocketChannel> channelList = new ArrayList<>();

    public static void main(String[] args) throws IOException, InterruptedException {

        // 创建NIO ServerSocketChannel,与BIO的serverSocket类似
        ServerSocketChannel serverSocket = ServerSocketChannel.open();
        serverSocket.socket().bind(new InetSocketAddress(9000));
        
        // 设置ServerSocketChannel为非阻塞, 配置为true，则和BIO类似
        serverSocket.configureBlocking(false);
        System.out.println("服务启动成功");

        while (true) {
            // 非阻塞模式accept方法不会阻塞，否则会阻塞
            // NIO的非阻塞是由操作系统内部实现的，底层调用了linux内核的accept函数
            SocketChannel socketChannel = serverSocket.accept();
            
            if (socketChannel != null) { // 如果有客户端进行连接
                System.out.println("连接成功");
                // 设置SocketChannel为非阻塞
                socketChannel.configureBlocking(false);
                // 保存客户端连接在List中
                channelList.add(socketChannel);
            }
            
            // 遍历连接进行数据读取
            Iterator<SocketChannel> iterator = channelList.iterator();
            
            while (iterator.hasNext()) {
                SocketChannel sc = iterator.next();
                ByteBuffer byteBuffer = ByteBuffer.allocate(128);
                // 非阻塞模式read方法不会阻塞，否则会阻塞
                int len = sc.read(byteBuffer);
                // 如果有数据，把数据打印出来
                if (len > 0) {
                    System.out.println("接收到消息：" + new String(byteBuffer.array()));
                } else if (len == -1) { // 如果客户端断开，把socket从集合中去掉
                    iterator.remove();
                    System.out.println("客户端断开连接");
                }
            }
        }
    }
}

```

这种方式虽然解决了BIO的部分痛点，但并不是很完美。因为select和poll模型的底层实现、io效率、最大连接数在面对高并发时还存在一定弊端！他们的多路复用采用的是遍历Selector中所有的连接，然后对有事件的连接做出响应。 假如连接数太多，有10000个连接，其中只有1000个连接有写数据，但是由于其他9000个连接并没有断开，我们还是要每次轮询遍历一万次，其中有十分之九的遍历都是无效的，这显然不是一个让人很满意的状态。为了处理无效遍历的问题，在jdk1.5及以上版本引入了epoll模型



## epoll模型

   JDK1.5开始引入了epoll基于事件响应机制来优化NIO。epoll模型解决了elect和poll模型的无效遍历问题，是NIO的核心。epoll是基于事件响应的，类似于观察者模式！

NIO第二个版本：使用epoll模型后的代码示例

```java
public class NioSelectorServer {

    public static void main(String[] args) throws IOException, InterruptedException {

        // 创建NIO ServerSocketChannel
        ServerSocketChannel serverSocket = ServerSocketChannel.open();
        serverSocket.socket().bind(new InetSocketAddress(9000));
        // 设置ServerSocketChannel为非阻塞
        serverSocket.configureBlocking(false);
        // 打开Selector处理Channel，即创建epoll
        Selector selector = Selector.open();
        // 把ServerSocketChannel注册到selector上，并且selector对客户端accept连接操作感兴趣
        serverSocket.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("服务启动成功");

        while (true) {
            // 阻塞等待需要处理的事件发生
            selector.select();

            // 获取selector中注册的全部事件的 SelectionKey 实例
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectionKeys.iterator();

            // 遍历SelectionKey对事件进行处理
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                // 如果是OP_ACCEPT事件，则进行连接获取和事件注册
                if (key.isAcceptable()) {
                    ServerSocketChannel server = (ServerSocketChannel) key.channel();
                    SocketChannel socketChannel = server.accept();
                    socketChannel.configureBlocking(false);
                    // 这里只注册了读事件，如果需要给客户端发送数据可以注册写事件
                    socketChannel.register(selector, SelectionKey.OP_READ);
                    System.out.println("客户端连接成功");
                } else if (key.isReadable()) {  // 如果是OP_READ事件，则进行读取和打印
                    SocketChannel socketChannel = (SocketChannel) key.channel();
                    ByteBuffer byteBuffer = ByteBuffer.allocate(128);
                    int len = socketChannel.read(byteBuffer);
                    // 如果有数据，把数据打印出来
                    if (len > 0) {
                        System.out.println("接收到消息：" + new String(byteBuffer.array()));
                    } else if (len == -1) { // 如果客户端断开连接，关闭Socket
                        System.out.println("客户端断开连接");
                        socketChannel.close();
                    }
                }
                //从事件集合里删除本次处理的key，防止下次select重复处理
                iterator.remove();
            }
        }
    }
}
```

 在使用epoll模型之后，对简单版本的NIO做了优化处理，可以理解为在第一个版本的NIO上，又增加了一个就绪事件列表集合，这个集合中存放着有事件响应的连接，然后开启一个线程去监听这个集合，有元素的话就进行处理。

   总结：NIO整个调用流程就是Java调用了操作系统的内核函数来创建Socket，获取到Socket的文件描述符，再创建一个Selector对象，对应操作系统的Epoll描述符，将获取到的Socket连接的文件描述符的事件绑定到Selector对应的Epoll文件描述符上，进行事件的异步通知，这样就实现了使用一条线程，并且不需要太多的无效的遍历，将事件处理交给了操作系统内核(操作系统硬中断程序实现)，大大提高了效率。

-----------------------------------
