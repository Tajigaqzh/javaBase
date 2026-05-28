package com.hp.javabase.common.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.hp.javabase.common.utils.oss.AliyunOssStsUtils;
import com.hp.javabase.common.utils.oss.ObjectStorageUploadTokenUtils;
import com.hp.javabase.config.ObjectStorageProperties;
import com.hp.javabase.model.enums.ObjectStorageProviderEnum;
import com.hp.javabase.model.vo.ObjectStorageUploadTokenVO;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * {@link ObjectStorageUploadTokenUtils} 单元测试，覆盖上传目录、上传 host 和统一临时凭证协议的组装行为。
 */
class ObjectStorageUploadTokenUtilsTests {

    @Test
    void createUploadTokenShouldBuildAliyunStsResultForCurrentUser() {
        ObjectStorageProperties properties = new ObjectStorageProperties();
        properties.setProvider("ALIYUN_OSS");
        properties.setUploadDirPrefix("upload");
        properties.getAliyun().setBucket("javabase-dev");
        properties.getAliyun().setRegion("cn-hangzhou");
        properties.getAliyun().setEndpoint("oss-cn-hangzhou.aliyuncs.com");
        properties.getAliyun().setRoleArn("acs:ram::1234567890123456:role/javabase-upload-role");
        properties.getAliyun().setRoleSessionNamePrefix("javabase-upload");
        properties.getAliyun().setDurationSeconds(900L);

        AliyunOssStsUtils aliyunOssStsUtils = Mockito.mock(AliyunOssStsUtils.class);
        when(aliyunOssStsUtils.assumeRole(anyString(), anyString(), anyString(), anyLong()))
                .thenReturn(AliyunOssStsUtils.AliyunStsCredentials.builder()
                        .accessKeyId("STS_ID")
                        .accessKeySecret("STS_SECRET")
                        .securityToken("STS_TOKEN")
                        .expiration("2026-05-28T08:30:00Z")
                        .build());

        ObjectStorageUploadTokenUtils utils = new ObjectStorageUploadTokenUtils(properties, aliyunOssStsUtils);
        ObjectStorageUploadTokenVO token = utils.createUploadToken(10001L);

        assertEquals(ObjectStorageProviderEnum.ALIYUN_OSS, token.getProvider());
        assertEquals("STS", token.getCredentialType());
        assertEquals("javabase-dev", token.getBucket());
        assertEquals("cn-hangzhou", token.getRegion());
        assertEquals("https://javabase-dev.oss-cn-hangzhou.aliyuncs.com", token.getUploadHost());
        assertTrue(token.getObjectKeyPrefix().startsWith("upload/10001/"));
        assertEquals("STS_ID", token.getAccessKeyId());
        assertEquals("STS_SECRET", token.getAccessKeySecret());
        assertEquals("STS_TOKEN", token.getSecurityToken());
        assertEquals("2026-05-28T08:30:00Z", token.getExpiration());
    }
}
