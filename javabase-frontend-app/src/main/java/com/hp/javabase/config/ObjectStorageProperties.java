package com.hp.javabase.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 对象存储配置，统一承载上传凭证接口所需的供应商选择、目录前缀和阿里云 STS 参数。
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.storage")
public class ObjectStorageProperties {

    private String provider = "ALIYUN_OSS";

    private String uploadDirPrefix = "upload";

    private Aliyun aliyun = new Aliyun();

    @Getter
    @Setter
    public static class Aliyun {

        private String region = "cn-hangzhou";

        private String endpoint = "oss-cn-hangzhou.aliyuncs.com";

        private String stsEndpoint = "sts.cn-hangzhou.aliyuncs.com";

        private String bucket;

        private String roleArn;

        private String roleSessionNamePrefix = "javabase-upload";

        private Long durationSeconds = 900L;

        private String publicHost;

        private String accessKeyId;

        private String accessKeySecret;
    }
}
