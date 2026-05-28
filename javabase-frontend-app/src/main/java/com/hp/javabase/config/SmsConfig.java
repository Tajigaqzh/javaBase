package com.hp.javabase.config;

import com.hp.javabase.common.utils.sms.AliyunSmsSender;
import com.hp.javabase.common.utils.sms.NoopSmsSender;
import com.hp.javabase.common.utils.sms.SmsSender;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 短信配置入口，负责启用短信供应商相关配置属性绑定。
 */
@Configuration
@EnableConfigurationProperties({AliyunSmsProperties.class, SmtpMailProperties.class})
public class SmsConfig {

    @Bean
    @ConditionalOnProperty(prefix = "app.sms.aliyun", name = "enabled", havingValue = "true")
    SmsSender aliyunSmsSender(AliyunSmsProperties properties) {
        return new AliyunSmsSender(properties);
    }

    @Bean
    @ConditionalOnMissingBean(SmsSender.class)
    SmsSender noopSmsSender() {
        return new NoopSmsSender();
    }
}
