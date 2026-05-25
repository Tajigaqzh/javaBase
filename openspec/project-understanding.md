# 项目理解

## 1. 项目定位

这是一个基于 Spring Boot 4.0.6 和 Java 21 的示例工程，当前已经接入以下核心能力：

- Web MVC
- JDBC + Druid 数据源
- MySQL
- Redis
- Redisson
- MyBatis-Plus
- Sa-Token
- OpenAI Responses API 调用
- 自定义注解 + AOP 限流

项目整体仍处于基础能力集成阶段，已经具备认证、权限、数据库访问、Redis 访问和 AI 调用样例，但业务域相对简单，更多偏向一个可继续扩展的脚手架。

## 2. 当前代码结构

### `controller`

- `AuthController`：登录、登出、查询当前用户、查询 token 信息
- `DemoController`：Sa-Token 登录态和权限校验样例
Controller 层基本遵循“校验基础参数 + 返回统一响应”的约束，没有直接写数据库逻辑。

### `service`

- `AuthService`：登录登出、按登录态读取当前用户
- `OpenAiChatService`：封装对 OpenAI `/responses` 接口的调用
- `RedisCacheService`：Redisson 基础 KV 操作封装

目前 `AuthService` 直接围绕 `UserMapper` 和 `StpUtil` 组织认证流程，职责清晰但还比较轻量。

### `mapper` / `entity`

- `UserMapper` + `UserMapper.xml`
- `User`

当前用户体系很简单，登录逻辑依赖按用户名查询用户，再通过 Sa-Token 建立会话。

### `common`

- `response`：统一响应模型和响应码
- `handler`：全局异常处理
- `annotation`：`@RateLimit`
- `aspect`：`RateLimitAspect`
- `exception`：限流异常

这一层承担了项目的基础设施能力，已经形成了比较标准的 Spring 工程骨架。

### `config`

- `MybatisPlusConfig`：分页拦截器
- `RedissonConfig`：Redisson 配置
- `SaTokenPermissionConfig`：Sa-Token 权限/角色数据提供
- `OpenAiConfig` / `OpenAiProperties`：OpenAI 配置绑定

## 3. 当前关键链路

## 3.1 登录与鉴权链路

1. `POST /auth/login` 接收用户名。
2. `AuthService` 按用户名查询 `user` 表。
3. 用户存在则调用 `StpUtil.login(user.getId())` 建立登录态。
4. 返回用户信息和 token。
5. 需要登录态的接口通过 `@SaCheckLogin` 校验。
6. 需要权限的接口通过 `@SaCheckPermission` 校验。
7. `SaTokenPermissionConfig` 根据用户名简单返回角色和权限集合。

这说明当前权限模型是演示型实现，还没有独立的角色表、权限表和授权关系表。

## 3.2 AI 能力现状

当前仓库中仍保留 OpenAI 配置与服务封装，但 HTTP 暴露层已暂时移除，后续将按任务方式重新集成。

后续重新接入时，建议按如下链路补齐：

1. 提供受控的 AI 接口入口。
2. `OpenAiChatService` 读取 `OpenAiProperties`。
3. 使用 Spring `RestClient` 调用 OpenAI `POST /responses`。
4. 从响应体 `output -> content -> output_text` 中提取文本。
5. 返回受统一响应封装和权限控制保护的结果。

重新接入时应同步补齐以下能力：

- 登录态与权限控制
- 调用超时与异常映射
- 调用日志与审计
- 限流与配额控制
- 测试覆盖与接口文档

## 3.3 限流链路

1. 方法上标注 `@RateLimit`。
2. `RateLimitAspect` 通过 `@Around` 拦截方法调用。
3. 使用 SpEL 解析业务 key。
4. 基于 Redisson `RRateLimiter` 创建或复用限流器。
5. 根据业务 key 和 IP 决定最终限流维度。
6. 获取令牌失败时抛出 `RateLimitException`。

这个能力是典型的 Spring AOP 场景，因此依赖 Spring Boot 4 的专用 starter `spring-boot-starter-aspectj` 比单独显式引入 `aspectjweaver` 更符合项目语义。

## 4. 为什么这里更适合 Spring Boot 4 的 `spring-boot-starter-aspectj`

当前代码只使用了：

- `@Aspect`
- `@Around`
- Spring 容器管理的切面 Bean
- 运行时代理拦截 Spring Bean 方法

没有使用：

- AspectJ 编译期织入
- Load-Time Weaving
- `aop.xml`
- `ajc` 编译插件
- 对非 Spring 管理对象的织入

因此更合理的选择是 Spring Boot 4 的 `spring-boot-starter-aspectj`，原因如下：

- 语义更清晰：它表达的是“这是一个 Spring Boot AOP 项目能力”。
- 依赖更完整：starter 会把 Spring AOP 所需依赖按 Boot 管理好，不需要手动维护版本。
- 和自动配置一致：后续排查 AOP 是否生效时，starter 的行为更符合 Spring Boot 习惯。
- 降低误导：单独看到 `aspectjweaver` 容易让人误以为项目启用了 AspectJ 织入。

## 5. `aspectjweaver` 与 Spring Boot starter 的差异

### `aspectjweaver`

- 本质是 AspectJ 的织入器能力。
- 更偏向 AspectJ 体系本身。
- 适合需要 LTW/CTW 的场景。
- 单独引入并不等于 Spring Boot 已按推荐方式启用 AOP。

### `spring-boot-starter-aspectj`

- 是 Spring Boot 4 中面向 AOP/AspectJ 集成的启动器。
- 面向 Spring AOP 的常规项目集成。
- 由 Boot 管理依赖版本和自动配置。
- 适合绝大多数基于 `@Aspect` 的业务切面。

补充一点：

- 在较老的 Spring Boot 版本里，常见坐标是 `spring-boot-starter-aop`。
- 但当前项目实际使用的是 Spring Boot `4.0.6`，这一代受管理的对应坐标已经是 `spring-boot-starter-aspectj`。

## 6. 当前项目的明显改进点

- `AuthController#me` 直接返回 `User` 实体，后续最好改为专用 VO，避免实体直接暴露。
- `AuthService` 当前登录只校验用户名，不包含密码或更完整的认证因子。
- `SaTokenPermissionConfig` 现在是硬编码角色权限，后续应落库。
- `OpenAiChatController` 尚未增加登录和权限控制，理论上任何人都能直接调用。
- `OpenAiChatService` 缺少调用失败时的统一异常封装。
- 限流配置当前基于接口切面，但未体现针对 AI 接口的差异化配额策略。
- README 仍主要描述 JDBC/MySQL 集成，尚未覆盖 Sa-Token、Redis、Redisson、MyBatis-Plus 与 AI 能力。

## 7. 我对当前项目阶段的判断

这不是一个业务已经定型的系统，而是一个“基础设施已接通、业务还在搭骨架”的工程。当前最适合继续推进的方向是：

1. 先把认证、权限、限流、统一异常这条基础链路收紧。
2. 再按任务方式把 AI 接口纳入登录态和权限体系。
3. 最后再扩展更具体的业务模块，而不是继续只堆依赖。

## 8. 本次依赖调整结论

针对当前代码，`aspectjweaver` 可以安全替换为 Spring Boot 4 的 `spring-boot-starter-aspectj`。除非后续你明确要做手工控制的 AspectJ 编译期织入或类加载期织入，否则没有必要单独保留 `aspectjweaver` 作为项目表意依赖。
