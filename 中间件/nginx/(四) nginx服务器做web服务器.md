# nginx服务器做web服务器

## nginx 做静态服务器

HTML页面如下

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Title</title>
</head>
<body>

<h1>图片展示</h1>

<div>
    <img src="/static/images/1.png">
</div>
</body>
</html>
```

上传相关文件，生成如下路径

```
tree html/
html/
├── index.html
└── static
    └── images
        └── 1.png

```

```
## 配置nginx.conf 配置文件
worker_processes  1;
events {
    worker_connections  1024;
}
http {
    include       mime.types;
    default_type  application/octet-stream;
    sendfile        on;
    keepalive_timeout  65;
    server {
        listen       80;
        server_name  localhost;
        location / {
            root   html;
            index  index.html index.htm;
        }
        error_page   500 502 503 504  /50x.html;
        location = /50x.html {
            root   html;
        }
    }
}
```

```
/data/app/nginx/sbin/nginx -t 
nginx: the configuration file /data/app/nginx-1.10.3/conf/nginx.conf syntax is ok
nginx: configuration file /data/app/nginx-1.10.3/conf/nginx.conf test is successful
/data/app/nginx/sbin/nginx -s reload  
```

浏览器访问：

![1602226251653](assets/1602226251653.png)

这个时候我们可以把static静态页面给拆分出来

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
    server {
        listen       80;
        server_name  localhost;
        location / {
            root   html;
            index  index.html index.htm;
        }
        location /static/ {
            root /data/db;
        }
        error_page   500 502 503 504  /50x.html;
        location = /50x.html {
            root   html;
        }
    }
}
```

将静态文件迁移到/data/db目录下，并重启nginx服务。

```shell
mv html/static/ /data/db/
/data/app/nginx/sbin/nginx -t 
/data/app/nginx/sbin/nginx -s reload  
```

测试图片是否能否获取：

```shell
curl -I http://192.168.56.12/static/images/1.png
HTTP/1.1 200 OK
Server: nginx/1.10.3
Date: Sun, 08 Apr 2018 09:31:35 GMT
Content-Type: image/png
Content-Length: 32239
Last-Modified: Sun, 08 Apr 2018 09:21:26 GMT
Connection: keep-alive
ETag: "5ac9df16-7def"
Accept-Ranges: bytes
```

### 对图片开启gzip压缩

```
worker_processes  1;
events {
    worker_connections  1024;
}
http {
    include       mime.types;
    default_type  application/octet-stream;
    sendfile        on;

    gzip on;
    gzip_min_length 1k;
    gzip_buffers    4 16k;
    gzip_http_version 1.1;
    gzip_comp_level 6;
    gzip_types image/png;
    gzip_vary on;
    
    keepalive_timeout  65;
    server {
        listen       80;
        server_name  localhost;
        location / {
            root   html;
            index  index.html index.htm;
        }
        location /static/ {
            root /data/db;
        }
        error_page   500 502 503 504  /50x.html;
        location = /50x.html {
            root   html;
        }
    }
}
```

```
/data/app/nginx/sbin/nginx -t 
/data/app/nginx/sbin/nginx -s reload  
```

对比两次响应头信息，开启gzip 压缩后响应头多了`Content-Encoding: gzip`，开启压缩成功。

![](assets/802666-20180502135617649-1809737079.jpg)

![](assets/802666-20180502135634163-74509592.jpg)

