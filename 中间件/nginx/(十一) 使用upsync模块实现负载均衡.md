# 使用upsync模块实现负载均衡

### 问题描述

nginx reload是有一定损耗的，如果你使用的是长连接的话，那么当reload nginx时长连接所有的worker进程会进行优雅退出，并当该worker进程上的所有连接都释放时，进程才真正退出。

### 解决办法

对于社区版nginx目前有三个选择方式：

1. Tengine 的Dyups模块。
2. 微博的Upsync+Consul 实现动态负载均衡。
3. OpenResty的balancer_by_lua(又拍云使用其开源的slardar(Consul balancer_by_lua))。

本文使用upsync模块来解决配置文件修改后，reload nginx进程造成性能下降的问题。

它的功能是拉取 consul 的后端 server 的列表，并更新 Nginx 的路由信息。此模块不依赖于任何第三方模块。consul 作为 Nginx 的 db，利用 consul 的 KV 服务，每个 Nginx work 进程独立的去拉取各个 upstream 的配置，并更新各自的路由。

## 实战

### 给nginx打补丁包

这步可以不做，如果不做，编译的时候删除这个模块

```shell
git clone https://github.com/xiaokai-wang/nginx_upstream_check_module
## 打补丁包
patch -p0 < /usr/local/src/nginx_upstream_check_module-master/check_1.9.2+.patch 
```

下载nginx-upsync-module源码

```shell
git clone https://github.com/weibocom/nginx-upsync-module.git
下载nginx源码
wget 'http://nginx.org/download/nginx-1.10.1.tar.gz'
tar -xzvf nginx-1.10.1.tar.gz
cd nginx-1.10.1/
开始编译
./configure --prefix=/data/app/nginx-1.10.1 --user=nginx --group=nginx  --with-http_ssl_module  --with-http_stub_status_module   --add-module=/usr/local/src/nginx-upsync-module-master/ --add-module=/usr/local/src/nginx_upstream_check_module-master/
make
make install
```

## 启动consul

```shell
wget https://releases.hashicorp.com/consul/0.6.4/consul_0.6.4_linux_amd64.zip
unzip consul_0.6.4_linux_amd64.zip
./consul agent -advertise=x.x.x.x -client=0.0.0.0 -dev
```

创建nginx配置文件

```shell
mkdir -p /usr/local/nginx/conf/servers
/usr/local/nginx/conf/nginx.conf
events {
  worker_connections  4096;  ## Default: 1024
}

http {
    upstream test {
        # fake server otherwise ngx_http_upstream will report error when startup
        server 127.0.0.1:11111;

        # all backend server will pull from consul when startup and will delete fake server
        upsync 127.0.0.1:8500/v1/kv/upstreams/test upsync_timeout=6m upsync_interval=500ms upsync_type=consul strong_dependency=off;
        upsync_dump_path /usr/local/nginx/conf/servers/servers_test.conf;
    }

    upstream bar {
        server 127.0.0.1:8090 weight=1 fail_timeout=10 max_fails=3;
    }

    server {
        listen 8080;

        location = /proxy_test {
            proxy_pass http://test;
        }

        location = /bar {
            proxy_pass http://bar;
        }

        location = /upstream_show {
            upstream_show;
        }

    }
}
```

## 测试

```shell
for i in `seq 3`;do mkdir html$i/test -p && echo $i >html$i/test/test.html; done;  

docker run -d -p 8001:80 -v /root/html1/:/usr/share/nginx/html nginx 
docker run -d -p 8002:80 -v /root/html2/:/usr/share/nginx/html nginx 
docker run -d -p 8003:80 -v /root/html3/:/usr/share/nginx/html nginx 
```

## 添加服务

```shell
curl -X PUT -d '{"weight":1, "max_fails":2, "fail_timeout":10}' http://127.0.0.1:8500/v1/kv/upstreams/test/192.168.56.12:8001
curl -X PUT -d '{"weight":1, "max_fails":2, "fail_timeout":10}' http://127.0.0.1:8500/v1/kv/upstreams/test/192.168.56.12:8002
curl -X PUT -d '{"weight":1, "max_fails":2, "fail_timeout":10}' http://127.0.0.1:8500/v1/kv/upstreams/test/192.168.56.12:8003
```

查看conf/servers/servers_test.conf 文件中是否有内容

```shell
cat conf/servers/servers_test.conf 
server 192.168.56.12:8003 weight=1 max_fails=2 fail_timeout=10s;
server 192.168.56.12:8002 weight=1 max_fails=2 fail_timeout=10s;
server 192.168.56.12:8001 weight=1 max_fails=2 fail_timeout=10s;
```

或者浏览器打开`http://192.168.56.11:8080/upstream_show?test`
显示内容如下：

```shell
Upstream name: test; Backend server count: 3
        server 192.168.56.12:8003 weight=1 max_fails=2 fail_timeout=10s;
        server 192.168.56.12:8002 weight=1 max_fails=2 fail_timeout=10s;
        server 192.168.56.12:8001 weight=1 max_fails=2 fail_timeout=10s;
```

总结
此模块只修改upstream 中的缓存信息，不能修改或添加其他配置

测试中遇到的问题
在添加服务时出现如下错误，导致服务添加不能实时进行，大约需要3分钟左右时间。

consul日志：

```shell
2016/03/22 05:34:42 [DEBUG] http: Request GET /v1/kv/upstreams/test?recurse&index=169 (149.023µs) from=127.0.0.1:38853
    2016/03/22 05:34:43 [DEBUG] http: Request GET /v1/kv/upstreams/test?recurse&index=169 (146.759µs) from=127.0.0.1:38854
    2016/03/22 05:34:45 [DEBUG] http: Request GET /v1/kv/upstreams/test?recurse&index=169 (149.853µs) from=127.0.0.1:38855
    2016/03/22 05:34:46 [DEBUG] http: Request GET /v1/kv/upstreams/test?recurse&index=169 (111.46µs) from=127.0.0.1:38856
    2016/03/22 05:34:48 [DEBUG] http: Request GET /v1/kv/upstreams/test?recurse&index=169 (142.696µs) from=127.0.0.1:38857
    2016/03/22 05:34:48 [DEBUG] http: Request GET /v1/kv/upstreams/test?recurse&index=169 (112.089µs) from=127.0.0.1:38858
    2016/03/22 05:34:49 [DEBUG] http: Request GET /v1/kv/upstreams/test?recurse&index=169 (114.29µs) from=127.0.0.1:38859
    2016/03/22 05:34:50 [DEBUG] http: Request GET /v1/kv/upstreams/test?recurse&index=169 (148.245µs) from=127.0.0.1:38860
```

nginx日志

```shell
2016/03/22 05:35:09 [error] 18879#0: recv() failed (104: Connection reset by peer)
2016/03/22 05:35:09 [error] 18879#0: upsync_recv: recv error with upsync_server: 127.0.0.1:8500
2016/03/22 05:35:10 [error] 18879#0: recv() failed (104: Connection reset by peer)
2016/03/22 05:35:10 [error] 18879#0: upsync_recv: recv error with upsync_server: 127.0.0.1:8500
2016/03/22 05:35:11 [error] 18879#0: recv() failed (104: Connection reset by peer)
2016/03/22 05:35:11 [error] 18879#0: upsync_recv: recv error with upsync_server: 127.0.0.1:8500
2016/03/22 05:35:13 [error] 18879#0: recv() failed (104: Connection reset by peer)
2016/03/22 05:35:13 [error] 18879#0: upsync_recv: recv error with upsync_server: 127.0.0.1:8500
2016/03/22 05:35:13 [error] 18879#0: recv() failed (104: Connection reset by peer)
2016/03/22 05:35:13 [error] 18879#0: upsync_recv: recv error with upsync_server: 127.0.0.1:8500
2016/03/22 05:35:14 [error] 18879#0: recv() failed (104: Connection reset by peer)
```

问题现象
当添加一个服务时，出现此问题，新增服务不能及时添加到负载中，不影响运行正常的服务。 此时再往consul继续添加一个服务时，可能会导致此错误终止，并能成功添加当前两条服务记录。