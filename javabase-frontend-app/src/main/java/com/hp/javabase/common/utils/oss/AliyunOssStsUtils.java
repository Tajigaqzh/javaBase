package com.hp.javabase.common.utils.oss;

import com.aliyun.sts20150401.Client;
import com.aliyun.sts20150401.models.AssumeRoleRequest;
import com.aliyun.sts20150401.models.AssumeRoleResponse;
import com.aliyun.teaopenapi.models.Config;
import com.hp.javabase.common.exception.BusinessException;
import com.hp.javabase.config.ObjectStorageProperties;
import lombok.Builder;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 阿里云 OSS STS 工具，负责与阿里云 STS SDK 交互并换取临时上传凭证。
 */
@Component
public class AliyunOssStsUtils {

    private static final Logger log = LoggerFactory.getLogger(AliyunOssStsUtils.class);

    private final ObjectStorageProperties properties;

    public AliyunOssStsUtils(ObjectStorageProperties properties) {
        this.properties = properties;
    }

    /**
     * 使用 AssumeRole 模式申请一份临时上传凭证。
     *
     * @param roleArn 目标角色 ARN
     * @param roleSessionName 会话名称
     * @param policy 上传策略 JSON
     * @param durationSeconds 过期秒数
     * @return 阿里云 STS 临时凭证
     */
    public AliyunStsCredentials assumeRole(
            String roleArn,
            String roleSessionName,
            String policy,
            long durationSeconds) {
        validateClientConfig();
        try {
            Config config = new Config()
                    .setAccessKeyId(properties.getAliyun().getAccessKeyId())
                    .setAccessKeySecret(properties.getAliyun().getAccessKeySecret())
                    .setEndpoint(properties.getAliyun().getStsEndpoint());
            Client client = new Client(config);
            AssumeRoleRequest request = new AssumeRoleRequest()
                    .setRoleArn(roleArn)
                    .setRoleSessionName(roleSessionName)
                    .setPolicy(policy)
                    .setDurationSeconds(durationSeconds);
            AssumeRoleResponse response = client.assumeRole(request);
            if (response.getBody() == null || response.getBody().getCredentials() == null) {
                throw new BusinessException("sts credentials response is empty");
            }
            return AliyunStsCredentials.builder()
                    .accessKeyId(response.getBody().getCredentials().getAccessKeyId())
                    .accessKeySecret(response.getBody().getCredentials().getAccessKeySecret())
                    .securityToken(response.getBody().getCredentials().getSecurityToken())
                    .expiration(response.getBody().getCredentials().getExpiration())
                    .build();
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("aliyun sts assume role failed, roleArn={}", roleArn, exception);
            throw new BusinessException("object storage sts token create failed");
        }
    }

    private void validateClientConfig() {
        if (!StringUtils.hasText(properties.getAliyun().getAccessKeyId())) {
            throw new IllegalStateException("app.storage.aliyun.access-key-id must not be blank");
        }
        if (!StringUtils.hasText(properties.getAliyun().getAccessKeySecret())) {
            throw new IllegalStateException("app.storage.aliyun.access-key-secret must not be blank");
        }
        if (!StringUtils.hasText(properties.getAliyun().getStsEndpoint())) {
            throw new IllegalStateException("app.storage.aliyun.sts-endpoint must not be blank");
        }
    }

    /**
     * 阿里云 STS 临时凭证。
     */
    @Getter
    @Builder
    public static class AliyunStsCredentials {

        private final String accessKeyId;

        private final String accessKeySecret;

        private final String securityToken;

        private final String expiration;
    }
}
