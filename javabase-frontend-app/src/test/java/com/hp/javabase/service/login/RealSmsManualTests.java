package com.hp.javabase.service.login;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.hp.javabase.config.AliyunSmsProperties;
import com.hp.javabase.model.enums.VerificationSceneEnum;
import com.hp.javabase.model.enums.VerificationTargetTypeEnum;
import com.hp.javabase.model.vo.VerificationCodeSendVO;
import com.hp.javabase.common.utils.email.MailSender;
import com.hp.javabase.common.utils.sms.SmsSender;
import com.hp.javabase.common.utils.sms.AliyunSmsSender;
import com.hp.javabase.common.utils.email.NoopMailSender;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.util.StringUtils;

/**
 * 手工真实短信发送测试。
 *
 * <p>该测试默认不会执行，只有在显式传入系统属性后才会调用阿里云真实发送短信，
 * 用于上线前联调短信供应商配置，不参与日常单元测试门禁。
 */
class RealSmsManualTests {

    /**
     * 真实调用阿里云短信接口发送验证码。
     *
     * <p>运行前需要准备：
     * 1. 本机可用的阿里云凭证，供 DefaultCredentialProvider 读取；
     * 2. 系统属性 -Drun.real.sms.test=true；
     * 3. 系统属性 -Dreal.sms.phone=目标手机号。
     */
    @Test
    @EnabledIfSystemProperty(named = "run.real.sms.test", matches = "true")
    void sendPhoneCodeShouldReallySendSmsWhenExplicitlyEnabled() {
        String phone = System.getProperty("real.sms.phone");
        Assumptions.assumeTrue(StringUtils.hasText(phone), "set -Dreal.sms.phone=<target phone>");

        VerificationTests.FakeRedisSupport fakeRedisSupport = new VerificationTests.FakeRedisSupport();
        SmsSender smsSender = new AliyunSmsSender(createAliyunSmsProperties());
        MailSender mailSender = new NoopMailSender();
        VerificationCodeUtils service = new VerificationCodeUtils(
                fakeRedisSupport.createClient(),
                smsSender,
                mailSender,
                true);

        VerificationCodeSendVO response = service.sendPhoneCode(
                phone,
                VerificationSceneEnum.LOGIN,
                "127.0.0.1");

        assertNotNull(response.getDebugCode());
        assertEquals(6, response.getDebugCode().length());
        assertEquals(
                response.getDebugCode(),
                fakeRedisSupport.getValue(codeKey(VerificationTargetTypeEnum.PHONE, VerificationSceneEnum.LOGIN, phone)));
    }

    private static String codeKey(VerificationTargetTypeEnum targetType, VerificationSceneEnum scene, String target) {
        return "auth:verification:code:" + targetType.name() + ":" + scene + ":" + target;
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
