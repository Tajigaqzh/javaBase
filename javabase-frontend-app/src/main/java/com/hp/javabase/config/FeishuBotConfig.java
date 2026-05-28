package com.hp.javabase.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 飞书机器人配置入口，负责启用飞书机器人属性绑定。
 */
@Configuration
@EnableConfigurationProperties(FeishuBotProperties.class)
public class FeishuBotConfig {
}
