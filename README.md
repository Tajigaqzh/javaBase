# javaBase

## 项目说明

这是一个基于 Spring Boot 的示例项目，当前已经改为使用 `spring-boot-starter-data-jdbc` 连接 MySQL，并把连接池切换为 `Druid`。

当前数据库配置位于 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    type: com.alibaba.druid.pool.DruidDataSource
    url: jdbc:mysql://localhost:3306/javabase?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8
    username: javabase
    password: javabase123
    driver-class-name: com.mysql.cj.jdbc.Driver
```

## 1. 启动 MySQL 容器

项目根目录已经提供了 `docker-compose.yml`，直接执行：

```bash
docker compose up -d
```

当前启动的是 `docker-compose.yml` 里定义的这个服务：

```yaml
services:
  mysql:
    image: mysql:8.4
    container_name: javabase-mysql
    environment:
      MYSQL_ROOT_PASSWORD: root123456
      MYSQL_DATABASE: javabase
      MYSQL_USER: javabase
      MYSQL_PASSWORD: javabase123
    ports:
      - "3306:3306"
```

也可以只启动 MySQL 这一个服务：

```bash
docker compose up -d mysql
```

查看容器状态：

```bash
docker ps
```

查看 MySQL 启动日志：

```bash
docker compose logs -f mysql
```

如果需要停止容器：

```bash
docker compose down
```

如果需要连同数据卷一起删除：

```bash
docker compose down -v
```

## 2. MySQL 连接信息

- Host: `localhost`
- Port: `3306`
- Database: `javabase`
- Username: `javabase`
- Password: `javabase123`
- Root Password: `root123456`

## 3. MySQL 数据卷位置

当前不是把 MySQL 数据直接映射到项目目录，而是使用 Docker 命名卷：

```yaml
volumes:
  - mysql_data:/var/lib/mysql
```

含义：

- 容器内数据目录：`/var/lib/mysql`
- 宿主机侧：Docker 管理的命名卷 `mysql_data`

查看卷列表：

```bash
docker volume ls
```

查看卷的实际挂载位置：

```bash
docker volume inspect javaBase_mysql_data
```

## 4. 推荐的数据库连接工具

推荐优先级：

- `DataGrip`：功能最完整，适合长期开发。
- `DBeaver`：免费且稳定，适合大多数场景。
- `TablePlus`：界面轻量，macOS 下体验很好。

无论使用哪个工具，新增 MySQL 连接时填入上面的连接参数即可。

## 5. 项目连接数据库

项目已经配置为连接本机 Docker 中的 MySQL，因此只要容器启动成功，应用启动时就会自动尝试连接：

```bash
./mvnw spring-boot:run
```

或者运行测试验证 Spring Boot 是否能成功拿到数据库连接：

```bash
./mvnw clean test
```

说明：

- 如果 MySQL 没启动，测试或应用启动会报 `Connection refused`。
- 如果第一次拉取 MySQL 镜像较慢，等待镜像下载完成后再执行验证命令。

## 6. 当前依赖调整

`pom.xml` 已做如下调整：

- 使用 `spring-boot-starter-data-jdbc`
- 保留 `mysql-connector-j`
- 增加 `druid`
- 测试依赖改为 `spring-boot-starter-test`
- Java 版本调整为 `21`，与本机 JDK 对齐

## 7. 当前连接池

当前项目使用的是 `Druid` 连接池，而不是 Spring Boot 默认的 `HikariCP`。

切换方式：

- 在 `pom.xml` 中增加 `com.alibaba:druid`
- 在 `application.yml` 中指定 `spring.datasource.type=com.alibaba.druid.pool.DruidDataSource`

## 8. 常用命令

启动数据库：

```bash
docker compose up -d
```

查看日志：

```bash
docker compose logs -f mysql
```

启动项目：

```bash
./mvnw spring-boot:run
```

运行测试：

```bash
./mvnw clean test
```
