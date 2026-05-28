package com.hp.javabase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hp.javabase.model.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
@Mapper
public interface UserMapper extends BaseMapper<User> {

    User selectByPhone(@Param("phone") String phone);

    User selectByEmail(@Param("email") String email);
}
