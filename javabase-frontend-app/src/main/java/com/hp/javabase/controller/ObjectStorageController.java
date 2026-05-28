package com.hp.javabase.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.hp.javabase.common.response.BaseResponse;
import com.hp.javabase.common.utils.resonse.ResponseUtils;
import com.hp.javabase.model.vo.ObjectStorageUploadTokenVO;
import com.hp.javabase.service.common.ObjectStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 对象存储控制器，对外提供前端直传所需的临时上传凭证接口。
 */
@RestController
@RequestMapping("/storage")
@Tag(name = "Object Storage", description = "对象存储上传凭证接口。")
public class ObjectStorageController {

    private final ObjectStorageService objectStorageService;

    public ObjectStorageController(ObjectStorageService objectStorageService) {
        this.objectStorageService = objectStorageService;
    }

    @SaCheckLogin
    @GetMapping("/upload/token")
    @Operation(
            summary = "获取对象存储上传临时凭证",
            description = "为当前登录用户签发一份对象存储直传临时凭证，当前阶段底层实现为阿里云 OSS STS。",
            security = @SecurityRequirement(name = "satoken"))
    public BaseResponse<ObjectStorageUploadTokenVO> getUploadToken() {
        return ResponseUtils.success(objectStorageService.createUploadToken(StpUtil.getLoginIdAsLong()));
    }
}
