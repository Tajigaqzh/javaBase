package com.hp.javabase.common.utils.oss;

import com.hp.javabase.config.ObjectStorageProperties;
import com.hp.javabase.model.vo.ObjectStorageUploadTokenVO;
import java.util.Properties;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

/**
 * 本地对象存储上传凭证调试入口。
 *
 * <p>该入口用于单人开发时快速验证阿里云 OSS STS 配置是否可用，不依赖 Spring 容器。
 */
public final class ObjectStorageUploadTokenManualMain {

    private ObjectStorageUploadTokenManualMain() {
    }

    public static void main(String[] args) {
        Long userId = resolveUserId(args);
        ObjectStorageProperties properties = loadObjectStorageProperties();
        ObjectStorageUploadTokenUtils utils = new ObjectStorageUploadTokenUtils(
                properties,
                new AliyunOssStsUtils(properties));
        ObjectStorageUploadTokenVO token = utils.createUploadToken(userId);
        System.out.println("provider=" + token.getProvider());
        System.out.println("bucket=" + token.getBucket());
        System.out.println("region=" + token.getRegion());
        System.out.println("endpoint=" + token.getEndpoint());
        System.out.println("uploadHost=" + token.getUploadHost());
        System.out.println("objectKeyPrefix=" + token.getObjectKeyPrefix());
        System.out.println("accessKeyId=" + token.getAccessKeyId());
        System.out.println("securityToken=" + token.getSecurityToken());
        System.out.println("expiration=" + token.getExpiration());
    }

    private static Long resolveUserId(String[] args) {
        if (args != null && args.length > 0 && StringUtils.hasText(args[0])) {
            return Long.parseLong(args[0].trim());
        }
        String userId = System.getProperty("storage.user-id");
        if (StringUtils.hasText(userId)) {
            return Long.parseLong(userId.trim());
        }
        return 10001L;
    }

    private static ObjectStorageProperties loadObjectStorageProperties() {
        Properties secretProperties = loadYamlIfExists("application-secrets-local.yml");
        Properties localProperties = loadYamlIfExists("application-local.yml");
        Properties defaultProperties = loadYamlIfExists("application.yml");

        ObjectStorageProperties properties = new ObjectStorageProperties();
        properties.setProvider(resolveValue("app.storage.provider", secretProperties, localProperties, defaultProperties, "ALIYUN_OSS"));
        properties.setUploadDirPrefix(resolveValue("app.storage.upload-dir-prefix", secretProperties, localProperties, defaultProperties, "upload"));
        properties.getAliyun().setRegion(resolveValue("app.storage.aliyun.region", secretProperties, localProperties, defaultProperties, "cn-beijing"));
        properties.getAliyun().setEndpoint(resolveValue("app.storage.aliyun.endpoint", secretProperties, localProperties, defaultProperties, "oss-cn-beijing.aliyuncs.com"));
        properties.getAliyun().setStsEndpoint(resolveValue("app.storage.aliyun.sts-endpoint", secretProperties, localProperties, defaultProperties, "sts.cn-beijing.aliyuncs.com"));
        properties.getAliyun().setBucket(resolveValue("app.storage.aliyun.bucket", secretProperties, localProperties, defaultProperties, null));
        properties.getAliyun().setRoleArn(resolveValue("app.storage.aliyun.role-arn", secretProperties, localProperties, defaultProperties, null));
        properties.getAliyun().setRoleSessionNamePrefix(resolveValue("app.storage.aliyun.role-session-name-prefix", secretProperties, localProperties, defaultProperties, "javabase-upload"));
        properties.getAliyun().setDurationSeconds(Long.parseLong(resolveValue("app.storage.aliyun.duration-seconds", secretProperties, localProperties, defaultProperties, "900")));
        properties.getAliyun().setPublicHost(resolveValue("app.storage.aliyun.public-host", secretProperties, localProperties, defaultProperties, null));
        properties.getAliyun().setAccessKeyId(resolveValue("app.storage.aliyun.access-key-id", secretProperties, localProperties, defaultProperties, null));
        properties.getAliyun().setAccessKeySecret(resolveValue("app.storage.aliyun.access-key-secret", secretProperties, localProperties, defaultProperties, null));
        return properties;
    }

    private static String resolveValue(
            String propertyKey,
            Properties secretProperties,
            Properties localProperties,
            Properties defaultProperties,
            String defaultValue) {
        String systemValue = System.getProperty(propertyKey);
        if (StringUtils.hasText(systemValue)) {
            return systemValue.trim();
        }
        String secretValue = secretProperties.getProperty(propertyKey);
        if (StringUtils.hasText(secretValue)) {
            return normalizePropertyValue(secretValue.trim());
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
