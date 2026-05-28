package com.hp.javabase.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 飞书机器人配置，统一承载机器人开关和 webhook 地址。
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.feishu.bot")
public class FeishuBotProperties {

    private boolean enabled;

    private String webhookUrl;
}
