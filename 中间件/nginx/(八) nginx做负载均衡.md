# 使用nginx做负载均衡

使用nginx做负载均衡的两大模块：

- upstream 定义负载节点池。
- location 模块 进行URL匹配。
- proxy模块 发送请求给upstream定义的节点池。



## upstream模块解读

nginx 的负载均衡功能依赖于 ngx_http_upstream_module模块，所支持的代理方式有 proxy_pass(一般用于反向代理),fastcgi_pass(一般用于和动态程序交互),memcached_pass,proxy_next_upstream,fastcgi_next_pass,memcached_next_pass 。

upstream 模块应该放于http{}标签内。



模块写法：

```
upstream backend {
    ip_hash; 
    server backend1.example.com       weight=5;
    server backend2.example.com:8080;
    server backup1.example.com:8080   backup;
    server backup2.example.com:8080   backup;
}
```

实例一：

```
upstream dynamic {
    zone upstream_dynamic 64k;

    server backend1.example.com      weight=5;
    server backend2.example.com:8080 fail_timeout=5s slow_start=30s;
    server 192.0.2.1                 max_fails=3;
    server backend3.example.com      resolve;

    server backup1.example.com:8080  backup;
    server backup2.example.com:8080  backup;
}
```

语法解释：

### nginx默认支持四种调度算法

- 轮询(rr),每个请求按时间顺序逐一分配到不同的后端服务器，如果后端服务器故障，故障系统自动清除，使用户访问不受影响。
- 轮询权值(weight),weight值越大，分配到的访问几率越高，主要用于后端每个服务器性能不均的情况。
- ip_hash，每个请求按访问IP的hash结果分配，这样来自同一个IP的固定访问一个后端服务器，主要解决动态网站session共享的问题。
- url_hash，按照访问的URL的hash结果来分配请求，是每个URL定向到同一个后端服务器，可以进一步提高后端缓存服务器的效率，nginx本身不支持，如果想使用需要安装nginx的hash软件包。
- fair，这个算法可以依据页面大小和加载时间长短智能的进行负载均衡，也就是根据后端服务器的响应时间来分配请求，相应时间短的优先分配，默认不支持，如果想使用需要安装upstream_fail模块。
- least_conn 最少链接数，那个机器连接数少就分发。



### server模块的写法

```
server IP 调度状态
```

server指令指定后端服务器IP地址和端口，同时还可以设定每个后端服务器在负载均衡调度中的状态。

- down 表示当前的server暂时不参与负载均衡。
- backup 预留的备份服务器，当其他所有的非backup服务器出现故障或者忙的时候，才会请求backup机器，因为这台集群的压力最小。
- max_fails 允许请求失败的次数，默认是1，当超过最大次数时，返回proxy_next_upstream模块定义的错误。0表示禁止失败尝试，企业场景：2-3.京东1次，蓝汛10次，根据业务需求去配置。
- fail_timeout，在经历了max_fails次失败后，暂停服务的时间。京东是3s，蓝汛是3s，根据业务需求配置。常规业务2-3秒合理。

例：如果max_fails是5，他就检测5次，如果五次都是502.那么，他就会根据fail_timeout 的值，等待10秒，再去检测。

> server 如果接域名，需要内网有DNS服务器，或者在负载均衡器的hosts文件做域名解析。server后面还可以直接接IP或IP加端口。

### 长连接 keepalive

```
upstream backend {
    server backend2.example.com:8080;
    server backup1.example.com:8080   backup;
    keepalive 100;
}
```

通过该指令配置了每个worker进程与上游服务器可缓存的空闲连接的最大数量。
当超出这个数量时，最近最少使用的连接将被关闭。keepalive指令不限制worker进程与上游服务器的总连接。

```
location / {
    # 支持keep-alive
    proxy_http_version 1.1;
    proxy_set_header Connection "";
    proxy_pass http://backup;
}
```

- 如果是http/1.0 需要配置发送"Connection: Keep-Alive" 请求头。
- 上游服务器不要忘记开启长连接支持。

连接池配置建议

- 总长连接数是"空闲连接池"+"释放连接池"的长连接总数。
- 首先，长连接配置不会限制worker进程可以打开的总连接数（超了的作为短连接）。另外连接池一定要根据场景合理进行设置。

1. 空闲连接池太小，连接不够用，需要不断建连接。
2. 空闲连接池太大，空闲连接太多，还没使用就超时。
3. 建议只对小报文开启长连接。



## location 模块解读

location作用：基于一个指令设置URI。
基本语法：

```shell
Syntax:	location [ = | ~ | ~* | ^~ ] uri { ... }
location @name { ... }
Default:	—
Context:	server, location
```

- `=` 精确匹配，如果找到匹配=号的内容，立即停止搜索，并立即处理请求(优先级最高)
- `~` 区分大小写
- `~*` 不区分大小写
- `^~`	只匹配字符串，不匹配正则表达式
- `@`	指定一个命名的location，一般用于内部重定义请求，location @name {…}

**匹配是有优先级的，不是按照nginx的配置文件进行。**

```
官方的例子：

location = / {
    [ configuration A ]
}
location / {
    [ configuration B ]
}
location /documents/ {
    [ configuration C ]
}
location ^~ /images/ {
    [ configuration D ]
}
location ~* \.(gif|jpg|jpeg)$ {
    [ configuration E ]
}
```

结论：

- `/` 匹配A。
- `/index.html` 匹配B
- `/documents/document.html` 匹配C
- `/images/1.gif` 匹配D
- `/documents/1.jpg` 匹配的是E。

测试用的例子：

```shell
location / {
           return 401;
        }
        location = / {
            return 402;
        }
        location /documents/ {
            return 403;
        }
        location ^~ /images/ {
            return 404;
        }
        location ~* \.(gif|jpg|jpeg)$ {
            return 500;
        }
```

测试结果（重点看）：

```shell
[root@lb01 conf]# curl -I -s -o /dev/null -w "%{http_code}\n" http://10.0.0.7/
402
[root@lb01 conf]# curl -I -s -o /dev/null -w "%{http_code}\n" http://10.0.0.7/index.html
401
[root@lb01 conf]# curl -I -s -o /dev/null -w "%{http_code}\n" http://10.0.0.7/documents/document.html 
403
[root@lb01 conf]# curl -I -s -o /dev/null -w "%{http_code}\n" http://10.0.0.7/images/1.gif
404
[root@lb01 conf]# curl -I -s -o /dev/null -w "%{http_code}\n" http://10.0.0.7/dddd/1.gif  
500
```

结果总结：
匹配的优先顺序，`=`>`^~`（匹配固定字符串，忽略正则）>`完全相等`>`~*`>`空`>`/` 。
工作中尽量将'='放在前面

## proxy_pass 模块解读

proxy_pass 指令属于ngx_http_proxy_module 模块，此模块可以将请求转发到另一台服务器。

写法：

```shell
proxy_pass http://localhost:8000/uri/; 
```

实例一：

```
    upstream blog_real_servers {
         server 10.0.0.9 :80  weight=5;
         server 10.0.0.10:80  weight=10;
         server 10.0.0.19:82  weight=15;
    }
    server {
       listen       80;
       server_name  blog.etiantian.org;
       location / {
        proxy_pass http://blog_real_servers;
        proxy_set_header host $host;
       }
    }
```

- proxy_set_header：当后端Web服务器上也配置有多个虚拟主机时，需要用该Header来区分反向代理哪个主机名，`proxy_set_header host $host;`。
- proxy_set_header X-Forwarded-For ：如果后端Web服务器上的程序需要获取用户IP，从该Header头获取。`proxy_set_header X-Forwarded-For $remote_addr;`



### 配置后端服务器接收前端真实IP

配置如下：

```shell
    log_format  commonlog  '$remote_addr - $remote_user [$time_local] "$request" '
                      '$status $body_bytes_sent "$http_referer" '
                      '"$http_user_agent" "$http_x_forwarded_for"';
```

rs_apache节点的httpd.conf配置

```shell
LogFormat "\"%{X-Forwarded-For}i\" %l %u %t \"%r\" %>s %b \"%{Referer}i\" \"%{U
ser-Agent}i\"" combined修改日志记录
apache
LogFormat "\"%{X-Forwarded-For}i\" %l %u %t \"%r\" %>s %b" common
```

### proxy_pass相关的优化参数

- `client_max_body_size 10m;` 允许客户端请求的最大的单个文件字节数。
- `client_body_buffer_size 128k;` 缓冲区代理缓冲用户端请求的最大字节数 可以理解为先保存到本地再传给用户。
- `proxy_connect_timeout 600;` 跟后端服务器连接的超时时间_发起握手等候响应超时时间。
- `proxy_read_timeout 600;` 连接成功后_等候后端服务器响应时间_其实已经进入后端的排队之中等候处理。
- `proxy_send_timeout 600;` 后端服务器回传数据时间,就是在规定时间之内后端服务器必须传完所有的数据。
- `proxy_buffer_size 8k;` 代理请求缓存区，这个缓存区间会保存用户的头信息以供Nginx进行规则处理,一般只要设置能保存下头信息即可。
- `proxy_buffers 4 32k;` 同上 告诉Nginx保存单个页面使用的空间大小，假设网页大小平均在32k以下的话。
- `proxy_busy_buffers_size 64k;` 如果系统很忙的时候可以申请更大的proxy_buffers 官方推荐(proxy_buffers*2)。
- `proxy_max_temp_file_size 1024m;` 当 proxy_buffers 放不下后端服务器的响应内容时，会将一部分保存到硬盘的临时文件中，这个值用来设置最大临时文件大小，默认1024M，它与 proxy_cache 没有关系。大于这个值，将从upstream服务器传回。设置为0禁用。
- `proxy_temp_file_write_size 64k;` proxy缓存临时文件的大小 proxy_temp_path（可以在编译的时候）指定写到哪那个目录。



## 健康检查

Nginx提供了health_check语句来提供负载（upstream）时的键康检查机制（注意：此语句需要设置在location上下文中）。

支持的参数有：

- interval=time：设置两次健康检查之间的间隔值，默认为5秒
- fails=number：设置将服务器视为不健康的连续检查次数，默认为1次
- passes=number：设置一个服务器被视为健康的连续检查次数，默认为1次
- uri=uri：定义健康检查的请求URI，默认为”/“
- match=name：指定匹配配置块的名字，用记测试响应是否通过健康检测。默认为测试返回状态码为2xx和3xx

一个简单的设置如下，将使用默认值：

```shell
location / {
    proxy_pass http://backend;
    health_check;
}
```

对就应用，我们可以专门定义一个API用于健康检查：/api/health_check，并只返回HTTP状态码为200。并设置两次检查之间的间隔值为1秒。这样，health_check语句的配置如下：

```shell
health_check uri="/api/health_check" interval;
```

匹配match的方法

```shell
http {
    server {
    ...
        location / {
            proxy_pass http://backend;
            health_check match=welcome;
        }
    }

    match welcome {
        status 200;
        header Content-Type = text/html;
        body ~ "Welcome to nginx!";
    }
}
```

match 例子举例

- `status 200;`: status 等于 200
- `status ! 500;`: status 不是 500
- `status 200 204;`: status 是 200 或 204
- `status ! 301 302;`: status 不是301或302。
- `status 200-399;`: status 在 200 到 399之间。
- `status ! 400-599;`: status 不在 400 到 599之间。
- `status 301-303 307;`: status 是 301, 302, 303, 或 307。
- `header Content-Type = text/html;`: “Content-Type” 得值是 text/html。
- `header Content-Type != text/html;`: “Content-Type” 不是 text/html。
- `header Connection ~ close;`: “Connection” 包含 close。
- `header Connection !~ close;`: “Connection” 不包含 close。
- `header Host;`: 请求头包含 “Host”。
- `header ! X-Accel-Redirect;`: 请求头不包含 “X-Accel-Redirect”。
- `body ~ "Welcome to nginx!";`: body 包含 “Welcome to nginx!”。
- `body !~ "Welcome to nginx!";`: body 不包含 “Welcome to nginx!”。

## 一个完整的nginx实例

```
worker_processes  1;
events {
    worker_connections  1024;
}
http {
    include       mime.types;
    default_type  application/octet-stream;
    sendfile        on;
    keepalive_timeout  65;
    #blog lb by oldboy at 201303
    upstream blog_real_servers {
   server   10.0.0.9:80 weight=1 max_fails=1 fail_timeout=10s;
   server   10.0.0.10:80 weight=1 max_fails=2 fail_timeout=20s;

    }
    server {
       listen       80;
       server_name  blog.etiantian.org;
       location / {
        proxy_pass http://blog_real_servers;
        include proxy.conf;
       }
    }
}
```

```
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $remote_addr;
        proxy_connect_timeout 90;        
        proxy_send_timeout 90;
        proxy_read_timeout 90;
        proxy_buffer_size 4k;
        proxy_buffers 4 32k;
        proxy_busy_buffers_size 64k; proxy_temp_file_write_size 64k;
```

