# 典型回答
<br />

1. **URL解析**，对URL自动编码，然后检查长度，之后根据url查看浏览器是否缓存了该页面
2. **DNS**查询，依次通过浏览器缓存，OS hosts缓存，路由器缓存，ISP缓存和根域名服务器去查询对应的ip
3. 浏览器将请求封装为**HTTP**报文，在client和server建立连接之前，会进行**TCP三次握手**
4. 之后将报文从外到里封装为 以太网首部+ip首部+tcp首部+http首部经过**网关和路由器**发送给server
5. 对于淘宝来说，请求会先到**nginx服务器**上，然后nginx采用默认的轮询算法进行**负载均衡**，携带原来browser的ip把报文发送给**Servlet容器**
6. Servlet容器接收到请求之后会解析请求行，请求体，请求头，然后交给**MVC**处理
7. DispatcherServlet接收到请求后，通过请求路径返回相应的**拦截器和Controller；**
8. 在Controller中会进行业务逻辑的执行，可能会调用下层的Service以及持久层进行数据的CRUD。
9. 对Controller进行处理并返回ModelAndView；然后在通过ViewResolve对ModelAndView进行处理，返回View视图；最后一步是进行渲染View，产生response
10. 浏览器接收**response**，HTTP响应报文的头部包含了状态码（Status-Code），并进行**缓存和解码**
11. 浏览器**渲染页面**
