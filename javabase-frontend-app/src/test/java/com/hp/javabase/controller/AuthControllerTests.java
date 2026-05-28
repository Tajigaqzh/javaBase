package com.hp.javabase.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hp.javabase.common.handler.GlobalExceptionHandler;
import com.hp.javabase.model.dto.ChangePasswordDTO;
import com.hp.javabase.model.enums.VerificationSceneEnum;
import com.hp.javabase.model.vo.CurrentUserVO;
import com.hp.javabase.model.vo.LoginUserVO;
import com.hp.javabase.model.vo.VerificationCodeSendVO;
import com.hp.javabase.service.common.FeishuBotService;
import com.hp.javabase.service.login.AuthService;
import com.hp.javabase.service.login.VerificationCodeUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * {@link AuthController} 控制器测试，覆盖前台认证接口的参数校验和基础委派行为。
 */
class AuthControllerTests {

    @Test
    void loginByPasswordShouldRejectMissingPhone() throws Exception {
        AuthService authService = Mockito.mock(AuthService.class);
        VerificationCodeUtils verificationCodeUtils = Mockito.mock(VerificationCodeUtils.class);
        MockMvc mockMvc = createMockMvc(authService, verificationCodeUtils);

        mockMvc.perform(post("/auth/login/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "password": "Abcdef!234",
                                  "terminalType": "WEB",
                                  "deviceId": "web-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("phone must not be blank"));

        verifyNoInteractions(authService);
    }

    @Test
    void loginByPasswordShouldReturnTokenWhenRequestValid() throws Exception {
        AuthService authService = Mockito.mock(AuthService.class);
        VerificationCodeUtils verificationCodeUtils = Mockito.mock(VerificationCodeUtils.class);
        MockMvc mockMvc = createMockMvc(authService, verificationCodeUtils);
        LoginUserVO loginUserVO = LoginUserVO.builder()
                .userId(1L)
                .phone("13800138000")
                .token("token-1")
                .terminalType("WEB")
                .deviceId("web-1")
                .status(1)
                .build();
        when(authService.loginByPassword(any(), any(), any())).thenReturn(loginUserVO);

        mockMvc.perform(post("/auth/login/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("User-Agent", "JUnit")
                        .content("""
                                {
                                  "phone": "13800138000",
                                  "password": "Abcdef!234",
                                  "terminalType": "WEB",
                                  "deviceId": "web-1",
                                  "deviceName": "Mac Chrome"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.phone").value("13800138000"))
                .andExpect(jsonPath("$.data.terminalType").value("WEB"))
                .andExpect(jsonPath("$.data.deviceId").value("web-1"));

        verify(authService).loginByPassword(any(), eq("127.0.0.1"), eq("JUnit"));
    }

    @Test
    void loginByPasswordShouldUseForwardedHeaderIpWhenPresent() throws Exception {
        AuthService authService = Mockito.mock(AuthService.class);
        VerificationCodeUtils verificationCodeUtils = Mockito.mock(VerificationCodeUtils.class);
        MockMvc mockMvc = createMockMvc(authService, verificationCodeUtils);
        LoginUserVO loginUserVO = LoginUserVO.builder()
                .userId(1L)
                .phone("13800138000")
                .token("token-1")
                .terminalType("WEB")
                .deviceId("web-1")
                .status(1)
                .build();
        when(authService.loginByPassword(any(), any(), any())).thenReturn(loginUserVO);

        mockMvc.perform(post("/auth/login/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("User-Agent", "JUnit")
                        .header("X-Forwarded-For", "unknown, 10.20.30.40, 127.0.0.1")
                        .content("""
                                {
                                  "phone": "13800138000",
                                  "password": "Abcdef!234",
                                  "terminalType": "WEB",
                                  "deviceId": "web-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(authService).loginByPassword(any(), eq("10.20.30.40"), eq("JUnit"));
    }

    @Test
    void registerByPhoneShouldRejectInvalidTerminalType() throws Exception {
        AuthService authService = Mockito.mock(AuthService.class);
        VerificationCodeUtils verificationCodeUtils = Mockito.mock(VerificationCodeUtils.class);
        MockMvc mockMvc = createMockMvc(authService, verificationCodeUtils);

        mockMvc.perform(post("/auth/register/phone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "13800138000",
                                  "smsCode": "123456",
                                  "password": "Abcdef!234",
                                  "terminalType": "WINDOWS",
                                  "deviceId": "pc-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("terminalType must be one of WEB, ANDROID, IOS"));

        verifyNoInteractions(authService);
    }

    @Test
    void sendPhoneCodeShouldDelegateToVerificationService() throws Exception {
        AuthService authService = Mockito.mock(AuthService.class);
        VerificationCodeUtils verificationCodeUtils = Mockito.mock(VerificationCodeUtils.class);
        MockMvc mockMvc = createMockMvc(authService, verificationCodeUtils);
        when(verificationCodeUtils.sendPhoneCode("13800138000", VerificationSceneEnum.REGISTER, "127.0.0.1"))
                .thenReturn(new VerificationCodeSendVO(300, 60, "123456"));

        mockMvc.perform(post("/auth/code/phone/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "13800138000",
                                  "scene": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.debugCode").value("123456"));

        verify(verificationCodeUtils).sendPhoneCode("13800138000", VerificationSceneEnum.REGISTER, "127.0.0.1");
    }

    @Test
    void setPasswordShouldRejectWhenPhoneAndEmailAreBothMissing() throws Exception {
        AuthService authService = Mockito.mock(AuthService.class);
        VerificationCodeUtils verificationCodeUtils = Mockito.mock(VerificationCodeUtils.class);
        MockMvc mockMvc = createMockMvc(authService, verificationCodeUtils);

        mockMvc.perform(post("/auth/password/set")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "verificationCode": "123456",
                                  "newPassword": "Abcdef!234"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("exactly one of phone or email must be provided"));

        verifyNoInteractions(authService);
    }

    @Test
    void forgotResetPasswordShouldRejectWhenPhoneAndEmailAreBothProvided() throws Exception {
        AuthService authService = Mockito.mock(AuthService.class);
        VerificationCodeUtils verificationCodeUtils = Mockito.mock(VerificationCodeUtils.class);
        MockMvc mockMvc = createMockMvc(authService, verificationCodeUtils);

        mockMvc.perform(post("/auth/password/forgot/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "13800138000",
                                  "email": "demo@example.com",
                                  "verificationCode": "123456",
                                  "newPassword": "Abcdef!234"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("exactly one of phone or email must be provided"));

        verifyNoInteractions(authService);
    }

    @Test
    void changePasswordShouldRejectSameNewPassword() throws Exception {
        AuthService authService = Mockito.mock(AuthService.class);
        VerificationCodeUtils verificationCodeUtils = Mockito.mock(VerificationCodeUtils.class);
        MockMvc mockMvc = createMockMvc(authService, verificationCodeUtils);

        mockMvc.perform(post("/auth/password/change")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "oldPassword": "Abcdef!234",
                                  "newPassword": "Abcdef!234"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("newPassword must be different from oldPassword"));

        verifyNoInteractions(authService);
    }

    @Test
    void changePasswordShouldDelegateWhenRequestValid() {
        AuthService authService = Mockito.mock(AuthService.class);
        VerificationCodeUtils verificationCodeUtils = Mockito.mock(VerificationCodeUtils.class);
        AuthController controller = new AuthController(authService, verificationCodeUtils);

        ChangePasswordDTO request = new ChangePasswordDTO();
        request.setOldPassword("Abcdef!234");
        request.setNewPassword("Abcdef!567");

        controller.changePassword(request);

        verify(authService).changePassword(request);
    }

    @Test
    void meShouldReturnCurrentUserProfile() {
        AuthService authService = Mockito.mock(AuthService.class);
        VerificationCodeUtils verificationCodeUtils = Mockito.mock(VerificationCodeUtils.class);
        AuthController controller = new AuthController(authService, verificationCodeUtils);
        CurrentUserVO currentUserVO = CurrentUserVO.builder()
                .userId(7L)
                .userNo("U0007")
                .nickname("当前用户")
                .phone("13800138000")
                .status(1)
                .build();
        when(authService.getCurrentUserProfile()).thenReturn(currentUserVO);

        controller.me();

        verify(authService).getCurrentUserProfile();
    }

    @Test
    void kickoutSessionShouldRejectMissingSessionId() throws Exception {
        AuthService authService = Mockito.mock(AuthService.class);
        VerificationCodeUtils verificationCodeUtils = Mockito.mock(VerificationCodeUtils.class);
        MockMvc mockMvc = createMockMvc(authService, verificationCodeUtils);

        mockMvc.perform(post("/auth/session/kickout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("sessionId must not be null"));

        verifyNoInteractions(authService);
    }

    @Test
    void kickoutSessionShouldRejectNonPositiveSessionId() throws Exception {
        AuthService authService = Mockito.mock(AuthService.class);
        VerificationCodeUtils verificationCodeUtils = Mockito.mock(VerificationCodeUtils.class);
        MockMvc mockMvc = createMockMvc(authService, verificationCodeUtils);

        mockMvc.perform(post("/auth/session/kickout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sessionId": 0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("sessionId must be greater than 0"));

        verifyNoInteractions(authService);
    }

    @Test
    void loginByPasswordShouldNotifyFeishuWhenUnhandledExceptionOccurs() throws Exception {
        AuthService authService = Mockito.mock(AuthService.class);
        VerificationCodeUtils verificationCodeUtils = Mockito.mock(VerificationCodeUtils.class);
        FeishuBotService feishuBotService = Mockito.mock(FeishuBotService.class);
        MockMvc mockMvc = createMockMvc(authService, verificationCodeUtils, feishuBotService);
        when(authService.loginByPassword(any(), any(), any())).thenThrow(new RuntimeException("db down"));

        mockMvc.perform(post("/auth/login/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "13800138000",
                                  "password": "Abcdef!234",
                                  "terminalType": "WEB",
                                  "deviceId": "web-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("db down"));

        verify(feishuBotService).sendTextMessage(org.mockito.ArgumentMatchers.contains("uri: /auth/login/password"));
        verify(feishuBotService).sendTextMessage(org.mockito.ArgumentMatchers.contains("message: db down"));
    }

    private MockMvc createMockMvc(AuthService authService, VerificationCodeUtils verificationCodeUtils) {
        return createMockMvc(authService, verificationCodeUtils, Mockito.mock(FeishuBotService.class));
    }

    private MockMvc createMockMvc(
            AuthService authService,
            VerificationCodeUtils verificationCodeUtils,
            FeishuBotService feishuBotService) {
        return MockMvcBuilders.standaloneSetup(new AuthController(authService, verificationCodeUtils))
                .setControllerAdvice(new GlobalExceptionHandler(feishuBotService))
                .build();
    }
}
