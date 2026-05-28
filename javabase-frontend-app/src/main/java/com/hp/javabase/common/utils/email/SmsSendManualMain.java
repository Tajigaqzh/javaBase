package com.hp.javabase.common.utils.email;


import com.hp.javabase.config.AliyunSmsProperties;
import com.hp.javabase.common.utils.sms.SmsSender;
import com.hp.javabase.common.utils.sms.AliyunSmsSender;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

import java.util.Properties;

/**
 * 本地短信直连调试入口。
 *
 * <p>该入口用于单人开发时快速验证阿里云短信配置是否可用，不依赖 Spring 容器、Redis 或 Mockito。
 * 优先读取 src/main/resources 下的 application-secrets-local.yml 和 application-local.yml，
 * 本地文件未配置的项再回退到系统属性和环境变量。
 */
public final class SmsSendManualMain {

    private SmsSendManualMain() {
    }

    /**
     * 直接调用阿里云短信发送器发送测试验证码。
     *
     * <p>手机号读取顺序：
     * 1. 程序参数 args[0]
     * 2. 系统属性 sms.phone
     * 3. 环境变量 SMS_PHONE
     */
    public static void main(String[] args) {
        String phone = resolvePhone(args);
        if (!StringUtils.hasText(phone)) {
            throw new IllegalArgumentException("missing target phone, pass args[0] or -Dsms.phone=<phone>");
        }

        AliyunSmsProperties properties = loadAliyunSmsProperties();
        SmsSender smsSender = new AliyunSmsSender(properties);
        smsSender.sendVerificationCode(phone, "123456", 5L);
        System.out.println("sms send request submitted, phone=" + phone);
    }

    private static String resolvePhone(String[] args) {
        if (args != null && args.length > 0 && StringUtils.hasText(args[0])) {
            return args[0].trim();
        }
        String phone = System.getProperty("sms.phone");
        if (StringUtils.hasText(phone)) {
            return phone.trim();
        }
        phone = System.getenv("SMS_PHONE");
        if (StringUtils.hasText(phone)) {
            return phone.trim();
        }
        return null;
    }

    private static AliyunSmsProperties loadAliyunSmsProperties() {
        Properties localSecretProperties = loadYamlIfExists("application-secrets-local.yml");
        Properties localProperties = loadYamlIfExists("application-local.yml");
        Properties defaultProperties = loadYamlIfExists("application.yml");

        AliyunSmsProperties properties = new AliyunSmsProperties();
        properties.setEnabled(true);
        properties.setRegion(resolveValue("app.sms.aliyun.region", "ALIYUN_SMS_REGION", localSecretProperties, localProperties, defaultProperties, "ap-southeast-1"));
        properties.setEndpoint(resolveValue("app.sms.aliyun.endpoint", "ALIYUN_SMS_ENDPOINT", localSecretProperties, localProperties, defaultProperties, "dypnsapi.aliyuncs.com"));
        properties.setSignName(resolveValue("app.sms.aliyun.sign-name", "ALIYUN_SMS_SIGN_NAME", localSecretProperties, localProperties, defaultProperties, null));
        properties.setVerifyCodeTemplateCode(resolveValue(
                "app.sms.aliyun.verify-code-template-code",
                "ALIYUN_SMS_VERIFY_CODE_TEMPLATE_CODE",
                localSecretProperties,
                localProperties,
                defaultProperties,
                null));
        properties.setAccessKeyId(resolveValue(
                "app.sms.aliyun.access-key-id",
                "ALIYUN_SMS_ACCESS_KEY_ID",
                localSecretProperties,
                localProperties,
                defaultProperties,
                null));
        properties.setAccessKeySecret(resolveValue(
                "app.sms.aliyun.access-key-secret",
                "ALIYUN_SMS_ACCESS_KEY_SECRET",
                localSecretProperties,
                localProperties,
                defaultProperties,
                null));
        return properties;
    }

    private static Properties loadYamlIfExists(String resourcePath) {
        Resource resource = new ClassPathResource(resourcePath);
        if (!resource.exists()) {
            return new Properties();
        }
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(resource);
        Properties properties = factory.getObject();
        return properties == null ? new Properties() : properties;
    }

    private static String resolveValue(
            String propertyKey,
            String environmentKey,
            Properties localSecretProperties,
            Properties localProperties,
            Properties defaultProperties,
            String defaultValue) {
        String systemValue = System.getProperty(propertyKey);
        if (StringUtils.hasText(systemValue)) {
            return systemValue.trim();
        }
        String environmentValue = System.getenv(environmentKey);
        if (StringUtils.hasText(environmentValue)) {
            return environmentValue.trim();
        }
        String localSecretValue = localSecretProperties.getProperty(propertyKey);
        if (StringUtils.hasText(localSecretValue)) {
            return normalizePropertyValue(localSecretValue.trim());
        }
        String localValue = localProperties.getProperty(propertyKey);
        if (StringUtils.hasText(localValue)) {
            return normalizePropertyValue(localValue.trim());
        }
        String defaultConfigValue = defaultProperties.getProperty(propertyKey);
        if (StringUtils.hasText(defaultConfigValue)) {
            return normalizePropertyValue(defaultConfigValue.trim());
        }
        return defaultValue;
    }

    private static String normalizePropertyValue(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        if (!value.startsWith("${") || !value.endsWith("}")) {
            return value;
        }
        int separatorIndex = value.indexOf(':');
        if (separatorIndex < 0 || separatorIndex >= value.length() - 2) {
            return value;
        }
        return value.substring(separatorIndex + 1, value.length() - 1).trim();
    }
}

