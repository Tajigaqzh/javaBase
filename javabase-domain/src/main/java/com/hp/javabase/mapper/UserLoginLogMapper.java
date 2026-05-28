package com.hp.javabase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hp.javabase.model.entity.UserLoginLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户登录日志 Mapper，负责登录审计记录的数据访问。
 */
@Mapper
public interface UserLoginLogMapper extends BaseMapper<UserLoginLog> {
}
