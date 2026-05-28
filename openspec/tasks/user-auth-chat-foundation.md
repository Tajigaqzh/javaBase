# 任务：用户体系、认证体系与聊天业务基础方案

## 1. 背景

当前项目只有一个简化版 `user` 表，认证流程也仅支持“演示账号直接登录”的简单实现，无法满足真实办公聊天系统的用户、权限、注册登录和后续扩展需求。

目标产品方向接近“飞书式办公聊天系统”，因此需要优先把以下底座设计清楚：

- 用户主表
- 用户等级与权限体系
- 注册与登录体系
- 账号绑定关系
- 会话登录态方案
- 后续聊天、群组、文件、音视频能力可复用的用户域基础

## 2. 目标

本阶段不急着一次性做完所有聊天业务，而是先沉淀一版可落地的基础方案，确保后续实现时不会在用户体系上返工。

本阶段要解决的问题：

- 如何重构用户表
- 如何设计用户等级与权限表
- 是否使用 JWT
- 如何支持多种注册登录方式
- 如何保证“账号必须绑定手机号”
- 登录后如何返回等级、角色和权限
- 如何为后续单聊、群聊、文件、音视频留出演进空间

## 3. 关键结论

### 3.1 登录态方案

当前项目已接入 `Sa-Token`，本阶段建议继续以 `Sa-Token` 作为主登录态方案，不建议此时切换为纯 JWT。

原因：

1. 项目已经完成 Sa-Token 接入，切换成本高。
2. 后续办公聊天系统通常需要：
- 多端登录
- 强制下线
- 会话续期
- 权限动态收回
- 设备级会话管理

这些能力用 Sa-Token 更直接。

3. JWT 更适合作为：
- 开放平台 Access Token
- 网关透传凭证
- 与第三方系统交互的短时签名令牌

而不是当前内部业务登录态的首选。

### 3.2 注册登录接口策略

建议采用“多入口注册/登录接口 + 统一 service 认证编排”的设计。

也就是：

- Controller 层可以拆多个接口，便于协议清晰、参数清晰
- Service 层统一沉淀登录注册主流程，避免重复逻辑

推荐接口示例：

#### 注册接口

- `POST /auth/register/phone`
- `POST /auth/register/email`
- `POST /auth/register/qq`

#### 登录接口

- `POST /auth/login/password`
- `POST /auth/login/phone-code`
- `POST /auth/login/email-code`
- `POST /auth/login/qq`

#### 密码相关接口

- `POST /auth/password/set`
- `POST /auth/password/forgot/reset`
- `POST /auth/password/change`

如果希望对前端暴露一个统一入口，也可以在上层补：

- `POST /auth/login`

通过 `loginType` 分流，但 service 内部仍应走统一认证编排。

### 3.3 手机绑定要求

“账号必须绑定手机号”建议拆成两层约束：

1. 注册完成后，账号进入可登录前，必须具备已验证手机号。
2. 对 QQ 第三方登录等场景，如果第三方身份首次落库时没有手机号，允许先创建“待补全账号”，但必须在后续绑定手机号后才开放完整能力。

这样可以兼顾第三方登录体验与业务安全要求。

### 3.4 用户身份标识规则

当前方案明确采用“手机号为主身份标识”的设计。

规则如下：

1. `phone` 是用户主身份标识，必须唯一。
2. 所有账号最终都必须绑定手机号。
3. `user_no` 作为系统内部稳定业务编号，不可修改，可用于内部业务查询和跨模块关联。
4. `email` 作为辅助登录字段和联系字段，不取代手机号主地位。
5. 第三方账号首次登录如果没有手机号，必须进入“待绑定手机号”状态，绑定完成后才能视为完整账号。

建议补充约束：

- `phone` 必须唯一
- `user_no` 必须唯一
- `email` 可选，但若存在则必须唯一

推荐登录策略：

- 手机号注册：直接主流程
- 账号密码登录：以 `手机号 + 密码` 为主，不依赖展示昵称作为登录主键
- 手机验证码登录：作为手机号登录的并行主流程，适合移动端和找回场景
- 邮箱注册：允许，但注册完成前或首次登录前必须补绑定手机号
- QQ 登录：允许，但首次登录必须绑定手机号后才算完整账号

### 3.4.1 第一阶段登录方式结论

第一阶段真正上线的登录方式明确如下：

- 必须上线 `手机号登录`
- 第一阶段同时支持：
- `手机号 + 密码登录`
- `手机号 + 验证码登录`

原因：

1. 手机号是当前主身份标识，必须具备完整主登录能力。
2. 只做验证码登录，不利于后续密码体系、安全中心和后台风控闭环。
3. 只做密码登录，又不利于移动端体验和验证码找回链路。

因此第一阶段认证入口的正式结论是：

- 手机号注册
- 手机号密码登录
- 手机号验证码登录

第一阶段注册链路补充结论：

1. 手机号注册必须完成短信验证码校验。
2. 邮箱验证可以作为补充验证方式存在。
3. 后台允许创建用户后再补激活。

后台创建用户后的补激活流程结论：

1. 后台创建用户后，默认进入 `PENDING_COMPLETE` 状态。
2. 用户首次进入激活流程时，必须完成设密。
3. 激活过程可结合短信验证码或邮箱验证码完成身份确认。
4. 完成激活、设密和必要绑定后，账号状态再转为 `NORMAL`。

### 3.4.2 第一阶段密码能力结论

既然第一阶段上线密码登录，则密码相关能力也必须一起进入第一阶段范围。

第一阶段明确包含：

- 设置密码
- 忘记密码
- 修改密码

建议对应接口如下：

- `POST /auth/password/set`
- `POST /auth/password/forgot/reset`
- `POST /auth/password/change`

三者职责边界如下：

#### 设置密码

适用于：

- 首次设置密码
- 验证码校验后的补设密码
- 尚未设置密码账号的补全流程

#### 忘记密码

适用于：

- 未登录状态下
- 通过手机号验证码重置密码

#### 修改密码

适用于：

- 已登录状态下
- 校验旧密码后修改密码

规则建议先明确如下：

1. 忘记密码成功后，当前账号所有设备立即失效，要求重新登录。
2. 修改密码成功后，当前账号所有设备立即失效，要求重新登录。
3. 设置密码、忘记密码、修改密码都应记录安全审计日志。
4. 新密码不应与最近 3 次历史密码相同。
5. 忘记密码支持短信验证码或邮箱验证码完成身份校验。
6. 修改密码必须校验旧密码。

### 3.5 账号状态与登录规则

建议定义以下账号状态：

- 正常
- 待补全
- 禁用
- 冻结
- 注销

登录规则明确如下：

- 正常：允许登录，允许正常使用系统功能
- 待补全：允许登录，但只能进入资料补全流程，补全前不能使用核心业务功能
- 禁用：不允许登录
- 冻结：不允许登录
- 注销：不允许登录

补充约束：

- 待补全账号登录后，后端应明确返回状态标识，前端据此强制进入补全流程
- 待补全账号默认只开放极少量权限，例如查看本人资料、补全手机号、补全邮箱、退出登录
- 冻结通常用于风控或异常登录场景，允许用户通过手机号验证码自助解冻
- 禁用通常用于后台管理封禁，允许设置封禁时长或永久封禁，到期自动解封或由后台手动解封
- 注销通常是用户主动发起，后续若用户尝试重新登录或重新注册原手机号，应优先进入“恢复原账号”流程，而不是创建新账号

### 3.6 逻辑删除与账号恢复规则

`is_delete = 1` 后，账号允许恢复，但不允许将原手机号释放给新账号。

规则如下：

1. 逻辑删除账号允许恢复。
2. 恢复账号时必须做验证码校验，优先校验绑定手机号验证码。
3. 已逻辑删除账号的原手机号不允许被重新注册新账号。
4. 恢复账号时应保留原用户 ID，不应新建账号。
5. 删除后的账号本质上进入“可恢复但不可登录”的状态，而不是彻底销毁。
6. 注销恢复后，保留原用户 ID，并恢复账号主体与基础资料。
7. 注销恢复后，历史聊天数据、历史协作痕迹、历史消息引用应保留，以避免影响其他用户和群组的历史完整性。
8. 注销恢复后，不自动恢复订阅权益、高风险后台角色、特殊业务权限等敏感状态。
9. 群成员关系、好友关系、业务资格等是否自动恢复，可在后续详细设计阶段继续细化。
10. 恢复账号采用单独接口，不在普通登录接口中直接隐式恢复。
11. 当用户使用已注销但可恢复的手机号尝试登录时，系统先返回“账号可恢复”状态，由前端弹框提示用户确认是否恢复。
12. 用户确认后，再调用单独的恢复账号接口，完成验证码校验、状态恢复和新的登录态建立。

这样设计的原因是：

- 聊天和协作系统中的历史消息、群发言、文件记录往往不只属于单个用户自己，也会影响其他用户的会话视图。
- 如果简单定义为“恢复后原数据完全不恢复”，会让恢复账号的意义大幅降低，也不符合多数聊天协作产品对历史数据连续性的处理方式。
- 更合理的方式是：恢复账号主体与历史数据连续性，但不自动恢复可能带来安全、权限或付费争议的敏感状态。

### 3.7 用户信息修改边界

当前先明确以下边界：

- 手机号：一旦注册成功，用户自己不能修改
- 昵称：允许用户自己修改，且允许重复
- 邮箱：允许用户自己修改

后续如支持手机号换绑，应单独设计高安全级别流程，而不是在普通资料修改中直接开放。

换绑验证规则先明确如下：

- 邮箱换绑：必须进行验证码校验
- 手机号换绑：必须同时完成两步校验
- 原手机号接收并校验指定验证码
- 新手机号接收并校验验证码

也就是说，手机号换绑不能只验证新手机号，必须同时证明用户仍控制旧手机号与新手机号。

关于用户展示字段与内部稳定标识，明确如下：

1. `nickname` 是展示字段，可随时修改
2. `nickname` 不承担登录标识职责
3. `nickname` 不作为稳定业务主键
4. `user_no` 作为内部稳定业务编号，不可修改
5. 关系数据、审计数据、聊天数据、文件归属、会话归属应依赖 `user_id` 或 `user_no`
6. `nickname` 允许重复，不作为唯一约束字段

### 3.8 QQ 绑定关系规则

QQ 绑定关系明确采用一对一模型：

- 一个 QQ 账号只能绑定一个系统用户
- 一个系统用户也只能绑定一个 QQ 账号

约束含义：

1. `provider = qq` 与 `provider_user_id` 的组合必须唯一
2. 同一个 `user_id` 在 `provider = qq` 下只能存在一条绑定记录

这样可以避免：

- 一个 QQ 串多个系统用户
- 一个用户绑定多个 QQ
- 后续登录、解绑、审计关系混乱

### 3.9 权限模型结论

当前权限模型明确采用标准 `RBAC`。

核心实体与关系如下：

- User
- Role
- Permission
- UserRole
- RolePermission

明确约束：

1. 用户不直接绑定权限。
2. 所有权限通过角色授予给用户。
3. 角色用于控制“能不能做”。
4. 权限是系统功能访问控制的唯一主模型。

当前阶段明确不启用 `user_permission` 这类用户直授权模型。

原因：

- 纯 RBAC 更清晰
- 权限来源更容易追踪
- 更适合当前项目早期阶段
- 可以避免权限配置越来越散、越来越难排查

### 3.10 用户等级与权益模型

用户等级（普通用户、VIP、SVIP）不等同于角色，也不直接作为 RBAC 的组成部分。

建议拆分为两层：

#### 权限层

采用 `RBAC`，用于控制是否允许执行某类动作，例如：

- 是否允许创建群
- 是否允许删除消息
- 是否允许管理用户
- 是否允许创建会议

#### 等级权益层

采用 `UserLevel`，用于控制同一功能的额度、质量、人数、配额等差异化能力，例如：

- 音视频通话质量
- 创建会议人数上限
- 文件上传大小上限
- 云存储容量
- 群组数量上限
- AI 调用额度

也就是说：

- `RBAC` 负责：能不能做
- `Level / Benefit` 负责：能做到什么程度

### 3.11 权限粒度建议

当前阶段建议先以“接口级权限”作为主实现粒度。

原因：

1. 接口级权限最适合当前 Spring Boot + Sa-Token 项目落地。
2. 接口级权限是真正的后端安全边界。
3. 按钮级、菜单级更多偏前端展示控制，不应替代后端鉴权。
4. 数据级权限复杂度高，需要组织架构、数据范围模型支撑，当前阶段不建议先做。

当前建议：

- 第一阶段：只做接口级权限
- 表结构中保留 `permission_type`
- 先启用 `api`
- 后续可扩展：
- `menu`
- `button`
- `data`

### 3.13 权限变更生效策略

权限变更需要实时生效。

建议规则：

1. 当角色权限发生变化时，已登录用户的权限不能长期继续使用旧缓存。
2. 角色权限发生变化后，当前账号所有已登录设备应立即失效，要求重新登录。
3. 默认实现方式采用“清理权限缓存 + 失效所有登录态”，避免旧会话继续持有过期权限。
4. 对管理员权限收回、角色降级、敏感资源权限删除等场景，必须按立即失效处理，不允许延迟生效。

也就是说：

- 权限缓存必须及时失效
- 旧登录态不能继续保留
- 用户需要重新登录后再按最新角色权限重新建立会话

### 3.14 登录失败限制策略

登录失败限制先明确如下：

- 连续登录失败 3 次后，账号进入临时锁定状态
- 锁定时长：1 小时
- 锁定到期后自动解封

建议说明：

1. 此处的临时锁定更偏认证失败保护，可单独记录为登录风控状态，不必直接等同于后台“禁用”。
2. 若后续需要更精细化控制，可扩展为按账号、按手机号、按 IP、按设备维度分别限流或锁定。

### 3.15 短信/邮箱验证码规则

当前先统一采用 6 位数字验证码。

建议规则如下：

- 验证码长度：6 位
- 验证码类型：纯数字
- 适用场景：
- 手机号注册
- 手机号登录
- 手机号换绑
- 邮箱注册
- 邮箱登录
- 邮箱换绑
- 找回密码

风控规则明确如下：

1. 单次验证码有效期：5 分钟
2. 单手机号或单邮箱发送间隔：60 秒
3. 单手机号或单邮箱每小时最多发送 5 次
4. 单手机号或单邮箱每天最多发送 20 次
5. 同一 IP 每小时最多发送 20 次
6. 同一 IP 每天最多发送 100 次
7. 验证码连续输错 3 次后，当前验证码校验对象冻结 1 小时
8. 验证码使用后立即失效
9. 超过有效期的验证码立即失效，不允许继续校验

补充说明：

- “当前验证码校验对象冻结 1 小时”可按手机号、邮箱分别记录
- 冻结期间不允许继续校验当前对象对应的验证码
- 短信服务与邮件服务接口按可插拔方式设计，后续仅替换供应商实现

本阶段正式结论：

- 验证码发送频率、有效期、次数限制和冻结策略，按本节当前规则执行。

### 3.16 会话与设备管理规则

会话与设备管理规则明确如下：

1. 允许多端同时在线，第一阶段支持手机端、Web 端、桌面端并行登录。
2. 同端重复登录采用“新登录覆盖旧登录”的策略。
3. 这里的“同端重复登录”是指同一账号在同一终端类型上再次登录，例如同一账号再次登录 Web 端，旧的 Web 会话失效，新会话生效。
4. 不同终端类型之间不互踢，例如手机端登录不会挤掉 Web 端或桌面端。
5. 支持查看当前账号已登录设备列表。
6. 支持手动踢指定设备下线。
7. 支持执行“退出所有设备”操作。
8. 改密码后，当前账号所有设备立即失效，要求重新登录。
9. 账号被禁用后，当前账号所有设备立即失效。
10. 角色或权限发生变化后，当前账号所有设备立即失效。

上述规则可正式概括为：

- 多端共存
- 同端互斥

### 3.12 等级权益配置建议

为支持 VIP / SVIP 后续功能差异，建议预留等级权益表：

```sql
CREATE TABLE `level_benefit` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `level_id` BIGINT UNSIGNED NOT NULL COMMENT '等级ID',
    `benefit_code` VARCHAR(64) NOT NULL COMMENT '权益编码',
    `benefit_name` VARCHAR(128) NOT NULL COMMENT '权益名称',
    `benefit_type` VARCHAR(32) NOT NULL COMMENT 'int/string/boolean/json',
    `benefit_value` VARCHAR(255) NOT NULL COMMENT '权益值',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '权益说明',
    `is_delete` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除',
    `create_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
    `update_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
    `delete_time` DATETIME DEFAULT NULL COMMENT '删除时间',
    `delete_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '删除人',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_level_benefit` (`level_id`, `benefit_code`)
) COMMENT='等级权益配置表';
```

示例权益：

- `meeting_max_member`
- `meeting_video_quality`
- `group_max_count`
- `group_member_limit`
- `file_upload_max_mb`
- `ai_daily_quota`

示例语义：

- 普通用户：`meeting_max_member = 10`
- VIP：`meeting_max_member = 50`
- SVIP：`meeting_max_member = 300`

## 4. 用户域表设计建议

### 4.1 用户主表 `user`

建议重构为：

```sql
CREATE TABLE `user` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_no` VARCHAR(64) NOT NULL COMMENT '用户唯一编号',
    `password_hash` VARCHAR(255) DEFAULT NULL COMMENT '密码哈希',
    `nickname` VARCHAR(64) DEFAULT NULL COMMENT '昵称',
    `avatar_url` VARCHAR(512) DEFAULT NULL COMMENT '头像地址',
    `gender` TINYINT UNSIGNED DEFAULT 0 COMMENT '性别 0-未知 1-男 2-女',
    `status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态 1-正常 2-待补全 3-禁用 4-冻结 5-注销',
    `status_reason` VARCHAR(255) DEFAULT NULL COMMENT '当前状态原因',
    `status_expire_time` DATETIME DEFAULT NULL COMMENT '状态失效时间，适用于临时禁用或冻结',
    `register_source` VARCHAR(32) NOT NULL COMMENT '注册来源 phone/email/qq/admin_create',
    `phone` VARCHAR(32) DEFAULT NULL COMMENT '手机号',
    `is_phone_verified` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '手机号是否已验证',
    `email` VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    `is_email_verified` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '邮箱是否已验证',
    `login_fail_count` INT NOT NULL DEFAULT 0 COMMENT '连续登录失败次数',
    `login_lock_expire_time` DATETIME DEFAULT NULL COMMENT '登录失败锁定失效时间',
    `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
    `last_login_ip` VARCHAR(64) DEFAULT NULL COMMENT '最后登录IP',
    `is_delete` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除 0-否 1-是',
    `create_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
    `update_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
    `delete_time` DATETIME DEFAULT NULL COMMENT '删除时间',
    `delete_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '删除人',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_user_no` (`user_no`),
    UNIQUE KEY `uk_user_phone` (`phone`),
    UNIQUE KEY `uk_user_email` (`email`)
) COMMENT='用户主表';
```

说明：

- `password_hash` 替代原来的明文字段 `password`
- 增加 `user_no` 作为更稳定的业务编号
- 增加 `is_phone_verified`
- 增加 `is_email_verified`
- 增加 `register_source`
- 增加 `status_reason`、`status_expire_time`
- 增加 `login_fail_count`、`login_lock_expire_time`
- 增加 `last_login_time`、`last_login_ip`
- 增加 `is_delete`
- 增加 `create_by`、`update_by`、`delete_time`、`delete_by` 审计字段
- `phone` 作为主身份标识，必须唯一
- `user_no` 作为内部稳定业务编号，必须唯一且不可修改
- `nickname` 作为展示昵称，可随时修改，允许重复
- `email` 若填写则必须唯一，作为辅助登录和联系字段使用
- `phone` 一旦注册成功，默认不允许用户自行修改

## 4.1.1 密码安全策略

密码规则建议明确如下：

### 密码复杂度

- 最小长度：10 位
- 必须同时包含：
- 大写字母
- 小写字母
- 数字
- 特殊符号

### 弱密码限制

- 常见弱密码库命中直接拒绝
- 不能包含 `nickname`
- 不能包含手机号后 4 位或后 6 位
- 不能是连续字符
- 不能是重复字符
- 不能是明显键盘顺序串

### 传输方案

本阶段建议采用“HTTPS + 前端 RSA 公钥加密 + 后端私钥解密”的方案。

规则如下：

1. 所有密码相关接口必须走 HTTPS。
2. 前端提交密码时，使用服务端提供的 RSA 公钥加密。
3. 后端收到密文后，使用 RSA 私钥解密得到原始密码。
4. 后端对原始密码执行复杂度校验和弱密码校验。
5. 后端不保存明文密码，也不保存可逆密文。

### 后端存储方案

建议使用：

- `Argon2id`

如果后续落地时因生态兼容或接入成本需要降级，可退一步使用：

- `BCrypt`

明确禁止：

- MD5
- SHA1
- 单独直接使用 SHA256 存储密码
- 可逆加密方式直接存储密码

### 密码过期策略

本阶段不建议默认启用强制周期过期。

建议仅在以下场景要求重置密码：

- 首次设置临时密码
- 安全风险命中
- 管理员重置密码
- 密码疑似泄露

### 找回与重置密码规则

密码找回与重置规则明确如下：

1. 忘记密码支持“短信验证码”或“邮箱验证码”二选一方式完成身份校验。
2. 若用户同时绑定手机号和邮箱，可由前端让用户选择找回渠道。
3. 完成密码重置后，必须强制当前账号全端下线，避免旧会话继续使用。
4. 管理员重置密码后，用户下次登录必须进入首次改密流程，修改完成前不允许继续使用核心功能。
5. 首次改密完成后，再建立新的正常登录态并清除“需改密”标记。

### 4.2 用户等级表 `user_level`

用于定义普通用户、VIP、SVIP 等平台等级。

```sql
CREATE TABLE `user_level` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `level_code` VARCHAR(32) NOT NULL COMMENT '等级编码 NORMAL/VIP/SVIP',
    `level_name` VARCHAR(64) NOT NULL COMMENT '等级名称',
    `level_rank` INT NOT NULL COMMENT '等级排序，数值越大等级越高',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '等级说明',
    `is_delete` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除',
    `create_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
    `update_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
    `delete_time` DATETIME DEFAULT NULL COMMENT '删除时间',
    `delete_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '删除人',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_level_code` (`level_code`)
) COMMENT='用户等级表';
```

### 4.3 用户等级关系表 `user_level_relation`

用于记录用户当前等级及生效时间。

```sql
CREATE TABLE `user_level_relation` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `level_id` BIGINT UNSIGNED NOT NULL COMMENT '等级ID',
    `start_time` DATETIME DEFAULT NULL COMMENT '生效时间',
    `end_time` DATETIME DEFAULT NULL COMMENT '失效时间',
    `status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态 1-生效 0-失效',
    `is_delete` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除',
    `create_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
    `update_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
    `delete_time` DATETIME DEFAULT NULL COMMENT '删除时间',
    `delete_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '删除人',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_level_relation_user_id` (`user_id`)
) COMMENT='用户等级关系表';
```

### 4.4 权限表 `permission`

```sql
CREATE TABLE `permission` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `permission_code` VARCHAR(64) NOT NULL COMMENT '权限编码',
    `permission_name` VARCHAR(128) NOT NULL COMMENT '权限名称',
    `permission_type` VARCHAR(32) NOT NULL COMMENT '权限类型，当前阶段主用 api，预留 menu/button/data',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '权限说明',
    `is_delete` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除',
    `create_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
    `update_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
    `delete_time` DATETIME DEFAULT NULL COMMENT '删除时间',
    `delete_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '删除人',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_permission_code` (`permission_code`)
) COMMENT='权限表';
```

### 4.5 角色表 `role`

```sql
CREATE TABLE `role` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `role_code` VARCHAR(64) NOT NULL COMMENT '角色编码',
    `role_name` VARCHAR(128) NOT NULL COMMENT '角色名称',
    `role_type` VARCHAR(32) NOT NULL COMMENT '角色类型 system/custom',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '角色说明',
    `is_delete` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除',
    `create_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
    `update_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
    `delete_time` DATETIME DEFAULT NULL COMMENT '删除时间',
    `delete_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '删除人',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`)
) COMMENT='角色表';
```

角色配置规则明确如下：

- 角色区分为系统内置角色和自定义角色
- 系统内置角色：用于平台核心角色，不允许随意删除或修改核心编码
- 自定义角色：允许后台动态配置和维护

建议：

1. `role_code` 必须唯一
2. `role_type` 取值建议为 `system` / `custom`
3. 系统角色应限制删除与关键字段变更

### 4.6 角色权限关系表 `role_permission`

```sql
CREATE TABLE `role_permission` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `role_id` BIGINT UNSIGNED NOT NULL,
    `permission_id` BIGINT UNSIGNED NOT NULL,
    `is_delete` TINYINT UNSIGNED NOT NULL DEFAULT 0,
    `create_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
    `update_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
    `delete_time` DATETIME DEFAULT NULL COMMENT '删除时间',
    `delete_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '删除人',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`)
) COMMENT='角色权限关系表';
```

### 4.7 用户角色关系表 `user_role`

```sql
CREATE TABLE `user_role` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `role_id` BIGINT UNSIGNED NOT NULL,
    `is_delete` TINYINT UNSIGNED NOT NULL DEFAULT 0,
    `create_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
    `update_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
    `delete_time` DATETIME DEFAULT NULL COMMENT '删除时间',
    `delete_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '删除人',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`)
) COMMENT='用户角色关系表';
```

### 4.8 第三方账号绑定表 `user_oauth_bind`

先为 QQ 登录预留。

```sql
CREATE TABLE `user_oauth_bind` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `provider` VARCHAR(32) NOT NULL COMMENT 'qq/wechat/github',
    `provider_user_id` VARCHAR(128) NOT NULL COMMENT '第三方用户唯一标识',
    `union_id` VARCHAR(128) DEFAULT NULL COMMENT '开放平台 unionId',
    `access_token` VARCHAR(512) DEFAULT NULL COMMENT '三方访问令牌',
    `refresh_token` VARCHAR(512) DEFAULT NULL COMMENT '三方刷新令牌',
    `is_delete` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除',
    `create_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人',
    `update_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人',
    `delete_time` DATETIME DEFAULT NULL COMMENT '删除时间',
    `delete_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '删除人',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_provider_user` (`provider`, `provider_user_id`),
    UNIQUE KEY `uk_user_provider` (`user_id`, `provider`)
) COMMENT='第三方账号绑定表';
```

## 5. 推荐接口设计

## 5.1 注册接口

### `POST /auth/register/phone`

适用场景：
- 手机号主流程注册

入参建议：
- phone
- smsCode
- password
- nickname（可选）

说明：
- 手机号是主身份标识，注册时即完成唯一账号建立
- `nickname` 仅作为展示字段，不承担登录主键职责

### `POST /auth/register/email`

适用场景：
- 邮箱验证码注册

入参建议：
- email
- emailCode
- phone
- smsCode

说明：
- 因为账号必须绑定手机号，所以邮箱注册流程里仍建议绑定手机号

### `POST /auth/register/qq`

适用场景：
- 第三方 QQ 授权注册/登录

入参建议：
- code
- state
- phone（若首次注册且未绑定手机号）
- smsCode

## 5.2 登录接口

### `POST /auth/login/password`

入参：
- phone
- password

### `POST /auth/login/phone-code`

入参：
- phone
- smsCode

### `POST /auth/login/email-code`

入参：
- email
- emailCode

说明：
- 邮箱属于辅助登录方式，不取代手机号主身份标识
- 若邮箱账号尚未绑定手机号，则登录后只能进入待补全流程

### `POST /auth/login/qq`

入参：
- code
- state

### `POST /auth/account/recover`

适用场景：
- 用户在登录时收到“账号可恢复”提示后，主动确认恢复原账号

入参建议：
- phone
- smsCode 或 emailCode

说明：
- 恢复账号必须走单独接口，不与普通登录接口混用
- 接口成功后恢复原账号状态，并建立新的登录态

### `POST /auth/password/forgot`

适用场景：
- 用户忘记密码后，通过短信验证码或邮箱验证码校验身份并重置密码

入参建议：
- phone 或 email
- smsCode 或 emailCode
- newPassword

说明：
- 找回密码支持短信或邮箱二选一
- 重置成功后必须强制当前账号所有登录设备下线

### `POST /auth/password/change-first-login`

适用场景：
- 管理员重置密码后，用户首次登录强制修改密码

入参建议：
- oldPassword 或 resetToken
- newPassword

说明：
- 用户在首次改密完成前，不允许继续访问核心业务接口
- 修改成功后清除“首次改密”限制标记

### `POST /auth/logout`

- 退出当前登录态

### `POST /auth/logout-all`

- 退出当前账号的所有登录设备

### `GET /auth/device/list`

- 查看当前账号已登录设备列表

### `POST /auth/device/{deviceId}/offline`

- 手动踢指定设备下线

### `GET /auth/me`

返回：
- 基础用户信息
- 当前等级
- 当前角色列表
- 当前权限列表

## 5.3 用户后台管理接口建议

用户后台管理侧建议至少支持以下操作：

- 新增用户
- 修改用户
- 禁用用户
- 解禁用户
- 冻结用户
- 解冻用户
- 注销用户
- 删除用户（逻辑删除）
- 用户分页查询
- 用户详情查询

推荐接口示例：

- `POST /admin/user`
- `PUT /admin/user/{id}`
- `POST /admin/user/{id}/disable`
- `POST /admin/user/{id}/enable`
- `POST /admin/user/{id}/freeze`
- `POST /admin/user/{id}/unfreeze`
- `POST /admin/user/{id}/cancel`
- `DELETE /admin/user/{id}`
- `GET /admin/user/page`
- `GET /admin/user/{id}`

### 后台新增用户规则

后台新增用户时，不采用“默认密码”策略。

当前建议：

1. 后台可先创建仅绑定手机号的用户主体。
2. 创建时不设置默认密码。
3. 用户后续通过短信验证码完成激活。
4. 激活完成后，再由用户自行设置密码。

进一步明确如下：

5. 后台创建用户后，账号默认进入 `PENDING_COMPLETE` 状态。
6. 激活流程中必须完成设密，未设密前不能转为 `NORMAL`。
7. 激活过程默认不自动登录，用户激活完成后再显式走正常登录流程。
8. 如需补充验证，可结合邮箱验证码作为辅助验证方式。

这样可以避免：

- 固定默认密码带来的安全风险
- 管理员直接掌握用户初始密码
- 首次登录即携带弱默认密码的问题

### 用户分页查询筛选条件建议

第一阶段建议支持以下筛选条件：

- `userNo`
- `nickname`
- `phone`
- `email`
- `status`
- `level`
- `role`
- `createTimeStart`
- `createTimeEnd`
- `isDelete`

查询建议：

- `userNo`：支持精确查询
- `nickname`：支持模糊查询
- `phone`：优先精确查询
- `email`：优先精确查询
- 创建时间：建议采用区间查询

推荐查询 DTO：

```java
public class UserPageQueryDTO {
    private String userNo;
    private String nickname;
    private String phone;
    private String email;
    private Integer status;
    private String levelCode;
    private String roleCode;
    private LocalDateTime createTimeStart;
    private LocalDateTime createTimeEnd;
    private Integer isDelete;
    private Integer pageNum;
    private Integer pageSize;
}
```

默认排序建议：

- 用户分页默认按 `create_time desc`
- 即最新创建的用户优先展示

### 用户状态类操作审计要求

以下操作必须填写原因：

- 冻结
- 禁用
- 注销
- 删除

建议同步记录：

- 操作原因
- 操作人
- 操作时间
- 备注信息

补充明确：

- 重置密码也属于高风险操作，必须记录审计日志
- 审计日志应区分后台人工操作与系统自动触发操作

## 6. 统一认证服务设计建议

Service 层建议统一沉淀：

- `AuthService`
- `RegisterService`
- `UserBindService`
- `PermissionService`

其中 `AuthService` 内部统一编排：

- 查找用户
- 校验登录方式
- 校验状态
- 建立登录态
- 返回登录信息

即使 Controller 拆成多个接口，Service 也不要四处复制逻辑。

## 7. 登录返回结构建议

建议专门定义登录返回 VO，例如：

```java
public class LoginUserVO {
    private Long userId;
    private String userNo;
    private String nickname;
    private String phone;
    private String token;
    private String levelCode;
    private String levelName;
    private List<String> roleCodes;
    private List<String> permissionCodes;
}
```

不要再直接返回 `User` 实体。

## 8. 架构图

```text
+-------------------+
| Client / Frontend |
+---------+---------+
          |
          v
+-------------------+
|   AuthController  |
| RegisterController|
+---------+---------+
          |
          v
+-------------------------------+
| AuthService / RegisterService |
| UserBindService               |
| PermissionService             |
+---------+---------------------+
          |
          +----------------------+
          |                      |
          v                      v
+-------------------+   +----------------------+
| User / Role /     |   | Third-party Provider |
| Permission Tables |   | QQ / SMS / Email     |
+-------------------+   +----------------------+
          |
          v
+-------------------+
| Sa-Token Session  |
| Redis / Token     |
+-------------------+
```

## 9. 注册登录流程图

### 9.1 账号密码登录流程

```text
用户提交手机号与密码
      |
      v
参数校验
      |
      v
根据手机号查用户
      |
      v
校验密码 / 状态 / 是否删除
      |
      v
读取用户等级 / 角色 / 权限
      |
      v
Sa-Token 建立登录态
      |
      v
返回 token + 用户信息 + 等级 + 权限
```

### 9.2 QQ 登录流程

```text
前端获取 QQ code
      |
      v
后端换取第三方身份标识
      |
      v
根据 provider_user_id 查绑定关系
      |
      +---- 已绑定 ----> 直接登录
      |
      +---- 未绑定 ----> 创建待补全账号 / 绑定手机号
                               |
                               v
                        建立绑定关系并登录
```

## 10. 与聊天业务的关系

后续办公聊天系统至少还会新增：

- 群组表
- 群成员表
- 会话表
- 单聊会话关系表
- 消息表
- 文件表
- 音视频会话表

这些业务都依赖“稳定用户主键 + 稳定用户编号 + 稳定权限体系 + 登录态体系”，因此用户域必须先收紧。

## 11. 实施顺序建议

建议按以下顺序落地：

1. 重构 `user` 表
2. 增加 `user_level`、`user_level_relation`
3. 增加 `role`、`permission`、`user_role`、`role_permission`
4. 增加 `user_oauth_bind`
5. 重构 `AuthService`
6. 重构 `AuthController`
7. 补注册/登录/登出接口
8. 补 VO / DTO
9. 补 Mapper / XML
10. 补测试

## 12. 当前已确认规则汇总

当前已明确的关键业务规则如下：

1. `phone` 是用户主身份标识，必须唯一，且逻辑删除后也不释放给新账号
2. `user_no` 是内部稳定业务编号，不可修改
3. `nickname` 是展示字段，允许重复，允许随时修改，不作为登录主键
4. `email` 是辅助联系与辅助登录字段，若填写则必须唯一
5. 所有账号最终都必须绑定手机号；QQ 首次登录若无手机号，进入待补全状态
6. 账号状态采用：正常、待补全、禁用、冻结、注销；仅正常和待补全允许登录
7. 待补全账号允许登录，但补全前不能使用核心业务功能
8. 冻结允许用户通过手机号验证码自助解冻；禁用需后台解封或到期自动解封
9. 注销后允许恢复原账号，不新建账号；恢复时保留历史聊天与协作痕迹，但不自动恢复敏感权限和订阅权益
10. 恢复账号采用单独接口模式；登录时若命中可恢复状态，先提示用户确认，再调用恢复接口
11. 登录失败连续 3 次锁定 1 小时
12. 验证码统一为 6 位数字；发送间隔、每小时/每天上限、同 IP 限流都必须执行；验证码连续输错 3 次冻结 1 小时
13. 忘记密码支持短信验证码或邮箱验证码二选一；密码重置成功后必须强制全端下线
14. 管理员重置密码后，用户下次登录必须先完成首次改密
15. 允许手机端、Web 端、桌面端多端同时在线；同端重复登录时，新登录覆盖旧登录
16. 支持查看已登录设备、踢指定设备下线、退出所有设备
17. 改密码、禁用、角色权限变更后，当前账号所有设备立即失效
18. 密码最少 10 位，必须包含大小写字母、数字、特殊符号，且不允许弱密码
19. 密码传输采用 HTTPS + 前端 RSA 公钥加密，后端解密后使用 `Argon2id` 或 `BCrypt` 哈希存储
20. 用户不允许直接持有权限，权限模型采用纯 `RBAC`
21. 第一阶段只做接口级权限；角色权限变更后需实时生效，并立即失效所有旧登录态
22. VIP / SVIP 来源于付费订阅，不直接替代 RBAC，而是控制配额、质量、人数上限等权益
23. 后台需要支持新增、修改、冻结、解冻、注销、删除、分页查询、详情查询
24. 后台新增用户时不设置默认密码，而是创建手机号主体后由用户通过短信验证码激活并自行设置密码
25. 冻结、禁用、注销、删除都必须填写原因，并记录操作人、操作时间和备注
26. 用户分页默认按 `create_time desc` 排序，第一阶段支持 `userNo`、`nickname`、`phone`、`email`、`status`、`level`、`role`、创建时间区间、`isDelete` 等筛选条件

27. 第一阶段后台管理能力应至少包含：用户列表、登录日志查看、在线设备查看、按设备踢下线、用户禁用与解冻
28. 第一阶段手机号注册必须通过短信验证码校验；邮箱验证码可作为补充验证方式；后台允许创建用户后补激活
29. 忘记密码支持短信验证码或邮箱验证码二选一；修改密码必须校验旧密码
30. 后台创建用户后默认进入待补全状态；首次激活时必须完成设密，再切换为正常状态
31. 后台创建用户后的激活流程默认不自动登录，用户完成激活和设密后再显式登录
32. 重置密码也属于高风险后台操作，必须记录审计日志并保留原因、操作人、时间和备注

## 13. 后续待细化问题

当前仍建议在详细设计或实现阶段继续细化以下内容：

1. 短信与邮件模板编号、供应商切换和发送失败降级策略
2. 注销恢复时好友关系、群成员关系、业务资格是否全部恢复，以及哪些关系需要二次确认
3. 设备风险标记、异常设备识别和高风险登录二次校验策略
4. 后续聊天系统是否扩展到企业组织架构、部门、审批、日历等办公能力

## 14. 建议结论

基于当前需求，建议：

- 登录态继续使用 `Sa-Token`
- 不在本阶段切换纯 JWT
- 先做多入口认证接口，统一 service 编排
- 先做用户等级 + 角色权限体系
- 所有表统一补 `is_delete`
- 登录后统一返回等级与权限
- 为 QQ 登录单独设计绑定表，不把第三方标识直接塞进用户主表
