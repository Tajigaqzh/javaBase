package com.hp.javabase.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户登录日志实体，记录登录请求的结果、设备信息和会话相关审计信息。
 */
@Getter
@Setter
@TableName("javabase_user_login_log")
public class UserLoginLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String phone;

    private String terminalType;

    private String deviceId;

    private String fingerprint;

    private String deviceName;

    private String ip;

    private String userAgent;

    private Integer loginStatus;

    private String failReason;

    private String tokenHash;

    private LocalDateTime loginTime;

    private LocalDateTime logoutTime;

    private LocalDateTime kickoutTime;

    private String kickoutReason;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
