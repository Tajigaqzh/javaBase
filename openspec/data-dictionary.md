# 枚举与数据字典总表

## 1. 目的

本文件用于统一收口当前 `openspec` 中已经出现或已明确规划的核心枚举、状态值和数据字典，避免后续数据库值、Java 枚举、接口文档和前端展示各写各的。

本文件是当前阶段的数据语义总表，后续新增状态或枚举时应优先更新本文件。

## 2. 用户状态

### 字段

- `user.status`

### 建议值

| Code | Name | 含义 | 是否允许登录 |
|---|---|---|---|
| `1` | `NORMAL` | 正常账号 | 是 |
| `2` | `PENDING_COMPLETE` | 待补全账号 | 是，但仅允许受限能力 |
| `3` | `DISABLED` | 被后台禁用 | 否 |
| `4` | `FROZEN` | 被临时冻结 | 否 |
| `5` | `CANCELLED` | 已注销 | 否，需单独恢复流程 |

## 3. 注册来源

### 字段

- `user.register_source`

### 建议值

| Code | Name | 含义 |
|---|---|---|
| `phone` | `PHONE` | 手机号注册 |
| `email` | `EMAIL` | 邮箱注册 |
| `qq` | `QQ` | QQ 首次登录或注册 |
| `admin_create` | `ADMIN_CREATE` | 后台创建 |

## 4. 终端类型

### 字段

- `login_request.terminalType`
- `user_login_log.terminal_type`
- `user_session_device.terminal_type`

### 建议值

| Code | Name | 含义 |
|---|---|---|
| `WEB` | `WEB` | 浏览器端 |
| `ANDROID` | `ANDROID` | 安卓端 |
| `IOS` | `IOS` | iOS 端 |

### 预留扩展值

| Code | Name | 含义 |
|---|---|---|
| `MINIAPP` | `MINIAPP` | 小程序端 |
| `PAD` | `PAD` | 平板端 |
| `DESKTOP` | `DESKTOP` | 桌面客户端 |

## 5. 登录状态

### 字段

- `user_login_log.login_status`

### 建议值

| Code | Name | 含义 |
|---|---|---|
| `1` | `SUCCESS` | 登录成功 |
| `0` | `FAIL` | 登录失败 |

## 6. 会话状态

### 字段

- `user_session_device.session_status`

### 建议值

| Code | Name | 含义 |
|---|---|---|
| `1` | `ONLINE` | 当前在线 |
| `2` | `LOGOUT` | 主动退出 |
| `3` | `KICKOUT` | 被踢下线 |
| `4` | `EXPIRED` | 会话失效或过期 |

## 7. 会话失效原因

### 字段

- `user_session_device.kickout_reason`
- `user_login_log.kickout_reason`

### 建议值

| Code | Name | 含义 |
|---|---|---|
| `ACCOUNT_DISABLED` | `ACCOUNT_DISABLED` | 账号被禁用 |
| `ACCOUNT_FROZEN` | `ACCOUNT_FROZEN` | 账号被冻结 |
| `PASSWORD_CHANGED` | `PASSWORD_CHANGED` | 密码修改导致失效 |
| `PERMISSION_CHANGED` | `PERMISSION_CHANGED` | 角色或权限变化导致失效 |
| `MANUAL_KICKOUT` | `MANUAL_KICKOUT` | 手动踢下线 |
| `LOGOUT` | `LOGOUT` | 主动退出 |
| `EXPIRED` | `EXPIRED` | 自然过期 |

## 8. 权限类型

### 字段

- `permission.permission_type`

### 建议值

| Code | Name | 含义 | 当前阶段是否启用 |
|---|---|---|---|
| `api` | `API` | 后端接口权限 | 是 |
| `menu` | `MENU` | 菜单展示权限 | 否 |
| `button` | `BUTTON` | 按钮权限 | 否 |
| `data` | `DATA` | 数据范围权限 | 否 |

## 9. 性别

### 字段

- `user.gender`

### 建议值

| Code | Name | 含义 |
|---|---|---|
| `0` | `UNKNOWN` | 未知 |
| `1` | `MALE` | 男 |
| `2` | `FEMALE` | 女 |

## 10. 手机/邮箱验证状态

### 字段

- `user.is_phone_verified`
- `user.is_email_verified`

### 建议值

| Code | Name | 含义 |
|---|---|---|
| `0` | `UNVERIFIED` | 未验证 |
| `1` | `VERIFIED` | 已验证 |

## 11. 逻辑删除标记

### 字段

- `user.is_delete`

### 建议值

| Code | Name | 含义 |
|---|---|---|
| `0` | `NOT_DELETED` | 未删除 |
| `1` | `DELETED` | 已逻辑删除 |

## 12. 等级权益值类型

### 字段

- `level_benefit.benefit_type`

### 建议值

| Code | Name | 含义 |
|---|---|---|
| `int` | `INT` | 整数型 |
| `string` | `STRING` | 字符串型 |
| `boolean` | `BOOLEAN` | 布尔型 |
| `json` | `JSON` | JSON 结构型 |

## 13. 验证码校验对象建议值

### 说明

当前任务文档中还没有正式落表，但后续验证码能力实现时建议提前统一对象类型。

### 建议值

| Code | Name | 含义 |
|---|---|---|
| `PHONE` | `PHONE` | 手机号验证码对象 |
| `EMAIL` | `EMAIL` | 邮箱验证码对象 |

## 14. 接口文档状态值

### 字段

- `openspec/api-catalog.md` 中的接口状态

### 建议值

| Code | Name | 含义 |
|---|---|---|
| `已实现` | `IMPLEMENTED` | 已存在并可调用 |
| `待实现` | `PENDING` | 已明确进入开发计划 |
| `仅方案` | `PROPOSED` | 只有需求或方案，尚未开始 |
| `已移除` | `REMOVED` | 之前存在，目前已撤销 |

## 15. 当前使用原则

后续实现时，建议遵循以下原则：

1. Java 枚举 code 与数据库 code 保持一致
2. 接口文档和数据库注释尽量复用本文件语义
3. 不要在不同模块里为同一状态创造不同命名
4. 新增状态值时先更新本文件，再落代码与表结构
