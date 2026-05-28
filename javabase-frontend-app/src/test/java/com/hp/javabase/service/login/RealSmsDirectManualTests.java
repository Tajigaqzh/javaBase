package com.hp.javabase.service.login;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.hp.javabase.config.AliyunSmsProperties;
import com.hp.javabase.common.utils.sms.SmsSender;
import com.hp.javabase.common.utils.sms.AliyunSmsSender;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.util.StringUtils;

/**
 * 真实短信直连测试。
 *
 * <p>该测试不经过验证码服务与 Redis，只用于验证阿里云短信供应商配置是否可真实发送。
 */
class RealSmsDirectManualTests {

    /**
     * 直接调用阿里云短信发送器发送一条验证码短信。
     */
    @Test
    @EnabledIfSystemProperty(named = "run.real.sms.test", matches = "true")
    void shouldSendSmsDirectlyWhenExplicitlyEnabled() {
        String phone = System.getProperty("real.sms.phone");
        Assumptions.assumeTrue(StringUtils.hasText(phone), "set -Dreal.sms.phone=<target phone>");

        SmsSender smsSender = new AliyunSmsSender(createAliyunSmsProperties());
        assertDoesNotThrow(() -> smsSender.sendVerificationCode(phone, "123456", 5L));
    }

    private static AliyunSmsProperties createAliyunSmsProperties() {
        AliyunSmsProperties properties = new AliyunSmsProperties();
        properties.setRegion(System.getProperty("real.sms.region", "ap-southeast-1"));
        properties.setEndpoint(System.getProperty("real.sms.endpoint", "dypnsapi.aliyuncs.com"));
        properties.setSignName(System.getProperty("real.sms.sign-name", "速通互联验证码"));
        properties.setVerifyCodeTemplateCode(System.getProperty("real.sms.template-code", "100001"));
        properties.setAccessKeyId(System.getProperty("real.sms.access-key-id"));
        properties.setAccessKeySecret(System.getProperty("real.sms.access-key-secret"));
        return properties;
    }
}
