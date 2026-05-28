package com.hp.javabase.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * SMTP 邮件配置，统一承载邮箱验证码发送开关、发件人和邮件标题等业务参数。
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.mail.smtp")
public class SmtpMailProperties {

    private boolean enabled;

    private String from;

    private String verificationSubject = "【JavaBase】验证码";
}
