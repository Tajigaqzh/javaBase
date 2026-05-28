package com.hp.javabase.common.utils.email;

import com.hp.javabase.common.exception.BusinessException;
import com.hp.javabase.config.SmtpMailProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Properties;

/**
 * SMTP 邮件发送器，负责通过 Spring Mail 和 SMTP 服务器发送验证码邮件。
 */
@Service
@Primary
@ConditionalOnProperty(prefix = "app.mail.smtp", name = "enabled", havingValue = "true")
class SmtpMailSender implements MailSender {

    private static final Logger log = LoggerFactory.getLogger(SmtpMailSender.class);

    private final JavaMailSender javaMailSender;
    private final SmtpMailProperties properties;

    public SmtpMailSender(JavaMailSender javaMailSender, SmtpMailProperties properties) {
        this.javaMailSender = javaMailSender;
        this.properties = properties;
        validateProperties(properties);
    }

    @Override
    public void sendVerificationCode(String email, String code, long expireMinutes) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.getFrom());
        message.setTo(email);
        message.setSubject(properties.getVerificationSubject());
        message.setText(buildVerificationBody(code, expireMinutes));
        try {
            javaMailSender.send(message);
        } catch (Exception exception) {
            log.error("smtp mail send failed, email={}", email, exception);
            throw new BusinessException("mail send failed");
        }
    }

    private void validateProperties(SmtpMailProperties properties) {
        if (!StringUtils.hasText(properties.getFrom())) {
            throw new IllegalStateException("app.mail.smtp.from must not be blank when smtp mail is enabled");
        }
    }

    private String buildVerificationBody(String code, long expireMinutes) {
        return String.format(
                "您正在进行邮箱验证，验证码为%s。该验证码%d分钟内有效，请注意保密，切勿告知他人。",
                code,
                expireMinutes);
    }

    /**
     * 本地邮箱直连调试入口。
     *
     * <p>该入口用于单人开发时快速验证 SMTP 邮件配置是否可用，不依赖 Spring 容器。
     * 优先读取 src/main/resources 下的 application-secrets-local.yml、application-local.yml、application-dev.yml。
     */
    public static final class MailSendManualMain {

        private MailSendManualMain() {
        }

        /**
         * 直接调用 SMTP 邮件发送器发送测试验证码。
         *
         * <p>邮箱读取顺序：
         * 1. 程序参数 args[0]
         * 2. 系统属性 mail.to
         * 3. 环境变量 MAIL_TO
         */
        public static void main(String[] args) {
            String email = "201267151@qq.com";


            MailSender mailSender = new SmtpMailSender(createJavaMailSender(), loadSmtpMailProperties());
            mailSender.sendVerificationCode(email, "123456", 5L);
            System.out.println("mail send request submitted, email=" + email);
        }

        private static String resolveTargetEmail(String[] args) {
            if (args != null && args.length > 0 && StringUtils.hasText(args[0])) {
                return args[0].trim();
            }
            String email = System.getProperty("mail.to");
            if (StringUtils.hasText(email)) {
                return email.trim();
            }
            email = System.getenv("MAIL_TO");
            if (StringUtils.hasText(email)) {
                return email.trim();
            }
            return null;
        }

        private static SmtpMailProperties loadSmtpMailProperties() {
            Properties secretProperties = loadYamlIfExists("application-secrets-local.yml");
            Properties localProperties = loadYamlIfExists("application-local.yml");
            Properties devProperties = loadYamlIfExists("application-dev.yml");
            Properties defaultProperties = loadYamlIfExists("application.yml");

            SmtpMailProperties properties = new SmtpMailProperties();
            properties.setEnabled(true);
            properties.setFrom(resolveValue("app.mail.smtp.from", "APP_MAIL_SMTP_FROM", secretProperties, localProperties, devProperties, defaultProperties, null));
            properties.setVerificationSubject(resolveValue(
                    "app.mail.smtp.verification-subject",
                    "APP_MAIL_SMTP_VERIFICATION_SUBJECT",
                    secretProperties,
                    localProperties,
                    devProperties,
                    defaultProperties,
                    "【JavaBase】邮箱验证码"));
            return properties;
        }

        private static JavaMailSenderImpl createJavaMailSender() {
            Properties secretProperties = loadYamlIfExists("application-secrets-local.yml");
            Properties localProperties = loadYamlIfExists("application-local.yml");
            Properties devProperties = loadYamlIfExists("application-dev.yml");
            Properties defaultProperties = loadYamlIfExists("application.yml");

            JavaMailSenderImpl sender = new JavaMailSenderImpl();
            sender.setHost(resolveValue("spring.mail.host", "SPRING_MAIL_HOST", secretProperties, localProperties, devProperties, defaultProperties, null));
            sender.setPort(resolveIntValue("spring.mail.port", "SPRING_MAIL_PORT", secretProperties, localProperties, devProperties, defaultProperties, 587));
            sender.setUsername(resolveValue("spring.mail.username", "SPRING_MAIL_USERNAME", secretProperties, localProperties, devProperties, defaultProperties, null));
            sender.setPassword(resolveValue("spring.mail.password", "SPRING_MAIL_PASSWORD", secretProperties, localProperties, devProperties, defaultProperties, null));
            sender.setProtocol(resolveValue("spring.mail.protocol", "SPRING_MAIL_PROTOCOL", secretProperties, localProperties, devProperties, defaultProperties, "smtp"));
            sender.setDefaultEncoding(resolveValue("spring.mail.default-encoding", "SPRING_MAIL_DEFAULT_ENCODING", secretProperties, localProperties, devProperties, defaultProperties, "UTF-8"));

            Properties javaMailProperties = sender.getJavaMailProperties();
            javaMailProperties.setProperty(
                    "mail.smtp.auth",
                    resolveValue("spring.mail.properties.mail.smtp.auth", "SPRING_MAIL_SMTP_AUTH", secretProperties, localProperties, devProperties, defaultProperties, "true"));
            javaMailProperties.setProperty(
                    "mail.smtp.starttls.enable",
                    resolveValue("spring.mail.properties.mail.smtp.starttls.enable", "SPRING_MAIL_SMTP_STARTTLS_ENABLE", secretProperties, localProperties, devProperties, defaultProperties, "false"));
            javaMailProperties.setProperty(
                    "mail.smtp.ssl.enable",
                    resolveValue("spring.mail.properties.mail.smtp.ssl.enable", "SPRING_MAIL_SMTP_SSL_ENABLE", secretProperties, localProperties, devProperties, defaultProperties, "false"));
            return sender;
        }

        private static int resolveIntValue(
                String propertyKey,
                String environmentKey,
                Properties secretProperties,
                Properties localProperties,
                Properties devProperties,
                Properties defaultProperties,
                int defaultValue) {
            String value = resolveValue(propertyKey, environmentKey, secretProperties, localProperties, devProperties, defaultProperties, null);
            if (!StringUtils.hasText(value)) {
                return defaultValue;
            }
            return Integer.parseInt(value);
        }

        private static String resolveValue(
                String propertyKey,
                String environmentKey,
                Properties secretProperties,
                Properties localProperties,
                Properties devProperties,
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
            String secretValue = secretProperties.getProperty(propertyKey);
            if (StringUtils.hasText(secretValue)) {
                return normalizePropertyValue(secretValue.trim());
            }
            String localValue = localProperties.getProperty(propertyKey);
            if (StringUtils.hasText(localValue)) {
                return normalizePropertyValue(localValue.trim());
            }
            String devValue = devProperties.getProperty(propertyKey);
            if (StringUtils.hasText(devValue)) {
                return normalizePropertyValue(devValue.trim());
            }
            String defaultConfigValue = defaultProperties.getProperty(propertyKey);
            if (StringUtils.hasText(defaultConfigValue)) {
                return normalizePropertyValue(defaultConfigValue.trim());
            }
            return defaultValue;
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
}