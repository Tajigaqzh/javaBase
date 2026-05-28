package com.hp.javabase.common.utils.sms;

import com.aliyun.auth.credentials.Credential;
import com.aliyun.auth.credentials.provider.DefaultCredentialProvider;
import com.aliyun.auth.credentials.provider.ICredentialProvider;
import com.aliyun.auth.credentials.provider.StaticCredentialProvider;
import com.aliyun.sdk.service.dypnsapi20170525.AsyncClient;
import com.aliyun.sdk.service.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.hp.javabase.common.exception.BusinessException;
import com.hp.javabase.config.AliyunSmsProperties;
import darabonba.core.client.ClientOverrideConfiguration;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * 阿里云短信发送器，负责将验证码通过阿里云 Dypnsapi 下发到用户手机号。
 */
public class AliyunSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(AliyunSmsSender.class);

    private final AliyunSmsProperties properties;
    private final AsyncClient asyncClient;

    public AliyunSmsSender(AliyunSmsProperties properties) {
        this.properties = properties;
        validateProperties(properties);
        this.asyncClient = AsyncClient.builder()
                .region(properties.getRegion())
                .credentialsProvider(createCredentialProvider(properties))
                .overrideConfiguration(ClientOverrideConfiguration.create()
                        .setEndpointOverride(properties.getEndpoint()))
                .build();
    }

    @Override
    public void sendVerificationCode(String phone, String code, long expireMinutes) {
        String templateParam = String.format("{\"code\":\"%s\",\"min\":\"%d\"}", code, expireMinutes);
        SendSmsVerifyCodeRequest request = SendSmsVerifyCodeRequest.builder()
                .signName(properties.getSignName())
                .templateCode(properties.getVerifyCodeTemplateCode())
                .phoneNumber(phone)
                .templateParam(templateParam)
                .build();
        try {
            asyncClient.sendSmsVerifyCode(request).get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException("sms send interrupted");
        } catch (ExecutionException exception) {
            log.error("aliyun sms send failed, phone={}", phone, exception);
            throw new BusinessException("sms send failed");
        }
    }

    private void validateProperties(AliyunSmsProperties properties) {
        if (!StringUtils.hasText(properties.getSignName())) {
            throw new IllegalStateException("app.sms.aliyun.sign-name must not be blank when aliyun sms is enabled");
        }
        if (!StringUtils.hasText(properties.getVerifyCodeTemplateCode())) {
            throw new IllegalStateException("app.sms.aliyun.verify-code-template-code must not be blank when aliyun sms is enabled");
        }
        boolean hasAccessKeyId = StringUtils.hasText(properties.getAccessKeyId());
        boolean hasAccessKeySecret = StringUtils.hasText(properties.getAccessKeySecret());
        if (hasAccessKeyId != hasAccessKeySecret) {
            throw new IllegalStateException("app.sms.aliyun.access-key-id and access-key-secret must be configured together");
        }
    }

    private ICredentialProvider createCredentialProvider(AliyunSmsProperties properties) {
        if (StringUtils.hasText(properties.getAccessKeyId())) {
            return StaticCredentialProvider.create(Credential.builder()
                    .accessKeyId(properties.getAccessKeyId())
                    .accessKeySecret(properties.getAccessKeySecret())
                    .build());
        }
        return DefaultCredentialProvider.builder().build();
    }

    @PreDestroy
    public void destroy() throws Exception {
        asyncClient.close();
    }
}
