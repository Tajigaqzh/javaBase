#!/usr/bin/env bash
set -euo pipefail

MYSQL_CONTAINER_NAME="${MYSQL_CONTAINER_NAME:-javabase-mysql}"
REDIS_CONTAINER_NAME="${REDIS_CONTAINER_NAME:-javabase-redis}"

MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-root123456}"
MYSQL_DATABASE="${MYSQL_DATABASE:-javabase}"
MYSQL_USER="${MYSQL_USER:-javabase}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-javabase123}"

MYSQL_PORT="${MYSQL_PORT:-3306}"
REDIS_PORT="${REDIS_PORT:-6379}"

MYSQL_IMAGE="${MYSQL_IMAGE:-mysql:8.4}"
REDIS_IMAGE="${REDIS_IMAGE:-redis:7.4}"

MYSQL_VOLUME="${MYSQL_VOLUME:-javabase_mysql_data}"
REDIS_VOLUME="${REDIS_VOLUME:-javabase_redis_data}"

echo "[1/6] Checking docker"
docker --version >/dev/null

echo "[2/6] Pulling images"
docker pull "${MYSQL_IMAGE}"
docker pull "${REDIS_IMAGE}"

echo "[3/6] Preparing volumes"
docker volume create "${MYSQL_VOLUME}" >/dev/null
docker volume create "${REDIS_VOLUME}" >/dev/null

if ! docker ps -a --format '{{.Names}}' | grep -qx "${MYSQL_CONTAINER_NAME}"; then
  echo "[4/6] Creating MySQL container"
  docker run -d \
    --name "${MYSQL_CONTAINER_NAME}" \
    --restart unless-stopped \
    -e MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD}" \
    -e MYSQL_DATABASE="${MYSQL_DATABASE}" \
    -e MYSQL_USER="${MYSQL_USER}" \
    -e MYSQL_PASSWORD="${MYSQL_PASSWORD}" \
    -p "${MYSQL_PORT}:3306" \
    -v "${MYSQL_VOLUME}:/var/lib/mysql" \
    "${MYSQL_IMAGE}" \
    --character-set-server=utf8mb4 \
    --collation-server=utf8mb4_unicode_ci
else
  echo "[4/6] MySQL container already exists, ensuring it is running"
  docker start "${MYSQL_CONTAINER_NAME}" >/dev/null || true
fi

if ! docker ps -a --format '{{.Names}}' | grep -qx "${REDIS_CONTAINER_NAME}"; then
  echo "[4/6] Creating Redis container"
  docker run -d \
    --name "${REDIS_CONTAINER_NAME}" \
    --restart unless-stopped \
    -p "${REDIS_PORT}:6379" \
    -v "${REDIS_VOLUME}:/data" \
    "${REDIS_IMAGE}" \
    redis-server --appendonly yes
else
  echo "[4/6] Redis container already exists, ensuring it is running"
  docker start "${REDIS_CONTAINER_NAME}" >/dev/null || true
fi

echo "[5/6] Waiting for MySQL readiness"
for _ in $(seq 1 60); do
  if docker exec "${MYSQL_CONTAINER_NAME}" mysqladmin ping -uroot "-p${MYSQL_ROOT_PASSWORD}" --silent >/dev/null 2>&1; then
    break
  fi
  sleep 2
done

docker exec "${MYSQL_CONTAINER_NAME}" mysqladmin ping -uroot "-p${MYSQL_ROOT_PASSWORD}" --silent >/dev/null 2>&1

echo "[6/6] Initializing database schema"
docker exec -i "${MYSQL_CONTAINER_NAME}" mysql -uroot "-p${MYSQL_ROOT_PASSWORD}" "${MYSQL_DATABASE}" <<'SQL'
CREATE TABLE IF NOT EXISTS `javabase_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_no` VARCHAR(64) NOT NULL COMMENT '用户唯一编号',
    `password_hash` VARCHAR(255) DEFAULT NULL COMMENT '密码哈希',
    `nickname` VARCHAR(64) DEFAULT NULL COMMENT '昵称',
    `email` VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    `phone` VARCHAR(32) NOT NULL COMMENT '手机号',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-正常 2-待补全 3-禁用 4-冻结 5-注销',
    `register_source` VARCHAR(32) NOT NULL COMMENT '注册来源 phone/email/qq/admin_create',
    `is_phone_verified` TINYINT NOT NULL DEFAULT 0 COMMENT '手机号是否已验证',
    `is_email_verified` TINYINT NOT NULL DEFAULT 0 COMMENT '邮箱是否已验证',
    `login_fail_count` INT NOT NULL DEFAULT 0 COMMENT '连续登录失败次数',
    `login_lock_expire_time` DATETIME DEFAULT NULL COMMENT '登录失败锁定到期时间',
    `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
    `last_login_ip` VARCHAR(64) DEFAULT NULL COMMENT '最后登录IP',
    `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_javabase_user_user_no` (`user_no`),
    UNIQUE KEY `uk_javabase_user_phone` (`phone`),
    UNIQUE KEY `uk_javabase_user_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE IF NOT EXISTS `javabase_user_login_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT DEFAULT NULL COMMENT '用户ID',
    `phone` VARCHAR(32) DEFAULT NULL COMMENT '登录手机号',
    `terminal_type` VARCHAR(32) NOT NULL COMMENT '终端类型 WEB/ANDROID/IOS',
    `device_id` VARCHAR(128) NOT NULL COMMENT '设备实例ID',
    `fingerprint` VARCHAR(255) DEFAULT NULL COMMENT '设备指纹摘要',
    `device_name` VARCHAR(128) DEFAULT NULL COMMENT '设备展示名称',
    `ip` VARCHAR(64) DEFAULT NULL COMMENT '登录IP',
    `user_agent` VARCHAR(512) DEFAULT NULL COMMENT '用户代理',
    `login_status` TINYINT NOT NULL COMMENT '登录状态 1-成功 0-失败',
    `fail_reason` VARCHAR(255) DEFAULT NULL COMMENT '失败原因',
    `token_hash` VARCHAR(64) DEFAULT NULL COMMENT 'token 哈希值',
    `login_time` DATETIME DEFAULT NULL COMMENT '登录时间',
    `logout_time` DATETIME DEFAULT NULL COMMENT '退出时间',
    `kickout_time` DATETIME DEFAULT NULL COMMENT '踢下线时间',
    `kickout_reason` VARCHAR(128) DEFAULT NULL COMMENT '踢下线原因',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_javabase_user_login_log_user_time` (`user_id`, `login_time`),
    KEY `idx_javabase_user_login_log_terminal_device` (`terminal_type`, `device_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户登录日志表';

CREATE TABLE IF NOT EXISTS `javabase_user_session_device` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `terminal_type` VARCHAR(32) NOT NULL COMMENT '终端类型 WEB/ANDROID/IOS',
    `device_id` VARCHAR(128) NOT NULL COMMENT '设备实例ID',
    `fingerprint` VARCHAR(255) DEFAULT NULL COMMENT '设备指纹摘要',
    `device_name` VARCHAR(128) DEFAULT NULL COMMENT '设备展示名称',
    `token_value` VARCHAR(255) DEFAULT NULL COMMENT '当前 token 原值，仅用于精确踢设备',
    `token_hash` VARCHAR(64) DEFAULT NULL COMMENT 'token 哈希值',
    `ip` VARCHAR(64) DEFAULT NULL COMMENT '登录IP',
    `user_agent` VARCHAR(512) DEFAULT NULL COMMENT '用户代理',
    `session_status` TINYINT NOT NULL DEFAULT 1 COMMENT '会话状态 1-在线 2-已退出 3-已踢下线 4-已过期',
    `login_time` DATETIME DEFAULT NULL COMMENT '登录时间',
    `last_active_time` DATETIME DEFAULT NULL COMMENT '最近活跃时间',
    `logout_time` DATETIME DEFAULT NULL COMMENT '退出时间',
    `kickout_time` DATETIME DEFAULT NULL COMMENT '踢下线时间',
    `kickout_reason` VARCHAR(128) DEFAULT NULL COMMENT '踢下线原因',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_javabase_user_session_user_status` (`user_id`, `session_status`),
    KEY `idx_javabase_user_session_terminal_device` (`terminal_type`, `device_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户设备会话表';
SQL

cat <<EOF

Deployment completed.

MySQL:
  host: $(hostname -I | awk '{print $1}')
  port: ${MYSQL_PORT}
  database: ${MYSQL_DATABASE}
  username: ${MYSQL_USER}
  password: ${MYSQL_PASSWORD}
  root password: ${MYSQL_ROOT_PASSWORD}

Redis:
  host: $(hostname -I | awk '{print $1}')
  port: ${REDIS_PORT}

Containers:
  mysql: ${MYSQL_CONTAINER_NAME}
  redis: ${REDIS_CONTAINER_NAME}
EOF
