package com.hp.javabase.common.utils.sms;

import static org.assertj.core.api.Assertions.assertThat;

import com.hp.javabase.config.SmsConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * {@link SmsConfig} 测试，覆盖短信发送器在默认关闭和显式开启阿里云时的装配结果。
 */
class SmsConfigTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(SmsConfig.class);

    @Test
    void shouldRegisterNoopSmsSenderWhenAliyunSmsDisabled() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(SmsSender.class);
            assertThat(context.getBean(SmsSender.class)).isInstanceOf(NoopSmsSender.class);
        });
    }

    @Test
    void shouldRegisterAliyunSmsSenderWhenAliyunSmsEnabled() {
        contextRunner
                .withPropertyValues(
                        "app.sms.aliyun.enabled=true",
                        "app.sms.aliyun.sign-name=test-sign",
                        "app.sms.aliyun.verify-code-template-code=test-template",
                        "app.sms.aliyun.access-key-id=test-access-key-id",
                        "app.sms.aliyun.access-key-secret=test-access-key-secret")
                .run(context -> {
                    assertThat(context).hasSingleBean(SmsSender.class);
                    assertThat(context.getBean(SmsSender.class)).isInstanceOf(AliyunSmsSender.class);
                });
    }
}
