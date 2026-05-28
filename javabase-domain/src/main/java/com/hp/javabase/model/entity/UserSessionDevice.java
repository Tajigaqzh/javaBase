package com.hp.javabase.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户设备会话实体，用于维护当前或最近一次登录设备与 token 的映射关系。
 */
@Getter
@Setter
@TableName("javabase_user_session_device")
public class UserSessionDevice {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String terminalType;

    private String deviceId;

    private String fingerprint;

    private String deviceName;

    private String tokenValue;

    private String tokenHash;

    private String ip;

    private String userAgent;

    private Integer sessionStatus;

    private LocalDateTime loginTime;

    private LocalDateTime lastActiveTime;

    private LocalDateTime logoutTime;

    private LocalDateTime kickoutTime;

    private String kickoutReason;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
