# 典型回答

Docker Compose 是一个用于定义和运行多容器 Docker 应用程序的工具。通过使用一个 YAML 文件来配置应用服务，给予Docker Compose 我们就可以通过一个单独的命令来创建和启动所有服务。可以大大的简化配置环境的复杂度。

利用Docker Compose ，可以实现以下常见功能：

1. **服务定义**：可以在一个文件中定义一组相互关联的服务（如数据库、后端应用、前端应用等），这些服务将被同时管理。
2. **一键部署**：通过一个简单的命令 docker-compose up，可以同时启动或停止所有定义的服务。
3. **环境隔离**：每个项目可以使用单独的隔离环境，通过在不同的项目中使用不同的 Docker Compose 文件来实现。
4. **简化配置**：通过 YAML 文件配置服务，使得配置过程标准化且易于理解和维护。

如以下 docker-compose.yml

```dockerfile
version: '3'
services:
  web:
    image: nginx:latest
    ports:
      - "80:80"
    volumes:
      - ./webroot:/usr/share/nginx/html
    depends_on:
      - app

  app:
    build:
      context: ./app
      dockerfile: Dockerfile
    environment:
      - DEBUG=false
    depends_on:
      - db

  db:
    image: postgres:latest
    environment:
      - POSTGRES_USER=exampleuser
      - POSTGRES_PASSWORD=examplepass
    volumes:
      - db-data:/var/lib/postgresql/data

volumes:
  db-data:
```
