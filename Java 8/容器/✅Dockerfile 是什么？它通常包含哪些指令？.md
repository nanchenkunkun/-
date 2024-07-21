# 典型回答

Dockerfile 是一个文本文件，用于自动化地构建 Docker 镜像。Dockerfile 定义了从基础镜像开始，按步骤配置环境和部署应用所需要的所有命令。

如以下dockerfile：
```
# 使用 Java 11 官方镜像作为基础镜像
FROM openjdk:11-jdk

# 设置维护者信息
LABEL maintainer="hollis@hollis.com"

# 环境变量，可以设置默认的环境变量用于应用配置
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-Xmx512m -Xms256m"

# 设置容器内的工作目录
WORKDIR /app

# 将 jar 包复制到工作目录
COPY target/my-spring-boot-app.jar my-app.jar

# 指定对外暴露的端口号
EXPOSE 8080

# 容器健康检查，定期检查应用是否响应
HEALTHCHECK --interval=1m --timeout=3s \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# 使用 ENTRYPOINT 和 CMD 以提供默认执行命令，同时允许覆盖参数
ENTRYPOINT ["java", "-jar", "my-app.jar"]
CMD ["--server.port=8080"]

# 在构建或部署前执行额外的命令
ONBUILD RUN echo "Built a new image based on Java Spring Boot Application"

```

1.  **FROM** - 设置基础镜像，每个 Dockerfile 至少需要一条 FROM 指令作为镜像的基础。 
```dockerfile
FROM openjdk:11-jdk
```
 

2.   **LABEL** - 添加元数据到镜像，如作者、版本、描述等。 
```dockerfile
LABEL maintainer="hollis@hollis.com"
```
 

3. **ENV** - 设置环境变量。 
```dockerfile
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-Xmx512m -Xms256m"
```
** **

4. **WORKDIR** - 为 RUN, CMD, ENTRYPOINT, COPY 和 ADD 设置工作目录。 
```dockerfile
WORKDIR /app
```
** **

5. **ADD** 和 **COPY** - 将文件从构建环境复制到镜像。COPY 是推荐的方法，因为它更透明。ADD 除了复制本地文件还可以直接解压缩和从URL下载。 
```dockerfile
COPY target/my-spring-boot-app.jar /app/my-app.jar
```
** **

6.  **EXPOSE** - 声明容器运行时监听的端口。 
```dockerfile
EXPOSE 8080
```
** **

7.  **HEALTHCHECK** - 告诉 Docker 如何测试容器以检查它是否仍在运行。 
```dockerfile
HEALTHCHECK --interval=1m --timeout=3s \
  CMD curl -f http://localhost:8080/actuator/health || exit 1
```
** **

8.  **ENTRYPOINT** - 配置容器启动时运行的命令，允许将容器作为可执行文件。 
```dockerfile
ENTRYPOINT ["java", "-jar", "my-app.jar"]
```
**  **

9. **CMD** - 提供容器默认的执行命令。每个 Dockerfile 只能有一个 CMD 指令，如果列出多个，只有最后一个会生效。 
```dockerfile
CMD ["--server.port=8080"]
```
** **

10. **ONBUILD - 为镜像添加将在之后基于该镜像的 Dockerfile 中触发的触发器指令。 **
```dockerfile
ONBUILD RUN echo "Built a new image based on Java Spring Boot Application"
```
** **

11. **RUN** - 执行命令并创建新的镜像层，常用于安装软件包。 
```dockerfile
RUN echo "Built a new image based on Java Spring Boot Application"
```
 

12.  **VOLUME** - 创建挂载点来持久化数据或与其他容器共享数据。 
```dockerfile
VOLUME /var/cache/nginx
```
 

13.  **USER** - 设置运行容器时的用户名或 UID。 
```dockerfile
USER nginx
```
 

14.  **ARG** - 定义构建时的变量，可用于传递动态数据如代理服务器、版本标签等。 
```dockerfile
ARG VERSION=1.0
```

