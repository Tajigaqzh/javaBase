package com.hp.javabase.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户实体，承载前台认证、状态控制和后续扩展所需的核心账号字段。
 */
@Getter
@Setter
@TableName("javabase_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String userNo;

    private String passwordHash;

    private String nickname;

    private String email;

    private String phone;

    private Integer status;

    private String registerSource;

    private Integer isPhoneVerified;

    private Integer isEmailVerified;

    private Integer loginFailCount;

    private LocalDateTime loginLockExpireTime;

    private LocalDateTime lastLoginTime;

    private String lastLoginIp;

    private Integer isDelete;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
