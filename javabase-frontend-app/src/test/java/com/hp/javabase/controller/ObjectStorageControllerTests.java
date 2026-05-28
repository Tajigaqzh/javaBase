package com.hp.javabase.controller;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hp.javabase.model.enums.ObjectStorageProviderEnum;
import com.hp.javabase.model.vo.ObjectStorageUploadTokenVO;
import com.hp.javabase.service.ObjectStorageService;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import cn.dev33.satoken.stp.StpUtil;

/**
 * {@link ObjectStorageController} 控制器测试，覆盖上传临时凭证接口的基础委派行为。
 */
class ObjectStorageControllerTests {

    @Test
    void getUploadTokenShouldDelegateToService() {
        ObjectStorageService objectStorageService = Mockito.mock(ObjectStorageService.class);
        ObjectStorageController controller = new ObjectStorageController(objectStorageService);
        ObjectStorageUploadTokenVO response = ObjectStorageUploadTokenVO.builder()
                .provider(ObjectStorageProviderEnum.ALIYUN_OSS)
                .credentialType("STS")
                .bucket("javabase-dev")
                .region("cn-hangzhou")
                .endpoint("oss-cn-hangzhou.aliyuncs.com")
                .uploadHost("https://javabase-dev.oss-cn-hangzhou.aliyuncs.com")
                .objectKeyPrefix("upload/10001/20260528/")
                .accessKeyId("STS_ID")
                .accessKeySecret("STS_SECRET")
                .securityToken("STS_TOKEN")
                .expiration("2026-05-28T08:30:00Z")
                .build();
        when(objectStorageService.createUploadToken(10001L)).thenReturn(response);

        try (MockedStatic<StpUtil> mockedStatic = Mockito.mockStatic(StpUtil.class)) {
            mockedStatic.when(StpUtil::getLoginIdAsLong).thenReturn(10001L);

            assertSame(response, controller.getUploadToken().getData());
        }

        verify(objectStorageService).createUploadToken(10001L);
    }
}
