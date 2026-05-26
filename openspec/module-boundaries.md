# 多模块边界说明

## 1. 目的

本文件用于明确当前项目多模块拆分后的职责边界，避免后续继续实现时再次把前台逻辑、后台逻辑和公共能力混在一起。

当前项目模块结构为：

- `javabase-utils`
- `javabase-domain`
- `javabase-frontend-app`
- `javabase-admin-app`

本文件属于仓库级约束文档，后续新增能力时，默认先按本文件判断应落在哪个模块。

## 2. 总体原则

当前阶段采用“前台优先、后台空壳、公共层收窄”的拆分策略。

明确原则如下：

1. 不为了“预留复用”提前把前台业务编排抽到公共层。
2. 公共模块只保留真正跨前后台可复用且不带业务语义的内容。
3. 认证流程、响应协议、前台 DTO/VO、前台 controller/service 默认属于 `javabase-frontend-app`。
4. 后台后续单独建设认证与权限体系，不复用前台认证编排。

## 3. 模块职责

### 3.1 `javabase-utils`

职责：

- 放前后台都能复用的纯工具能力
- 不带业务语义
- 不依赖前台协议模型

当前已放入：

- `ClientIpUtils`
- `RestClientUtils`

后续适合放入的内容：

- 时间工具
- 字符串工具
- 脱敏工具
- 通用校验工具
- 纯 HTTP/RPC 工具

不应放入：

- `BaseResponse`
- `ResponseCodeEnum`
- `ResponseUtils`
- 登录态封装
- 前台/后台专属 DTO、VO

原因：

- 这些内容带有明显协议语义或业务语义，不属于纯工具层。

### 3.2 `javabase-domain`

职责：

- 放实体模型
- 放 Mapper
- 放 Mapper XML
- 放真正通用的数据访问对象
- 放未来可能被前后台共同依赖的核心领域模型

当前已放入：

- `User`
- `UserMapper`
- `UserMapper.xml`

后续适合放入的内容：

- 登录日志实体与 Mapper
- 在线设备会话实体与 Mapper
- 角色、权限、用户角色关系实体与 Mapper

不应放入：

- 前台认证 service
- 后台认证 service
- 前后台 controller
- 前台返回 VO
- 后台管理 VO

原因：

- `domain` 负责数据和模型，不负责应用编排。

### 3.3 `javabase-frontend-app`

职责：

- 放当前前台应用的全部应用层能力
- 放前台控制器、服务、配置、鉴权、协议、限流、异常处理等内容

当前已放入：

- `AuthController`
- `DemoController`
- `AuthService`
- `OpenAiChatService`
- `RedisCacheService`
- `BaseResponse`
- `ResponseCodeEnum`
- `ResponseUtils`
- `SaTokenPermissionConfig`
- `OpenApiDocConfig`
- 前台 DTO / VO

后续默认继续放入：

- 前台登录注册接口
- 前台 AI 接口
- 前台设备会话接口
- 前台专用鉴权和限流逻辑

明确约束：

1. 前台登录流程只属于 `javabase-frontend-app`
2. 前台权限返回结构只属于 `javabase-frontend-app`
3. 前台响应协议默认不抽到公共层

### 3.4 `javabase-admin-app`

职责：

- 放后台管理应用
- 放后台独立的认证、权限、菜单、角色、操作审计等后台应用层能力

当前状态：

- 仅有空壳启动类
- 尚未实现后台认证与后台接口

后续默认放入：

- `AdminAuthController`
- `AdminAuthService`
- 后台专用 DTO / VO
- 后台菜单权限接口
- 后台用户管理接口

明确约束：

1. 后台认证流程后续单独设计
2. 后台不直接复用前台 `AuthService`
3. 后台不默认复用前台 `BaseResponse` 协议，是否共用以后再评估

## 4. 当前明确不抽公共的内容

以下内容当前阶段明确不进入公共模块：

- 前台登录流程
- 前台权限编排
- 前台返回结构
- 前台登录 DTO / VO
- `AuthService`
- `SaTokenPermissionConfig`

原因：

1. 这些内容都高度依赖前台场景。
2. 后台后续很可能有完全不同的登录方式、权限结构和返回协议。
3. 现在提前抽公共，只会让公共层变脏。

## 5. 后续新增需求时的归属判断规则

后续如果新增一个能力，可按以下顺序判断：

1. 它是不是纯工具？
   - 是：优先考虑 `javabase-utils`
2. 它是不是数据实体、Mapper、XML 或通用领域模型？
   - 是：优先考虑 `javabase-domain`
3. 它是不是前台应用编排、前台接口、前台协议？
   - 是：放 `javabase-frontend-app`
4. 它是不是后台管理应用编排、后台接口、后台协议？
   - 是：放 `javabase-admin-app`

如果一个能力暂时只被前台使用，即使未来可能复用，也不要提前抽公共。

## 6. 当前模块演进路线

### 第一阶段

- 持续完善 `javabase-frontend-app`
- 在 `javabase-domain` 中逐步补数据模型和数据访问
- 在 `javabase-utils` 中补纯工具
- `javabase-admin-app` 保持空壳或仅补最小骨架

### 第二阶段

- 单独建设后台认证和后台权限
- 将后台管理接口逐步放入 `javabase-admin-app`
- 评估是否存在真正可复用的后台/前台公共协议或领域服务

### 第三阶段

- 如果确实出现稳定、可复用、无前后台偏向的公共协议，再评估是否新增更细的公共模块

## 7. 当前结论

当前仓库的正式边界结论如下：

1. `utils` 只放纯工具
2. `domain` 只放数据模型和数据访问
3. 现有业务代码整体以 `frontend-app` 为主
4. `admin-app` 先空壳，后续单独生长
5. 前后台认证流程明确分离，不提前共用应用编排
