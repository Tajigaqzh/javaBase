package com.hp.javabase.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 阿里云短信配置，统一承载短信发送开关、地域、签名和验证码模板等参数。
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.sms.aliyun")
public class AliyunSmsProperties {

    private boolean enabled;

    private String region = "ap-southeast-1";

    private String endpoint = "dypnsapi.aliyuncs.com";

    private String signName;

    private String verifyCodeTemplateCode;

    private String accessKeyId;

    private String accessKeySecret;
}
