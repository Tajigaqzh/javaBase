package com.hp.javabase.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 后台应用启动入口。
 *
 * <p>当前阶段仅提供独立后台应用骨架，不复用前台认证流程，也暂不启用数据库自动装配。
 * 后续后台认证、权限和数据访问能力应在该模块内按后台边界逐步补齐。
 */
@SpringBootApplication(scanBasePackages = "com.hp.javabase")
public class JavaBaseAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(JavaBaseAdminApplication.class, args);
    }
}
