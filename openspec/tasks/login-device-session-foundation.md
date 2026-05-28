# 任务：登录设备标识、登录日志与会话管理基础方案

## 1. 背景

当前项目的登录流程仍然是演示级实现：

- `POST /auth/login` 只接收用户名
- `AuthService` 只按用户名查询用户并调用 `StpUtil.login`
- 没有设备维度
- 没有登录日志
- 没有按设备踢下线和多端会话管理能力

这套实现足够支撑最小样例，但不适合继续往真实前台业务推进。后续如果要支持：

- Web 和移动端多端登录
- 登录历史审计
- 按设备查看在线会话
- 强制下线
- 同端互斥
- 风控辅助判断

就必须先把“登录携带设备信息”和“登录日志落库”这两件事明确下来。

## 2. 目标

本阶段先沉淀一版可落地方案，不急着一次性把所有代码实现完。

要解决的问题包括：

- 登录时前端要传哪些设备相关字段
- 浏览器指纹、设备 ID、终端类型分别承担什么职责
- 服务端如何记录登录日志
- Sa-Token 的多端登录和踢人下线如何与设备信息结合
- 后续如何支持“查看我的登录设备”和“踢掉某台设备”

## 3. 关键结论

### 3.1 必须引入设备维度

登录时必须增加设备维度信息，不能继续只靠“用户名 + token”。

原因：

1. 登录日志如果没有设备标识，就只能记录“某人登录过”，无法回答“在哪台设备登录过”。
2. 会话管理如果没有设备标识，就很难支持“踢掉某个具体设备”。
3. 多端登录、同端互斥和风控识别都需要设备维度。

因此，本阶段明确要求：

- 登录时前端必须携带设备相关信息
- 服务端必须记录设备字段
- 会话层必须保留设备与 token 的关联关系

### 3.2 不把浏览器指纹当唯一可信主键

浏览器指纹不能直接当成唯一可信的设备主键。

原因：

1. Web 指纹受浏览器版本、插件、隐私策略和运行环境影响，稳定性不足。
2. 指纹存在被清理、漂移或伪造的可能。
3. 指纹本身更适合作为风控辅助特征，而不是主业务主键。

因此设备相关字段分工明确如下：

- `terminalType`：表示终端大类，例如 `WEB`、`ANDROID`、`IOS`
- `deviceId`：表示某个具体设备实例，由客户端生成并持久化
- `fingerprint`：表示浏览器或设备指纹，仅用于辅助识别和风控

结论：

- `deviceId` 是业务侧的设备实例标识
- `terminalType` 是 Sa-Token 端隔离的核心字段
- `fingerprint` 只做辅助信息，不承担唯一主键职责

### 3.3 登录请求建议新增字段

登录请求 DTO 建议至少新增以下字段：

- `deviceId`
- `terminalType`
- `fingerprint`
- `deviceName`

含义如下：

- `deviceId`
  - 必传
  - 客户端首次安装或首次访问时生成并持久化
  - 用于标识某个具体设备实例

- `terminalType`
  - 必传
  - 使用明确枚举值，例如 `WEB`、`ANDROID`、`IOS`
  - 用于区分 Web、安卓、iOS 等不同登录端

- `fingerprint`
  - Web 端建议可传
  - 移动端可按实际情况传设备指纹或摘要
  - 只做辅助记录和风控，不做强唯一

- `deviceName`
  - 可选
  - 用于展示友好的设备名称，例如“Mac Chrome”“iPhone 15 Pro”

### 3.4 服务端应补采集的登录上下文字段

除了客户端传入的设备信息，服务端还要补采集以下字段：

- `ip`
- `userAgent`
- `loginTime`
- `loginStatus`
- `failReason`

这样登录日志才能同时覆盖：

- 谁登录
- 从哪登录
- 用什么设备登录
- 是否成功
- 为什么失败

### 3.5 登录日志必须单独建表

登录日志不建议只打业务日志文件，必须落结构化表。

建议新增表：`user_login_log`

建议字段至少包括：

- `id`
- `user_id`
- `username`
- `terminal_type`
- `device_id`
- `fingerprint`
- `device_name`
- `ip`
- `user_agent`
- `login_status`
- `fail_reason`
- `token_value` 或 `token_last4`
- `login_time`
- `logout_time`
- `kickout_time`
- `kickout_reason`
- `create_time`
- `update_time`

补充说明：

1. `token_value` 是否完整保存，需要结合安全要求决定。
2. 如果担心完整 token 存储风险，可只保存：
- token 脱敏值
- token 后四位
- token 哈希值

### 3.6 Sa-Token 侧必须使用端信息

Sa-Token 的“设备端”能力应当启用，而不是只做默认登录。

本阶段方案约束如下：

1. 登录时必须带 `terminalType`
2. `terminalType` 应进入 Sa-Token 登录模型
3. 后续踢人、查询会话和同端互斥都要基于该字段

这样后续才能支持：

- Web 和 App 同时在线
- 只踢 Web，不踢 App
- 只踢某个终端类型
- 查询某个账号当前哪些端在线

### 3.7 设备信息在会话层的分工

设备维度建议分两层维护：

#### 第一层：Sa-Token 的“端”

用于解决：

- 多端共存
- 按端下线
- 同端互斥

核心字段：

- `terminalType`

#### 第二层：业务侧设备实例信息

用于解决：

- 查看当前账号登录过哪些具体设备
- 在同一终端下区分不同设备实例
- 精确踢掉某台设备

核心字段：

- `deviceId`
- `deviceName`
- `fingerprint`

因此后续会话管理不应只依赖 Sa-Token 默认 token 信息，还要在业务层维护 token 与设备实例的映射关系。

### 3.8 多端登录、同端互斥和踢人能力的关系

这三件事要一起设计，不能割裂：

#### 多端登录

同一账号允许多个终端类型共存，例如：

- `WEB`
- `ANDROID`
- `IOS`

#### 同端互斥

要明确同一终端类型下是否允许多设备同时在线。

例如：

- 同一账号允许一个 `WEB` 在线
- 但允许一个 `WEB` 和一个 `IOS` 同时在线

这个规则必须依赖：

- `terminalType`
- 必要时结合 `deviceId`

#### 踢人下线

至少要支持以下粒度：

- 按账号全踢
- 按终端类型踢
- 按具体设备踢

如果没有设备信息，最多只能做到“按账号踢”，无法做到真正可用的会话管理。

### 3.9 推荐的业务规则

本阶段建议先采用以下规则：

1. 同一账号允许多端登录
2. 不同终端类型之间默认可共存
3. 同一终端类型下采用“新登录覆盖旧登录”的策略
4. 后台管理或用户侧安全中心可对某个设备执行踢下线
5. 风险较高的设备可要求重新登录

当前阶段正式结论：

- 多端共存
- 同端互斥

用户侧设备管理补充结论：

1. 用户侧默认不允许踢掉当前正在使用的设备。
2. 用户侧支持“退出全部设备但保留当前设备”能力。
3. 后台管理侧允许踢掉任意设备，包括当前设备。

### 3.10 Web、Android、iOS 的设备标识策略

建议按终端分别处理：

#### Web

- `terminalType = WEB`
- `deviceId` 由前端生成并持久化到浏览器本地
- `fingerprint` 可选，用于辅助风控

#### Android / iOS

- `terminalType = ANDROID` / `IOS`
- `deviceId` 由 App 首次安装时生成并本地持久化
- `fingerprint` 可选，按端能力决定是否补充

明确一点：

- 无论 Web 还是移动端，都不要把易变的指纹直接当成唯一设备主键

## 4. 推荐数据结构

### 4.1 登录请求 DTO 建议

建议 `LoginRequest` 从“只传用户名”逐步演进为至少包含：

- `username`
- `deviceId`
- `terminalType`
- `fingerprint`
- `deviceName`

后续如果登录方式扩展为密码、验证码或第三方登录，再按登录类型继续扩充字段。

### 4.2 登录日志表建议

表名建议：

- `user_login_log`

建议补充索引：

- `idx_user_login_log_user_id`
- `idx_user_login_log_device_id`
- `idx_user_login_log_terminal_type`
- `idx_user_login_log_login_time`
- `idx_user_login_log_status`

如果后续存在“当前在线设备管理”高频查询，还可以再设计一张在线会话表，而不是只依赖日志表反查。

### 4.3 在线设备 / 会话映射表建议

如果后续要支持“我的登录设备列表”“踢掉某台设备”，建议增加一张会话映射表，例如：

- `user_session_device`

建议字段：

- `id`
- `user_id`
- `token_value` 或 `token_hash`
- `terminal_type`
- `device_id`
- `fingerprint`
- `device_name`
- `ip`
- `user_agent`
- `login_time`
- `last_active_time`
- `session_status`
- `logout_time`
- `kickout_time`
- `kickout_reason`

这样职责划分更清晰：

- `user_login_log`：记录历史事件
- `user_session_device`：记录当前或最近会话状态

## 5. 推荐实现顺序

建议按以下顺序推进，而不是一次性把所有能力混在一个提交里：

1. 扩展 `LoginRequest`
2. 增加终端类型枚举
3. 增加登录日志表
4. 登录流程补设备字段校验
5. 登录成功和失败都记录登录日志
6. 登录时把 `terminalType` 接入 Sa-Token
7. 新增当前登录设备查询接口
8. 新增按设备踢下线接口

## 6. 本阶段不建议做的事情

当前阶段不建议：

1. 把浏览器指纹当唯一登录主键
2. 只靠 user-agent 和 IP 识别设备
3. 不落库、只靠应用日志记录登录行为
4. 先做复杂风控评分，再回头补基础日志
5. 先写死“同端互斥”，不留策略开关

## 7. 对当前项目的直接影响

如果后续按本方案实现，当前项目至少会受以下影响：

### 7.1 接口层

- `LoginRequest` 需要增加设备字段
- 登录接口需要补参数校验

### 7.2 Service 层

- `AuthService` 需要改造登录编排
- 登录成功和失败都要记录日志
- 后续要支持按设备踢下线

### 7.3 Domain 层

- 新增登录日志实体、Mapper、XML
- 可能新增在线设备会话实体、Mapper、XML

### 7.4 配置与会话层

- Sa-Token 登录模型要带终端信息
- 会话信息要能映射回设备实例

## 8. 本次文档结论

当前阶段，登录流程必须尽快从“只看用户名”升级为“带设备维度的登录”。

明确结论如下：

1. 登录时要传 `deviceId` 和 `terminalType`
2. `fingerprint` 只做辅助记录，不做唯一可信主键
3. 登录日志必须落表
4. Sa-Token 的端信息必须接入
5. 后续踢人、多端登录、设备会话管理都建立在这套数据之上

只有先把这个底座落稳，后面再做安全中心、登录设备管理、同端互斥和风控，才不会返工。

## 9. 进一步实现草案

本节用于把上面的方案继续收敛成后续可直接编码的实现骨架，减少实现阶段的反复讨论。

### 9.1 数据库表结构草案

#### 9.1.1 登录日志表

建议表名：

- `user_login_log`

建议 DDL 草案如下：

```sql
CREATE TABLE `user_login_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT DEFAULT NULL COMMENT '用户ID，登录失败时可为空',
  `username` VARCHAR(64) NOT NULL COMMENT '登录时提交的用户名',
  `terminal_type` VARCHAR(32) NOT NULL COMMENT '终端类型，如 WEB/ANDROID/IOS',
  `device_id` VARCHAR(128) NOT NULL COMMENT '客户端设备实例ID',
  `fingerprint` VARCHAR(256) DEFAULT NULL COMMENT '浏览器或设备指纹摘要',
  `device_name` VARCHAR(128) DEFAULT NULL COMMENT '设备展示名称',
  `ip` VARCHAR(64) DEFAULT NULL COMMENT '登录IP',
  `user_agent` VARCHAR(512) DEFAULT NULL COMMENT '请求UA',
  `login_status` TINYINT NOT NULL COMMENT '登录状态：1成功 0失败',
  `fail_reason` VARCHAR(255) DEFAULT NULL COMMENT '失败原因',
  `token_hash` VARCHAR(128) DEFAULT NULL COMMENT 'token哈希或脱敏值',
  `login_time` DATETIME NOT NULL COMMENT '登录时间',
  `logout_time` DATETIME DEFAULT NULL COMMENT '主动退出时间',
  `kickout_time` DATETIME DEFAULT NULL COMMENT '被踢下线时间',
  `kickout_reason` VARCHAR(255) DEFAULT NULL COMMENT '踢下线原因',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_login_log_user_id` (`user_id`),
  KEY `idx_user_login_log_username` (`username`),
  KEY `idx_user_login_log_device_id` (`device_id`),
  KEY `idx_user_login_log_terminal_type` (`terminal_type`),
  KEY `idx_user_login_log_login_time` (`login_time`),
  KEY `idx_user_login_log_login_status` (`login_status`)
) COMMENT='用户登录日志表';
```

说明：

1. `user_id` 在登录失败时可能还未解析到用户，可为空。
2. `token_hash` 不建议直接保存原始 token，优先保存哈希值或脱敏值。
3. `username` 建议保留，即使后续支持手机号、邮箱或第三方登录，也能保留最初输入痕迹。

#### 9.1.2 在线设备会话表

建议表名：

- `user_session_device`

建议 DDL 草案如下：

```sql
CREATE TABLE `user_session_device` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `terminal_type` VARCHAR(32) NOT NULL COMMENT '终端类型，如 WEB/ANDROID/IOS',
  `device_id` VARCHAR(128) NOT NULL COMMENT '设备实例ID',
  `fingerprint` VARCHAR(256) DEFAULT NULL COMMENT '浏览器或设备指纹摘要',
  `device_name` VARCHAR(128) DEFAULT NULL COMMENT '设备展示名称',
  `token_hash` VARCHAR(128) NOT NULL COMMENT 'token哈希值',
  `ip` VARCHAR(64) DEFAULT NULL COMMENT '登录IP',
  `user_agent` VARCHAR(512) DEFAULT NULL COMMENT '请求UA',
  `session_status` TINYINT NOT NULL COMMENT '会话状态：1在线 2主动退出 3被踢下线 4失效',
  `login_time` DATETIME NOT NULL COMMENT '登录时间',
  `last_active_time` DATETIME DEFAULT NULL COMMENT '最近活跃时间',
  `logout_time` DATETIME DEFAULT NULL COMMENT '主动退出时间',
  `kickout_time` DATETIME DEFAULT NULL COMMENT '踢下线时间',
  `kickout_reason` VARCHAR(255) DEFAULT NULL COMMENT '踢下线原因',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_session_device_user_id` (`user_id`),
  KEY `idx_user_session_device_token_hash` (`token_hash`),
  KEY `idx_user_session_device_terminal_type` (`terminal_type`),
  KEY `idx_user_session_device_device_id` (`device_id`),
  KEY `idx_user_session_device_session_status` (`session_status`)
) COMMENT='用户在线设备会话表';
```

说明：

1. 这张表的目标是“当前或最近设备会话管理”，不是全量审计事件。
2. 如果后续要严格限制“同一账号同一设备只允许一个在线会话”，可以再增加唯一约束策略。

### 9.2 DTO 与枚举草案

#### 9.2.1 `LoginRequest` 建议新增字段

后续 `LoginRequest` 建议扩展为：

- `username`
- `deviceId`
- `terminalType`
- `fingerprint`
- `deviceName`

建议约束如下：

- `username`
  - `@NotBlank`
- `deviceId`
  - `@NotBlank`
  - `@Size(max = 128)`
- `terminalType`
  - `@NotNull`
- `fingerprint`
  - `@Size(max = 256)`
- `deviceName`
  - `@Size(max = 128)`

说明：

1. `deviceId` 本阶段建议必传，不要做“服务端帮你兜底生成”的隐式逻辑。
2. `terminalType` 要走明确枚举，不接受裸字符串随意写入。

#### 9.2.2 终端类型枚举草案

建议新增枚举：

- `TerminalTypeEnum`

建议取值：

- `WEB`
- `ANDROID`
- `IOS`

如后续有需要再扩展：

- `MINIAPP`
- `PAD`
- `DESKTOP`

实现建议：

1. 每个枚举值保留稳定 code。
2. 需要时增加显示名称。
3. DTO 输入不要直接用不受控字符串随意透传到数据库。

#### 9.2.3 登录状态枚举草案

建议新增：

- `LoginStatusEnum`

建议取值：

- `SUCCESS`
- `FAIL`

#### 9.2.4 会话状态枚举草案

建议新增：

- `SessionStatusEnum`

建议取值：

- `ONLINE`
- `LOGOUT`
- `KICKOUT`
- `EXPIRED`

### 9.3 `AuthService` 改造步骤草案

当前 `AuthService#login(String username)` 只有演示级登录逻辑，后续建议改造为显式接收登录上下文。

#### 9.3.1 推荐方法签名方向

建议后续演进为：

- `login(LoginRequest request, HttpServletRequest servletRequest)`

或者在 service 层收敛成一个内部 command 对象，例如：

- `login(LoginCommand command)`

其中 `LoginCommand` 统一包含：

- `username`
- `deviceId`
- `terminalType`
- `fingerprint`
- `deviceName`
- `ip`
- `userAgent`

这样 service 层不直接依赖 controller 参数拆装细节，更利于后续扩展多种登录方式。

#### 9.3.2 推荐执行步骤

`AuthService` 后续登录流程建议按以下顺序：

1. 校验用户名、设备 ID、终端类型等协议级参数
2. 查询用户
3. 如果用户不存在，记录失败登录日志
4. 如果用户存在但状态不允许登录，记录失败登录日志
5. 组装 Sa-Token 登录模型，将 `terminalType` 带入登录
6. 执行登录，获取 token
7. 保存或更新 `user_session_device`
8. 记录成功登录日志
9. 返回 `LoginUserVO`

#### 9.3.3 退出登录流程建议

`logout()` 后续也不应只调 `StpUtil.logout()`，还应补：

1. 从当前 token 定位在线设备会话
2. 更新会话状态为 `LOGOUT`
3. 回写 `logout_time`
4. 同步更新登录日志中的 `logout_time`
5. 最后销毁 Sa-Token 登录态

### 9.4 “按设备踢下线”接口草案

#### 9.4.1 用户侧接口

后续可提供：

- `POST /auth/session/kickout`

建议入参：

- `sessionId` 或 `deviceSessionId`

说明：

1. 用户只能踢自己的其他设备。
2. 默认不允许把当前正在使用的会话踢掉，或者需要显式确认。

#### 9.4.2 后台管理侧接口

后续后台模块可单独提供：

- `POST /admin/auth/session/kickout`

建议入参：

- `userId`
- `terminalType`（可选）
- `deviceId`（可选）
- `reason`

支持粒度：

- 踢某账号全部设备
- 踢某账号某终端
- 踢某账号某具体设备

#### 9.4.3 服务层处理步骤

按设备踢下线的推荐流程：

1. 根据会话表定位目标设备会话
2. 校验归属关系和权限
3. 调用 Sa-Token 对应下线能力
4. 更新 `user_session_device` 状态为 `KICKOUT`
5. 回写 `kickout_time`
6. 回写 `kickout_reason`
7. 同步更新登录日志

### 9.5 当前实现阶段建议

为了降低改造风险，建议分两批提交：

#### 第一批

- 扩展 `LoginRequest`
- 新增终端类型枚举
- 新增登录日志表
- 登录成功和失败都写日志

#### 第二批

- 增加在线设备会话表
- 接入 Sa-Token 端信息
- 提供当前设备列表接口
- 提供按设备踢下线接口

这样做的好处是：

1. 第一批先把审计信息补齐
2. 第二批再做更复杂的会话控制
3. 出问题时更容易回滚和定位

## 10. 后续实现时的注意点

1. 不要把 `fingerprint` 当唯一索引
2. 不要在 controller 中直接拼接登录日志写库逻辑
3. 不要把 token 明文直接长期保存到日志表
4. 登录失败也要记录日志，不能只记录成功
5. 后续如果加入密码、短信验证码或第三方登录，要保持同一套设备与日志模型

## 11. 本次追加结论

本任务后续实现时，建议明确采用以下最小落地路线：

1. 先扩展 `LoginRequest` 增加 `deviceId`、`terminalType`、`fingerprint`、`deviceName`
2. 先新增 `user_login_log`
3. 再把 `AuthService` 改成带设备上下文的登录编排
4. 第二阶段再新增 `user_session_device` 和按设备踢下线能力

这样实现节奏最稳，也最适合当前这个还在持续演进的前台认证模块。

## 12. 待办清单

以下事项当前仅记录为待办，不在本轮直接实现。

### 12.1 第一阶段待办

- 扩展 `LoginRequest`
  - 增加 `deviceId`
  - 增加 `terminalType`
  - 增加 `fingerprint`
  - 增加 `deviceName`
- 增加终端类型枚举
  - `WEB`
  - `ANDROID`
  - `IOS`
- 增加登录状态枚举
- 设计并落库 `user_login_log`
- 新增登录日志实体、Mapper、XML
- 在登录成功时记录登录日志
- 在登录失败时记录登录日志
- 补采集登录上下文
  - `ip`
  - `userAgent`
- 调整 `AuthService` 登录方法签名，支持设备上下文入参
- 调整登录接口参数校验和错误返回
- 增加手机号密码登录实现方案
- 增加手机号验证码登录实现方案
- 增加设置密码能力
- 增加忘记密码能力
- 增加修改密码能力

### 12.2 第二阶段待办

- 设计并落库 `user_session_device`
- 新增在线设备会话实体、Mapper、XML
- 登录成功后写入在线设备会话表
- 退出登录时回写会话状态和退出时间
- 接入 Sa-Token 端信息
- 提供“我的登录设备列表”接口
- 提供“踢掉某台设备”接口
- 提供“按终端类型踢下线”能力
- 提供后台管理侧强制下线能力

### 12.3 规则与策略待确认项

- `token` 在日志表中保存哈希、脱敏值还是后四位
- Web 端 `fingerprint` 是否必传
- `deviceName` 是否由前端直接传入，还是服务端辅助生成
- 后续是否增加 `MINIAPP`、`PAD`、`DESKTOP` 等终端类型
- 邮箱验证码在手机号注册链路中作为“补充验证”的具体触发场景
- “退出全部设备但保留当前设备”在接口层的参数表达方式

### 12.4 测试待办

- DTO 参数校验测试
- `AuthService` 登录成功日志测试
- `AuthService` 登录失败日志测试
- 退出登录回写日志测试
- 在线设备查询接口测试
- 按设备踢下线接口测试
