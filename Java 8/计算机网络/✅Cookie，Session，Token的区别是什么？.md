# 典型回答

Cookie、Session和Token是用于在Web应用程序中管理用户状态和身份验证的技术。因为在Web应用中，HTTP的通信是无状态的，每个请求都是完全独立的，所以服务端无法确认当前访问者的身份信息，无法分辨上一次的请求发送者和这一次的发送者是不是同一个人。

Cookie是由服务器发送给用户浏览器的小型文本文件，存储在客户端的浏览器中。它会在浏览器下次向同一服务器再发起请求时被携带并发送到服务器上。服务器可以读取Cookie并使用其中的信息来进行识别和个性化处理。

每个 cookie 都会绑定单一的域名，无法在别的域名下获取使用，一级域名和二级域名之间是允许共享使用的。**cookie 是不可跨域的，并且每个域名下面的Cookie的数量也是有限的。**

Session是在服务器端创建和管理的一种会话机制。当用户首次访问网站时，服务器会为该用户创建一个唯一的Session ID，通常通过Cookie在客户端进行存储。会话标识符在后续的请求中用于标识具体是哪个用户。**通常情况下，session 是基于 cookie 实现的，session 存储在服务器端，sessionId 会被存储到客户端的cookie 中。**

![](https://cdn.nlark.com/yuque/0/2023/png/5378072/1687330339680-8119359b-a515-4be9-9d03-41d41a51728a.png#averageHue=%23f6f6f5&clientId=u98734613-78c9-4&from=paste&id=u964c3bc8&originHeight=442&originWidth=690&originalType=url&ratio=1&rotation=0&showTitle=false&status=done&style=none&taskId=u2364ee53-292e-4abf-a6ff-fab0aa4ccdc&title=)

Cookie 和 Session 的区别主要有以下几个

1. 存储位置不同： Session 是存储在服务器端的，Cookie 是存储在客户端的。
2. 安全性不同：因为Session存储在服务器端，所以Session 比 Cookie 安全。
3. 存取值的类型不同：Cookie 只支持存字符串数据，想要设置其他类型的数据，需要将其转换成字符串，Session 可以存任意数据类型。
4. 有效期不同： Cookie 可设置为长时间保持，比如我们经常使用的默认登录功能，Session 一般失效时间较短，客户端关闭（默认情况下）或者 Session 超时都会失效。
5. 存储大小不同： 单个 Cookie 保存的数据不能超过 4K，Session 可存储数据远高于 Cookie，但是当访问量过多，会占用过多的服务器资源。

有了cookie和session之后，基本可以实现用户的各种验证、鉴权及身份识别了。但是，**还是有一些场景中，不是特别适合这两种方案，或者说这两种方案的话还不够，那么就需要token上场了。**

> Token也是一种用于用户身份鉴权的手段。他其实是一种代表用户身份验证和授权的令牌。在Web应用程序中，常用的身份验证方案是基于令牌的身份验证（Token-based Authentication）。当用户成功登录时，服务器会生成一个Token并将其返回给客户端。客户端在后续的请求中将Token包含在请求头或请求参数中发送给服务器。服务器接收到Token后，会进行验证和解析，以确定用户的身份和权限。Token通常是基于某种加密算法生成的，因此具有一定的安全性。


主要由以下几个场景：

1、**跨域请求**：Cookie是不支持跨域的，当在不同的域名之间进行通信时，使用Token可以更方便地在跨域请求中传递身份验证信息，而不受Cookie限制。<br />2、**分布式场景**：Session是存储在服务器上的，但是随着现在很多都是集群部署，这就使得Session也需要实现分布式Session，而如果能用Token的话，就可以不用这么复杂。<br />[✅怎么实现分布式Session？](https://www.yuque.com/hollis666/fo22bm/xbgu80vgxnhhb438?view=doc_embed)<br />3、**API交互**：当我们使用浏览器访问后端服务的时候，可以用cookie和session，但是如果是API调用，比如Dubbo交互，就没办法做cookie的存储和传递了，而使用Token是常见的身份验证方式。客户端通过提供Token来证明其身份，并获得对受保护资源的访问权限。<br />4、**跨平台应用程序**：Token可以轻松地在不同的平台和设备之间共享和传递，而无需依赖特定的会话机制或Cookie支持。<br />5、**前后端分离项目**：现在很多项目都是前后端分离的了，这种项目中，前端和后端之间通过API的方式交互，这种的话用Token也会更加方便一些。

总之，因为Cookie和Session存在各种限制，所以Token也是目前常见的身份验证和状态管理方式，它具有更大的灵活性和适用性，特别适用于现代的应用程序架构和需求。它提供了一种无状态、可扩展和安全的身份验证和授权机制。

而且，有了token之后，还可以基于Token做防重检测。

[✅不用redis分布式锁， 如何防止用户重复点击？](https://www.yuque.com/hollis666/fo22bm/bg9usqc0763mw2wm?view=doc_embed)
# 扩展知识

## 不用Cookie如何实现Session

Cookie是实现Session的主要手段，但是有些场景中是不能使用的，比如有的浏览器可以禁用cookie，或者某些网站的cookie数量也有限制，而且跨域场景也不能用cookie 。所以，如果不能用cookie，那么改如何实现session呢？

首先就是可以将Session ID作为查询参数附加在URL中。例如，http://www.hollischuang.com/page?sessionid=hollis666。服务器会解析URL中的Session ID，并使用它来识别和恢复用户的会话状态。这种方式在某些情况下可以工作，但也存在一些安全性和隐私方面的考虑。

其次，也可以在页面的在表单中添加一个隐藏字段来存储Session ID。当用户提交表单时，会话标识符会随着请求一起发送到服务器。服务器接收到请求后，从隐藏字段中提取Session ID，并使用它来识别和恢复会话状态。这种方法通常用于Web应用程序中的表单提交和POST请求。


