package com.hp.javabase.model.vo;

import com.hp.javabase.model.enums.ObjectStorageProviderEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

/**
 * 对象存储上传临时凭证视图，统一承载前端直传所需的公共元信息与 STS 临时密钥。
 */
@Getter
@Builder
public class ObjectStorageUploadTokenVO {

    @Schema(description = "对象存储供应商", example = "ALIYUN_OSS")
    private final ObjectStorageProviderEnum provider;

    @Schema(description = "凭证模式，当前阶段固定为 STS", example = "STS")
    private final String credentialType;

    @Schema(description = "对象存储桶名称", example = "javabase-dev")
    private final String bucket;

    @Schema(description = "对象存储所属地域", example = "cn-hangzhou")
    private final String region;

    @Schema(description = "对象存储访问 endpoint", example = "oss-cn-hangzhou.aliyuncs.com")
    private final String endpoint;

    @Schema(description = "前端上传目标 host", example = "https://javabase-dev.oss-cn-hangzhou.aliyuncs.com")
    private final String uploadHost;

    @Schema(description = "当前用户允许上传的对象 key 前缀", example = "upload/10001/20260528/")
    private final String objectKeyPrefix;

    @Schema(description = "临时 AccessKeyId")
    private final String accessKeyId;

    @Schema(description = "临时 AccessKeySecret")
    private final String accessKeySecret;

    @Schema(description = "临时安全令牌")
    private final String securityToken;

    @Schema(description = "凭证过期时间，ISO-8601 UTC 字符串", example = "2026-05-28T04:30:00Z")
    private final String expiration;
}
