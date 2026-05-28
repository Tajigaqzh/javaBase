# 接口清单与状态总表

## 1. 目的

本文件用于统一记录当前项目已有接口、规划中接口及其状态，避免后续需求讨论时反复确认：

- 接口是否已存在
- 接口属于前台还是后台
- 接口是否需要登录
- 接口当前是已实现、待实现还是仅有方案

## 2. 状态定义

为避免歧义，接口状态统一使用以下值：

- `已实现`
- `待实现`
- `仅方案`
- `已移除`

## 3. 当前前台接口

### 3.1 认证相关

| 路径 | 方法 | 所属模块 | 鉴权要求 | 状态 | 说明 |
|---|---|---|---|---|---|
| `/auth/code/phone/send` | `POST` | `javabase-frontend-app` | 无 | `已实现` | 发送手机号验证码，执行发送冷却与频控 |
| `/auth/code/email/send` | `POST` | `javabase-frontend-app` | 无 | `已实现` | 发送邮箱验证码，执行发送冷却与频控 |
| `/auth/register/phone` | `POST` | `javabase-frontend-app` | 无 | `已实现` | 手机号注册，必须完成短信验证码校验并直接建立登录态 |
| `/auth/login/password` | `POST` | `javabase-frontend-app` | 无 | `已实现` | 手机号密码登录，记录设备和登录日志 |
| `/auth/login/phone-code` | `POST` | `javabase-frontend-app` | 无 | `已实现` | 手机号验证码登录，记录设备和登录日志 |
| `/auth/logout` | `POST` | `javabase-frontend-app` | 已登录 | `已实现` | 退出当前登录态 |
| `/auth/logout-all` | `POST` | `javabase-frontend-app` | 已登录 | `已实现` | 退出当前账号全部设备，支持保留当前设备 |
| `/auth/me` | `GET` | `javabase-frontend-app` | 已登录 | `已实现` | 查询当前登录用户信息 |
| `/auth/token-info` | `GET` | `javabase-frontend-app` | 已登录 | `已实现` | 查询当前 Sa-Token 元信息，偏调试用途 |
| `/auth/password/set` | `POST` | `javabase-frontend-app` | 无 | `已实现` | 首次设置密码或待补全账号补设密码 |
| `/auth/password/forgot/reset` | `POST` | `javabase-frontend-app` | 无 | `已实现` | 通过手机号验证码或邮箱验证码重置密码 |
| `/auth/password/change` | `POST` | `javabase-frontend-app` | 已登录 | `已实现` | 已登录状态下修改密码，必须校验旧密码 |
| `/auth/session/list` | `GET` | `javabase-frontend-app` | 已登录 | `已实现` | 查询当前账号已登录设备列表 |
| `/auth/session/kickout` | `POST` | `javabase-frontend-app` | 已登录 | `已实现` | 踢掉指定设备，不允许踢当前设备 |

### 3.2 示例接口

| 路径 | 方法 | 所属模块 | 鉴权要求 | 状态 | 说明 |
|---|---|---|---|---|---|
| `/demo` | `GET` | `javabase-frontend-app` | 无 | `已实现` | 公开示例接口 |
| `/demo/login` | `GET` | `javabase-frontend-app` | 已登录 | `已实现` | 登录态示例接口 |
| `/demo/permission` | `GET` | `javabase-frontend-app` | 权限 `user:write` | `已实现` | 权限示例接口 |

## 4. 当前后台接口

当前后台模块 `javabase-admin-app` 仍处于空壳阶段。

| 路径 | 方法 | 所属模块 | 鉴权要求 | 状态 | 说明 |
|---|---|---|---|---|---|
| 无 | 无 | `javabase-admin-app` | 无 | `仅方案` | 后台接口尚未开始实现 |

## 5. 当前已移除或暂未暴露的能力

| 能力 | 所属模块 | 状态 | 说明 |
|---|---|---|---|
| AI HTTP 接口 | `javabase-frontend-app` | `已移除` | `OpenAiChatService` 仍在，但 HTTP 暴露层已暂时移除，后续会重新接入 |

## 6. 已有文档入口

当前前台文档入口：

| 路径 | 状态 | 说明 |
|---|---|---|
| `/scalar` | `已实现` | 当前主要 API 文档入口 |
| `/v3/api-docs` | `已实现` | OpenAPI JSON |
| `/doc.html` | `已实现` | 已重定向到 `/scalar` |
| `/swagger-ui.html` | `已实现` | 已重定向到 `/scalar` |

## 7. 规划中的前台接口

以下接口当前处于需求或方案阶段，尚未实现。

### 7.1 登录设备与会话管理

| 路径 | 方法 | 所属模块 | 鉴权要求 | 状态 | 说明 |
|---|---|---|---|---|---|
| `/auth/account/recover` | `POST` | `javabase-frontend-app` | 无 | `仅方案` | 恢复已注销但可恢复账号 |

### 7.2 前台注册与多方式登录

| 路径 | 方法 | 所属模块 | 鉴权要求 | 状态 | 说明 |
|---|---|---|---|---|---|
| `/auth/register/email` | `POST` | `javabase-frontend-app` | 无 | `仅方案` | 邮箱注册 |
| `/auth/register/qq` | `POST` | `javabase-frontend-app` | 无 | `仅方案` | QQ 注册或首次绑定 |
| `/auth/login/email-code` | `POST` | `javabase-frontend-app` | 无 | `仅方案` | 邮箱验证码登录 |
| `/auth/login/qq` | `POST` | `javabase-frontend-app` | 无 | `仅方案` | QQ 登录 |

### 7.3 AI 能力

| 路径 | 方法 | 所属模块 | 鉴权要求 | 状态 | 说明 |
|---|---|---|---|---|---|
| `/ai/chat` | `POST` | `javabase-frontend-app` | 已登录，后续可能附加权限 | `仅方案` | 重新接入 AI 对话能力 |

## 8. 规划中的后台接口

以下接口属于后台模块预留方向，当前不实现。

| 路径 | 方法 | 所属模块 | 鉴权要求 | 状态 | 说明 |
|---|---|---|---|---|---|
| `/admin/auth/login` | `POST` | `javabase-admin-app` | 无 | `仅方案` | 后台登录 |
| `/admin/auth/logout` | `POST` | `javabase-admin-app` | 后台已登录 | `仅方案` | 后台退出 |
| `/admin/auth/session/kickout` | `POST` | `javabase-admin-app` | 后台已登录 | `仅方案` | 后台强制设备下线 |
| `/admin/auth/login-log/page` | `GET` | `javabase-admin-app` | 后台已登录 | `仅方案` | 分页查看登录日志，第一阶段至少支持手机号、终端类型、登录状态、时间范围、设备ID、IP 筛选 |
| `/admin/auth/session/list` | `GET` | `javabase-admin-app` | 后台已登录 | `仅方案` | 查看用户在线设备会话 |
| `/admin/user/create` | `POST` | `javabase-admin-app` | 后台已登录 | `仅方案` | 后台创建用户，默认进入待补全状态 |
| `/admin/user/list` | `GET` | `javabase-admin-app` | 后台已登录 | `仅方案` | 后台用户管理列表，可覆盖后台创建用户、待补全状态和补激活场景 |
| `/admin/user/disable` | `POST` | `javabase-admin-app` | 后台已登录 | `仅方案` | 禁用用户 |
| `/admin/user/enable` | `POST` | `javabase-admin-app` | 后台已登录 | `仅方案` | 解禁用户 |
| `/admin/user/freeze` | `POST` | `javabase-admin-app` | 后台已登录 | `仅方案` | 冻结用户 |
| `/admin/user/unfreeze` | `POST` | `javabase-admin-app` | 后台已登录 | `仅方案` | 解冻用户 |
| `/admin/user/reset-password` | `POST` | `javabase-admin-app` | 后台已登录 | `仅方案` | 管理员重置密码，用户下次登录强制改密 |

## 9. 当前接口层面的明确约束

1. 前台接口默认只落在 `javabase-frontend-app`
2. 后台接口后续只落在 `javabase-admin-app`
3. `domain` 和 `utils` 不承载 HTTP 接口
4. 当前前台认证接口不代表后台认证可复用
5. 文档入口统一以 `/scalar` 为主

## 10. 后续维护要求

后续每当出现以下变更时，应同步更新本文件：

1. 新增接口
2. 删除接口
3. 接口路径变更
4. 鉴权要求变更
5. 接口状态从“仅方案”变为“已实现”
