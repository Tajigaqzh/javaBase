package com.hp.javabase.service;

import com.hp.javabase.common.utils.oss.ObjectStorageUploadTokenUtils;
import com.hp.javabase.model.vo.ObjectStorageUploadTokenVO;
import org.springframework.stereotype.Service;

/**
 * 对象存储服务，负责承接控制器对上传临时凭证的业务请求，并复用底层阿里云 OSS STS 能力生成结果。
 */
@Service
public class ObjectStorageService {

    private final ObjectStorageUploadTokenUtils objectStorageUploadTokenUtils;

    public ObjectStorageService(ObjectStorageUploadTokenUtils objectStorageUploadTokenUtils) {
        this.objectStorageUploadTokenUtils = objectStorageUploadTokenUtils;
    }

    /**
     * 为指定用户创建对象存储上传临时凭证。
     *
     * @param userId 当前登录用户 ID
     * @return 上传临时凭证
     */
    public ObjectStorageUploadTokenVO createUploadToken(Long userId) {
        return objectStorageUploadTokenUtils.createUploadToken(userId);
    }
}
