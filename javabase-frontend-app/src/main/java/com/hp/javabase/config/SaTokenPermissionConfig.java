package com.hp.javabase.config;

import cn.dev33.satoken.stp.StpInterface;
import com.hp.javabase.model.entity.User;
import com.hp.javabase.service.AuthService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.List;

@Configuration
public class SaTokenPermissionConfig {

    @Bean
    public StpInterface stpInterface(AuthService authService) {
        return new StpInterface() {
            @Override
            public List<String> getPermissionList(Object loginId, String loginType) {
                User user = authService.findByLoginId(loginId);
                if (user == null) {
                    return Collections.emptyList();
                }
                return List.of("user:read", "ai:chat");
            }

            @Override
            public List<String> getRoleList(Object loginId, String loginType) {
                User user = authService.findByLoginId(loginId);
                if (user == null) {
                    return Collections.emptyList();
                }
                return List.of("user");
            }
        };
    }
}
