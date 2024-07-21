# 典型回答

select, poll, 和 epoll 都是 Linux 中常见的 I/O 多路复用技术，它们可以用于同时监听多个文件描述符（file descriptor，后文简称fd），当任意一个文件描述符就绪时，就能够非阻塞的读写数据。

- select 是最原始的 I/O 多路复用技术，它的缺点是最多只能监听 1024 个文件描述符。
- poll 在 select 的基础上增加了支持监听更多的文件描述符的能力，但是复杂度随着监听的文件描述符数量的增加而增加。
- epoll 在 poll 的基础上进一步优化了复杂度，可以支持更多的文件描述符，并且具有更高的效率。
## select
函数签名如下：
```c
int select (int n, fd_set *readfds, fd_set *writefds, fd_set *exceptfds, struct timeval *timeout);
```
select函数可以监听read，write，except的fd。当select返回后，可以遍历对应的fd_set来寻找就绪的fd，从而进行业务处理

**select诞生比较早，几乎在所有的平台中都支持。但是select有个缺点就是单个进程能够监视的文件描述符的数量存在最大限制，在Linux上一般为1024，可以通过修改宏定义甚至重新编译内核的方式提升这一限制，但是这样也会造成效率的降低。**

除此之外，包含大量fd的数组被整体复制于用户态和内核的地址空间之间，而不论这些文件描述符是否就绪，其开销也随着文件描述符数量增加而线性增大。
## poll
函数签名如下所示：

```c
int poll (struct pollfd *fds, unsigned int nfds, int timeout);
struct pollfd {
    int fd; /* file descriptor */
    short events; /* requested events to watch */
    short revents; /* returned events witnessed */
};
```

同select一样，poll返回后，也是需要轮询pollfd来获取就绪的fd。不仅如此，所有的fds也是在内核态和用户态中来回切换，也会影响效率。

但是因为fds基于链表，所以就没有了最长1024的限制。
## epoll
epoll基于Linux2.4.5，函数签名如下：
```c
// 创建一个epoll的句柄，size用来告诉内核这个监听的数目一共有多大
int epoll_create(int size)；
// 注册要监听的事件类型
int epoll_ctl(int epfd, int op, int fd, struct epoll_event *event)；
// 等待事件发生
int epoll_wait(int epfd, struct epoll_event * events, int maxevents, int timeout);
```

每次注册新的事件调用epoll_ctl时，epoll会把所有的fd拷贝进内核，而不是在epoll_wait的时候重复拷贝。epoll保证了每个fd在整个过程中只会拷贝一次。

同时，epoll会通过epoll_wait查看是否有就绪的fd，如果有就绪的fd，就会直接使用（O(1)）。而不是像之前两个一样，每次需要手动遍历才能得到就绪的fd（O(n)）

除此之外，它所支持的fd上限是最大可以打开文件的数目，这个数字一般远大于2048，举个例子，在1GB内存的机器上大约是10万左右，具体数目可以cat /proc/sys/fs/file-max查看，一般来说这个数目和系统内存关系很大。

# 知识扩展
## 三者的主要区别是啥？
|  | fd长度 | 遍历所有fd | 把fd从用户态copy到内核态 |
| --- | --- | --- | --- |
| select | 1024 | 是 | 是 |
| poll | 无限制 | 是 | 是 |
| epoll | 无限制 | 否 | 否 |

## epoll的两种模式是啥？

我们知道epoll是通过epoll_wait来获取就绪的fd，那么如果就绪的fd一直没有被消费，该如何处理呢？这就又了两种模式。LT（level trigger）和ET（edge trigger）。LT模式是默认模式，LT模式与ET模式的区别如下： 

1. LT模式：当epoll_wait检测到描述符事件发生并将此事件通知应用程序，应用程序可以不立即处理该事件。下次调用epoll_wait时，会再次响应应用程序并通知此事件
2. ET模式：当epoll_wait检测到描述符事件发生并将此事件通知应用程序，应用程序必须立即处理该事件。如果不处理，下次调用epoll_wait时，不会再次响应应用程序并通知此事件。

因为ET模式在很大程度上减少了epoll事件被重复触发的次数，因此效率要比LT模式高。epoll工作在ET模式的时候，必须使用非阻塞socket，以避免由于一个文件句柄的阻塞读/阻塞写操作把处理多个文件描述符的任务饿死。
