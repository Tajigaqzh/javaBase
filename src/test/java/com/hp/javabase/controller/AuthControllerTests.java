package com.hp.javabase.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.hp.javabase.common.response.BaseResponse;
import com.hp.javabase.model.dto.LoginRequest;
import com.hp.javabase.model.entity.User;
import com.hp.javabase.model.vo.LoginUserVO;
import com.hp.javabase.service.AuthService;
import org.junit.jupiter.api.Test;

/**
 * {@link AuthController} 的轻量级控制器测试，聚焦参数校验和返回结构。
 */
class AuthControllerTests {

    @Test
    void loginShouldRejectBlankUsername() {
        AuthService authService = mock(AuthService.class);
        AuthController controller = new AuthController(authService);
        LoginRequest request = new LoginRequest();
        request.setUsername(" ");

        BaseResponse<LoginUserVO> response = controller.login(request);

        assertEquals(400, response.getCode());
        assertEquals("username must not be blank", response.getMessage());
        assertNull(response.getData());
        verifyNoInteractions(authService);
    }

    @Test
    void loginShouldReturnUserViewWhenUsernameValid() {
        AuthService authService = mock(AuthService.class);
        AuthController controller = new AuthController(authService);
        LoginRequest request = new LoginRequest();
        LoginUserVO loginUserVO = new LoginUserVO(1L, "tester", "token-1");
        request.setUsername("tester");
        when(authService.login("tester")).thenReturn(loginUserVO);

        BaseResponse<LoginUserVO> response = controller.login(request);

        assertEquals(200, response.getCode());
        assertSame(loginUserVO, response.getData());
        verify(authService).login("tester");
    }

    @Test
    void meShouldReturnCurrentUser() {
        AuthService authService = mock(AuthService.class);
        AuthController controller = new AuthController(authService);
        User user = new User();
        user.setId(7L);
        user.setUsername("current-user");
        when(authService.findCurrentUser()).thenReturn(user);

        BaseResponse<User> response = controller.me();

        assertEquals(200, response.getCode());
        assertSame(user, response.getData());
        verify(authService).findCurrentUser();
    }
}
