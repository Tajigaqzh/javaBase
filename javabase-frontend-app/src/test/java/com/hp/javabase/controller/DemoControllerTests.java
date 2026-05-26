package com.hp.javabase.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.hp.javabase.common.response.BaseResponse;
import org.junit.jupiter.api.Test;

/**
 * {@link DemoController} 的示例接口测试，校验统一响应输出是否稳定。
 */
class DemoControllerTests {

    private final DemoController controller = new DemoController();

    @Test
    void demoEndpointShouldReturnAnonymousUserLabel() {
        BaseResponse<String> response = controller.getUserName();

        assertEquals(200, response.getCode());
        assertEquals("user", response.getData());
    }

    @Test
    void loginProtectedEndpointShouldReturnSuccessMessage() {
        BaseResponse<String> response = controller.loginProtected();

        assertEquals(200, response.getCode());
        assertEquals("login success", response.getData());
    }

    @Test
    void permissionProtectedEndpointShouldReturnSuccessMessage() {
        BaseResponse<String> response = controller.permissionProtected();

        assertEquals(200, response.getCode());
        assertEquals("permission success", response.getData());
    }
}
