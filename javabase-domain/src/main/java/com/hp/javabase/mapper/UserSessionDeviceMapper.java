package com.hp.javabase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hp.javabase.model.entity.UserSessionDevice;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户设备会话 Mapper，负责设备登录态和会话映射的数据访问。
 */
@Mapper
public interface UserSessionDeviceMapper extends BaseMapper<UserSessionDevice> {
}
