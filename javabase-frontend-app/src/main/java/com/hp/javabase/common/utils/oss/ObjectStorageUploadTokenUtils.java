package com.hp.javabase.common.utils.oss;

import com.hp.javabase.common.exception.BusinessException;
import com.hp.javabase.config.ObjectStorageProperties;
import com.hp.javabase.model.enums.ObjectStorageProviderEnum;
import com.hp.javabase.model.vo.ObjectStorageUploadTokenVO;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 对象存储上传凭证工具，统一对外提供供应商无关的临时上传凭证结构。
 *
 * <p>当前阶段仅落阿里云 OSS + STS 实现，但控制器只依赖本工具，后续切换腾讯云 COS 或七牛时只需扩展内部实现。
 */
@Component
public class ObjectStorageUploadTokenUtils {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final ObjectStorageProperties properties;
    private final AliyunOssStsUtils aliyunOssStsUtils;

    public ObjectStorageUploadTokenUtils(
            ObjectStorageProperties properties,
            AliyunOssStsUtils aliyunOssStsUtils) {
        this.properties = properties;
        this.aliyunOssStsUtils = aliyunOssStsUtils;
    }

    /**
     * 为当前登录用户生成一份上传临时凭证。
     *
     * @param userId 当前登录用户 ID
     * @return 上传临时凭证
     */
    public ObjectStorageUploadTokenVO createUploadToken(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        ObjectStorageProviderEnum provider = parseProvider(properties.getProvider());
        if (provider != ObjectStorageProviderEnum.ALIYUN_OSS) {
            throw new BusinessException("unsupported object storage provider");
        }
        return createAliyunUploadToken(userId);
    }

    /**
     * 基于阿里云 STS 为当前用户签发一份仅允许上传本人目录的临时凭证。
     */
    private ObjectStorageUploadTokenVO createAliyunUploadToken(Long userId) {
        validateAliyunConfig();
        String objectKeyPrefix = buildObjectKeyPrefix(userId);
        String sessionName = buildRoleSessionName(userId);
        String policy = buildAliyunUploadPolicy(objectKeyPrefix);
        AliyunOssStsUtils.AliyunStsCredentials credentials = aliyunOssStsUtils.assumeRole(
                properties.getAliyun().getRoleArn(),
                sessionName,
                policy,
                properties.getAliyun().getDurationSeconds());
        return ObjectStorageUploadTokenVO.builder()
                .provider(ObjectStorageProviderEnum.ALIYUN_OSS)
                .credentialType("STS")
                .bucket(properties.getAliyun().getBucket())
                .region(properties.getAliyun().getRegion())
                .endpoint(properties.getAliyun().getEndpoint())
                .uploadHost(resolveUploadHost())
                .objectKeyPrefix(objectKeyPrefix)
                .accessKeyId(credentials.getAccessKeyId())
                .accessKeySecret(credentials.getAccessKeySecret())
                .securityToken(credentials.getSecurityToken())
                .expiration(credentials.getExpiration())
                .build();
    }

    private ObjectStorageProviderEnum parseProvider(String provider) {
        if (!StringUtils.hasText(provider)) {
            throw new IllegalStateException("app.storage.provider must not be blank");
        }
        return ObjectStorageProviderEnum.valueOf(provider.trim().toUpperCase(Locale.ROOT));
    }

    private void validateAliyunConfig() {
        if (!StringUtils.hasText(properties.getAliyun().getBucket())) {
            throw new IllegalStateException("app.storage.aliyun.bucket must not be blank");
        }
        if (!StringUtils.hasText(properties.getAliyun().getEndpoint())) {
            throw new IllegalStateException("app.storage.aliyun.endpoint must not be blank");
        }
        if (!StringUtils.hasText(properties.getAliyun().getRegion())) {
            throw new IllegalStateException("app.storage.aliyun.region must not be blank");
        }
        if (!StringUtils.hasText(properties.getAliyun().getRoleArn())) {
            throw new IllegalStateException("app.storage.aliyun.role-arn must not be blank");
        }
    }

    private String buildObjectKeyPrefix(Long userId) {
        String baseDir = normalizeDirPrefix(properties.getUploadDirPrefix());
        return baseDir + userId + "/" + LocalDate.now().format(DATE_FORMATTER) + "/";
    }

    private String buildRoleSessionName(Long userId) {
        String prefix = StringUtils.hasText(properties.getAliyun().getRoleSessionNamePrefix())
                ? properties.getAliyun().getRoleSessionNamePrefix().trim()
                : "javabase-upload";
        String rawValue = prefix + "-" + userId + "-" + System.currentTimeMillis();
        String sanitized = rawValue.replaceAll("[^a-zA-Z0-9-]", "-");
        return sanitized.length() > 64 ? sanitized.substring(0, 64) : sanitized;
    }

    private String buildAliyunUploadPolicy(String objectKeyPrefix) {
        String objectResource = "acs:oss:*:*:" + properties.getAliyun().getBucket() + "/" + objectKeyPrefix + "*";
        return """
                {
                  "Version": "1",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Action": [
                        "oss:PutObject",
                        "oss:AbortMultipartUpload",
                        "oss:InitiateMultipartUpload",
                        "oss:UploadPart",
                        "oss:CompleteMultipartUpload",
                        "oss:ListParts"
                      ],
                      "Resource": [
                        "%s"
                      ]
                    }
                  ]
                }
                """.formatted(objectResource);
    }

    private String resolveUploadHost() {
        if (StringUtils.hasText(properties.getAliyun().getPublicHost())) {
            return properties.getAliyun().getPublicHost().trim();
        }
        return "https://" + properties.getAliyun().getBucket() + "." + properties.getAliyun().getEndpoint();
    }

    private String normalizeDirPrefix(String dirPrefix) {
        if (!StringUtils.hasText(dirPrefix)) {
            return "";
        }
        String normalized = dirPrefix.trim();
        normalized = normalized.startsWith("/") ? normalized.substring(1) : normalized;
        normalized = normalized.endsWith("/") ? normalized : normalized + "/";
        return normalized;
    }
}
