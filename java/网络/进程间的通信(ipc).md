# 进程中的通信IPC

原文地址：https://blog.csdn.net/weixin_42005898/article/details/117081695



## 1.为什么需要进程中的通信

**1.数据传输**

一个进程需要将它的数据发送给另外一个进程。

**2.资源共享**

多个进程之间共享同样的资源

**3.通知事件**

一个进程需要向另一个或一组进程发送消息，通知它们发生了某种事件。

**4.进程控制**

有些进程希望完全控制另外一个进程的执行（如Debug进程），该控制进程希望能够拦截另一个进程的所有操作，并能够及时知道它的状态改变。



## **2.什么是进程之间的通信**



**1.进程隔离**

​		进程隔离是为保护操作系统中进程互不干扰而设计的一组不同硬件和软件的技术。这个技术是为了避免进程A写入进程B的情况发生。 进程的隔离实现，使用了虚拟地址空间。进程A的虚拟地址和进程B的虚拟地址不同，这样就防止进程A将数据信息写入进程B。



**2.虚拟地址空间**

​		就32位系统而言，当创建一个进程时，操作系统会为该进程分配一个 4GB 大小的虚拟进程地址空间。之所以是 4GB ，是因为在 32 位的操作系统中，一个指针长度是 4 字节，而 4 字节指针的寻址能力是从 0x00000000~0xFFFFFFFF ，最大值 0xFFFFFFFF 表示的即为 4GB 大小的容量。与虚拟地址空间相对的，还有一个物理地址空间，这个地址空间对应的是真实的物理内存。要注意的是这个 4GB 的地址空间是“虚拟”的，并不是真实存在的，而且每个进程只能访问自己虚拟地址空间中的数据，无法访问别的进程中的数据，通过这种方法实现了进程间的地址隔离。

   	 针对 Linux 操作系统，将最高的1G字节（从虚拟地址 0xC0000000 到 0xFFFFFFFF ）供内核使用，称为内核空间，而较低的 3G 字节（从虚拟地址 0x00000000 到0xBFFFFFFF），供各个进程使用，称为用户空间。每个进程都可以通过系统调用进入到内核。其中在 Linux 系统中，进程的用户空间是独立的，而内核空间是共有的，进程切换时，用户空间切换，内核空间不变。

​    	创建虚拟地址空间目的是为了解决进程地址空间隔离的问题。但程序要想执行，必须运行在真实的内存上，所以，必须在虚拟地址与物理地址间建立一种映射关系。这样，通过映射机制，当程序访问虚拟地址空间上的某个地址值时，就相当于访问了物理地址空间中的另一个值。人们想到了一种分段、分页的方法，它的思想是在虚拟地址空间和物理地址空间之间做一一映射。这种思想理解起来并不难，操作系统保证不同进程的地址空间被映射到物理地址空间中不同的区域上，这样每个进程最终访问到的物理地址空间都是彼此分开的。通过这种方式，就实现了进程间的地址隔离。

```
寻址空间一般指的是CPU对于内存寻址的能力。通俗地说，就是能最多用到多少内存的一个问题。数据在存储器(RAM)中存放是有规律的 ，CPU在运算的时候需要把数据提取出来就需要知道数据存放在哪里 ，这时候就需要挨家挨户的找，这就叫做寻址，但如果地址太多超出了CPU的能力范围，CPU就无法找到数据了。 CPU最大能查找多大范围的地址叫做寻址能力 ，CPU的寻址能力以字节为单位 ，如32位寻址的CPU可以寻址2的32次方大小的地址也就是4G，这也是为什么32位的CPU最大能搭配4G内存的原因 ，再多的话CPU就找不到了。
```

![img](E:\gitWork\-\java\网络\img\8f6630ddcab82d5377a87ff214428428.png)

![img](E:\gitWork\-\java\网络\img\20170702190629154.jpg)



**进程间通信（IPC，InterProcess Communication）**是指在不同进程之间传播或交换信息。



## 3.IPC通信原理

每个进程各自有不同的用户地址空间，任何一个进程的全局变量在另一个进程中都看不到，**所以进程之间要交换数据必须通过内核,在内核中开辟一块缓冲区**,进程1把数据从用户空间拷到内核缓冲区,进程2再从内核缓冲区把数据读走,内核提供的这种机制称为进程间通信机制。通常的做法是消息发送方将要发送的数据存放在内存缓存区中，通过系统调用进入内核态。然后内核程序在内核空间分配内存，开辟一块内核缓存区，内核空间调用 copy_from_user() 函数将数据从用户空间的内存缓存区拷贝到内核空间的内核缓存区中。同样的，接收方进程在接收数据时在自己的用户空间开辟一块内存缓存区，然后内核程序调用 copy_to_user() 函数将数据从内核缓存区拷贝到接收进程的用户空间内存缓存区。这样数据发送方进程和数据接收方进程就完成了一次数据传输，我们称完成了一次进程间通信。

主要的过程如下图所示：

![img](E:\gitWork\-\java\网络\img\20210520164005689.png)



## 4.通信方式

​		 IPC的方式通常有linux下的 管道（Streams)（包括无名管道和命名管道）、消息队列、信号量、信号、共享存储、[Socket](https://so.csdn.net/so/search?q=Socket&spm=1001.2101.3001.7020)等。其中 Socket和Streams支持不同主机上的两个进程IPC，以及android下的Binder。

### 一、管道

管道，通常指无名管道，是unix系统ipc最古老的形式。

#### 1.特点：

1.它是半双工（即数据只能在一个方向上流动），具有固定的读端和写端。

2.它是只能用于具有亲缘关系的进程之间的通信（也是父子进程或者兄弟进程之间）。

3.它可以看成是一种特殊的文件，对于它的读写也可以使用普通的read、write等函数，但是它不是普通的文件， 并不属于其他任何文件系统，并且只存在于内存中。



#### 2.原型

```c++
#include <unistd.h>
int pipe(int fd[2]);    // 返回值：若成功返回0，失败返回-1
```

当一个管道建立时，调用pipe函数 在内核中开辟一块缓冲区用于通信，它会创建两个文件描述符：`fd[0]`为读而打开，`fd[1]`为写而打开。如下图：

![img](E:\gitWork\-\java\网络\img\0fa667a0aeecc854f17c80bc5966fcf0.png)

要关闭管道只需将这两个文件描述符关闭即可。



#### 3.例子

单个进程中的管道几乎没有任何用处。所以，通常调用 pipe 的进程接着调用 fork，这样就创建了父进程与子进程之间的 IPC 通道。如下图所示：

![img](E:\gitWork\-\java\网络\img\d38afa5c0d915abbb0628e09a11e5204.png)

若要数据流从父进程流向子进程，则关闭父进程的读端（`fd[0]`）与子进程的写端（`fd[1]`）；反之，则可以使数据流从子进程流向父进程。

```c++
#include<stdio.h>
#include<unistd.h>
 
int main()
{
    int fd[2];  // 两个文件描述符
    pid_t pid;
    char buff[20];
 
    if(pipe(fd) < 0)  // 创建管道
        printf("Create Pipe Error!\n");
 
    if((pid = fork()) < 0)  // 创建子进程
    {
        printf("Fork Error!\n");
    }
    else if(pid > 0)  // 父进程
    {
        close(fd[0]); // 关闭读端
        write(fd[1], "hello world\n", 12);
    }
    else
    {
        close(fd[1]); // 关闭写端
        read(fd[0], buff, 20);
        printf("%s", buff);
    }
 
    return 0;
}
```



### 二、FIFO

FIFO，也称之为命名管道，它是一种文件类型

#### 1.特点

1.FIFO可以在无关的进程之间交换数据，与无名管道不通，

2.FIFO有路径名与之相关联，它以一种特殊设备文件形式存在于文件系统之中。

#### 2.原型

```c++
#include <sys/stat.h>
// 返回值：成功返回0，出错返回-1
int mkfifo(const char *pathname, mode_t mode);
```

其中的 mode 参数与open函数中的 mode 相同。一旦创建了一个 FIFO，就可以用一般的文件I/O函数操作它。

当 open 一个FIFO时，是否设置非阻塞标志（O_NONBLOCK）的区别：

若没有指定O_NONBLOCK（默认），只读 open 要阻塞到某个其他进程为写而打开此 FIFO。类似的，只写 open 要阻塞到某个其他进程为读而打开它。

若指定了O_NONBLOCK，则只读 open 立即返回。而只写 open 将出错返回 -1 如果没有进程已经为读而打开该 FIFO，其errno置ENXIO。

#### 3.例子

FIFO的通信方式类似于在进程中使用文件来传输数据，只不过FIFO类型文件同时具有管道的特性。在数据读出时，FIFO管道中同时清除数据，并且“先进先出”。下面的例子演示了使用 FIFO 进行 IPC 的过程：

```c++
#include<stdio.h>
#include<stdlib.h>   // exit
#include<fcntl.h>    // O_WRONLY
#include<sys/stat.h>
#include<time.h>     // time
 
int main()
{
    int fd;
    int n, i;
    char buf[1024];
    time_t tp;
 
    printf("I am %d process.\n", getpid()); // 说明进程ID
 
    if((fd = open("fifo1", O_WRONLY)) < 0) // 以写打开一个FIFO
    {
        perror("Open FIFO Failed");
        exit(1);
    }
 
    for(i=0; i<10; ++i)
    {
        time(&tp);  // 取系统当前时间
        n=sprintf(buf,"Process %d's time is %s",getpid(),ctime(&tp));
        printf("Send message: %s", buf); // 打印
        if(write(fd, buf, n+1) < 0)  // 写入到FIFO中
        {
            perror("Write FIFO Failed");
            close(fd);
            exit(1);
        }
        sleep(1);  // 休眠1秒
    }
 
    close(fd);  // 关闭FIFO文件
    return 0;
}
```



### 三、消息队列

#### 1.特点

消息队列，顾名思义，想必看到的公共资源可能就是一种队列。这种队列满足数据结构里队列的特点：先进先出。消息队列提供了一个进程向另一个进程发送一块数据快的通信方法，注意，是以块为基本单位，前面的管道是以字节流为基本单位。每个数据块都被认为是由类型的。接收者进程接受数据块可以有不同的类型值，比如可以是结构体型。每个消息 队列的最大长度是上限的(MSGMAX)，每个消息队列的总的字节数也是有上限的(MSGMNB),系统的消息队列总数也是有上线的(MSGMNI)

```c++
#define MSGMNI  16         // 消息队列总数上线 
#define MSGMAX  8192       // 消息队列最大长度上线
#define MSGMNB  16384      // 消息队列总的字节数上线
```

![img](E:\gitWork\-\java\网络\img\20210521143208104.png)

 一个消息队列由一个标识符（即队列ID）来标识。每个消息队列都有一个队列头，用结构struct msg_queue来描述。队列头中包含了该消息队列的大量信息，包括消息队列键值、用户ID、组ID、消息队列中消息数目等等，甚至记录了最近对消息队列读写进程的ID。读者可以访问这些信息，也可以设置其中的某些信息。

结构msg_queue用来描述消息队列头，存在于内核空间：


```c++
 struct msg_queue {
    struct kern_ipc_perm q_perm;
    time_t q_stime;        /* last msgsnd time */
    time_t q_rtime;        /* last msgrcv time */
    time_t q_ctime;        /* last change time */
    unsigned long q_cbytes;    /* current number of bytes on queue */
    unsigned long q_qnum;      /* number of messages in queue */
    unsigned long q_qbytes;    /* max number of bytes on queue */
    pid_t q_lspid;          /* pid of last msgsnd */
    pid_t q_lrpid;          /* last receive pid */
    struct list_head q_messages;
    struct list_head q_receivers;
    struct list_head q_senders;
};
```



 结构msqid_ds用来设置或返回消息队列的信息，存在于用户空间：

```c++
struct msqid_ds {
    struct ipc_perm msg_perm;
    struct msg *msg_first;      /* first message on queue,unused  */
    struct msg *msg_last;      /* last message in queue,unused */
    __kernel_time_t msg_stime;  /* last msgsnd time */
    __kernel_time_t msg_rtime;  /* last msgrcv time */
    __kernel_time_t msg_ctime;  /* last change time */
    unsigned long  msg_lcbytes; /* Reuse junk fields for 32 bit */
    unsigned long  msg_lqbytes; /* ditto */
    unsigned short msg_cbytes;  /* current number of bytes on queue */
    unsigned short msg_qnum;    /* number of messages in queue */
    unsigned short msg_qbytes;  /* max number of bytes on queue */
    __kernel_ipc_pid_t msg_lspid;  /* pid of last msgsnd */
    __kernel_ipc_pid_t msg_lrpid;  /* last receive pid */
};
```

下图说明了内核与消息队列是怎样建立起联系的：

其中：struct ipc_ids msg_ids是内核中记录消息队列的全局数据结构；struct msg_queue是每个消息队列的队列头。



![img](E:\gitWork\-\java\网络\img\c009c586ad93247433f89f2daadf98b8.png)

 从上图可以看出，全局数据结构 struct ipc_ids msg_ids 可以访问到每个消息队列头的第一个成员：struct kern_ipc_perm；而每个struct kern_ipc_perm能够与具体的消息队列对应起来是因为在该结构中，有一个key_t类型成员key，而key则唯一确定一个消息队列。 kern_ipc_perm结构如下：


```c++
struct kern_ipc_perm{  //内核中记录消息队列的全局数据结构msg_ids能够访问到该结构；
    key_t  key;    //该键值则唯一对应一个消息队列
    uid_t  uid;
    gid_t  gid;
    uid_t  cuid;
    gid_t  cgid;
    mode_t  mode;
    unsigned long seq;
}
```

ipc_perm结构体如下：

```cpp
struct ipc_perm {
    key_t key;                        /* Key supplied to msgget() */
    uid_t uid;                         /* Effective UID of owner */
    gid_t gid;                        /* Effective GID of owner */
    uid_t cuid;                       /* Effective UID of creator */
    gid_t cgid;                      /* Effective GID of creator */
    unsigned short mode;    /* Permissions */
    unsigned short seq;       /* Sequence number */
};
```



1、特点
    1.消息队列是消息的链表,具有特定的格式,存放在内存中并由消息队列标识符标识.
    2.消息队列允许一个或多个进程向它写入与读取消息.
    3.管道和命名管道都是通信数据都是先进先出的原则。
    4.消息队列可以实现消息的随机查询,消息不一定要以先进先出的次序读取,也可以按消息的类型读取.比FIFO更有优势。

#### 2.原型

```cpp
#include <sys/msg.h>
#include <sys/types.h>
#include <sys/ipc.h>
 
key_t ftok(char *pathname, char proj)；
  返回与路径pathname相对应的一个键值
  pathname：文件名（含路径）,通常设置为当前目录“.” 比如projid为'a',则为"./a"文件
  projid：项目ID，必须为非0整数（0-255）.
int msgget(key_t key, int flag); 
 创建或打开消息队列：成功返回队列ID，失败返回-1
 flag:
   IPC_CREAT:创建新的消息队列。
   IPC_EXCL:与IPC_CREAT一同使用，表示如果要创建的消息队列已经存在，则返回错误。
   IPC_NOWAIT:读写消息队列要求无法满足时，不阻塞
 
 
int msgsnd(int msqid,  struct msgbuf *msgp,  size_t msgsz,  int msgflag);
 添加消息：成功返回0，失败返回-1
 msqid:已打开的消息队列id
 msgp:存放消息的结构体指针。
 msgflag:函数的控制属性。
 消息结构msgbuf为:
 struct msgbuf
 {
    long mtype;//消息类型
    char mtext[1];//消息正文，消息数据的首地址，这个数据的最大长度为8012吧，又可把他看成是一个结构，也有类型和数据，recv时解析即可。
  }
  msgsz:消息数据的长度。
  msgflag:
      IPC_NOWAIT: 指明在消息队列没有足够空间容纳要发送的消息时，msgsnd立即返回。
      0:msgsnd调用阻塞直到条件满足为止.（一般选这个）
 
 
int msgrcv(int msqid,  struct msgbuf *msgp,  size_t msgsz,  long msgtype,  int msgflag);
读取消息：成功返回消息数据的长度，失败返回-1
msqid:已打开的消息队列id
    msgp:存放消息的结构体指针。msgp->mtype与第四个参数是相同的。
    msgsz:消息的字节数，指定mtext的大小。
    msgtype:消息类型，消息类型 mtype的值。如果为0，则接受该队列中的第一条信息，如果小于0，则接受小于该值的绝对值的消息类型，如果大于0，接受指定类型的消息，即该值消息。
    msgflag:函数的控制属性。
    msgflag:
        MSG_NOERROR:若返回的消息比nbytes字节多,则消息就会截短到nbytes字节,且不通知消息发送进程.
        IPC_NOWAIT:调用进程会立即返回.若没有收到消息则返回-1.
        0:msgrcv调用阻塞直到条件满足为止.
在成功地读取了一条消息以后，队列中的这条消息将被删除。
 
 
int msgctl(int msqid, int cmd, struct msqid_ds *buf);
 控制消息队列：成功返回0，失败返回-1
 msqid:消息队列ID，消息队列标识符，该值为msgget创建消息队列的返回值。
 cmd:
    IPC_STAT:将msqid相关的数据结构中各个元素的当前值存入到由buf指向的结构中.
    IPC_SET:将msqid相关的数据结构中的元素设置为由buf指向的结构中的对应值.
    IPC_RMID:删除由msqid指示的消息队列,将它从系统中删除并破坏相关数据结构.
 buf:消息队列缓冲区
```



在以下两种情况下，msgget将创建一个新的消息队列：

如果没有与键值key相对应的消息队列，并且flag中包含了IPC_CREAT标志位。
key参数为IPC_PRIVATE。
函数msgrcv在读取消息队列时，type参数有下面几种情况：

type == 0，返回队列中的第一个消息；
type > 0，返回队列中消息类型为 type 的第一个消息；
type < 0，返回队列中消息类型值小于或等于 type 绝对值的消息，如果有多个，则取类型值最小的消息。
可以看出，type值非 0 时用于以非先进先出次序读消息。也可以把 type 看做优先级的权值。

#### 3.例子

下面写了一个简单的使用消息队列进行IPC的例子，服务端程序一直在等待特定类型的消息，当收到该类型的消息以后，发送另一种特定类型的消息作为反馈，客户端读取该反馈并打印出来。



    #include <stdio.h>
    #include <stdlib.h>
    #include <sys/msg.h>
    
    // 用于创建一个唯一的key
    #define MSG_FILE "/etc/passwd"
    
    // 消息结构
    struct msg_form {
        long mtype;
        char mtext[256];
    };
    
    int main()
    {
        int msqid;
        key_t key;
        struct msg_form msg;
    // 获取key值
    if((key = ftok(MSG_FILE,'z')) < 0)
    {
        perror("ftok error");
        exit(1);
    }
     
    // 打印key值
    printf("Message Queue - Server key is: %d.\n", key);
     
    // 创建消息队列
    if ((msqid = msgget(key, IPC_CREAT|0777)) == -1)
    {
        perror("msgget error");
        exit(1);
    }
     
    // 打印消息队列ID及进程ID
    printf("My msqid is: %d.\n", msqid);
    printf("My pid is: %d.\n", getpid());
     
    // 循环读取消息
    for(;;)
    {
        msgrcv(msqid, &msg, 256, 888, 0);// 返回类型为888的第一个消息
        printf("Server: receive msg.mtext is: %s.\n", msg.mtext);
        printf("Server: receive msg.mtype is: %d.\n", msg.mtype);
     
        msg.mtype = 999; // 客户端接收的消息类型
        sprintf(msg.mtext, "hello, I'm server %d", getpid());
        msgsnd(msqid, &msg, sizeof(msg.mtext), 0);
    }
    return 0;
}




    #include <stdio.h>
    #include <stdlib.h>
    #include <sys/msg.h>
    
    // 用于创建一个唯一的key
    #define MSG_FILE "/etc/passwd"
    
    // 消息结构
    struct msg_form {
        long mtype;
        char mtext[256];
    };
    
    int main()
    {
        int msqid;
        key_t key;
        struct msg_form msg;
    // 获取key值
    if ((key = ftok(MSG_FILE, 'z')) < 0)
    {
        perror("ftok error");
        exit(1);
    }
     
    // 打印key值
    printf("Message Queue - Client key is: %d.\n", key);
     
    // 打开消息队列
    if ((msqid = msgget(key, IPC_CREAT|0777)) == -1)
    {
        perror("msgget error");
        exit(1);
    }
     
    // 打印消息队列ID及进程ID
    printf("My msqid is: %d.\n", msqid);
    printf("My pid is: %d.\n", getpid());
     
    // 添加消息，类型为888
    msg.mtype = 888;
    sprintf(msg.mtext, "hello, I'm client %d", getpid());
    msgsnd(msqid, &msg, sizeof(msg.mtext), 0);
     
    // 读取类型为999的消息
    msgrcv(msqid, &msg, 256, 999, 0);
    printf("Client: receive msg.mtext is: %s.\n", msg.mtext);
    printf("Client: receive msg.mtype is: %d.\n", msg.mtype);
    return 0;
}
