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



BIO几个致命的缺点：

1.如果BIO使用单线程接受连接，则会阻塞其他连接，效率较低。
2.如果使用多线程虽然减弱了单线程带来的影响，但当有大并发进来时，会导致服务器线程太多，压力太大而崩溃。就是会开启过多的线程，这样容易导致系统奔溃。
3.就算使用线程池，也只能同时允许有限个数的线程进行连接，如果并发量远大于线程池设置的数量，还是与单线程无异
4.IO代码里read操作是阻塞操作，如果连接不做数据读写操作会导致线程阻塞，就是说只占用连接，不发送数据，则会浪费资源。比如线程池中500个连接，只有100个是频繁读写的连接，其他占着茅坑不拉屎，浪费资源！
5.另外多线程也会有线程切换带来的消耗

综上所述，BIO方式已经不适用于如下的大并发场景，仅适用于连接数目比较小且固定的架构。这种方式对服务器资源要求比较高，但BIO程序简单易理解。



**todo**