package com.hp.javabase;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 前台应用启动入口，负责装配前台接口所需的控制器、服务、配置和数据访问能力。
 */
@SpringBootApplication
@MapperScan("com.hp.javabase.mapper")
public class JavaBaseApplication {

    public static void main(String[] args) {
        SpringApplication.run(JavaBaseApplication.class, args);
    }

}
