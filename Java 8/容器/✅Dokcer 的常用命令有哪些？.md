# 典型回答

Docker 有很多命令，这里列一下比较常用的：

**docker run**：启动一个新的容器

**docker ps**：列出正在运行的容器

**docker ps -a**：列出所有容器

**docker stop**： 停止正在运行的容器

**docker start**： 启动已停止的容器

**docker rm**：删除容器

**docker images**： 列出本地镜像

**docker pull**：从镜像仓库拉取或更新指定的镜像

**docker build**： 使用 Dockerfile 构建镜像

**docker logs **：查看日志

**docker logs -f <container-id or container-name>**：实时跟踪日志输出

**docker logs --tail 50 <container-id or container-name>**：查看最新的50行日志

**docker logs --since 30m <container-id or container-name>**：查看最近30分钟的日志
