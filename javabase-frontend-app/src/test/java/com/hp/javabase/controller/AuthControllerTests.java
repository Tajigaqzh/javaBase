package com.hp.javabase.controller;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.javabase.common.handler.GlobalExceptionHandler;
import com.hp.javabase.model.entity.User;
import com.hp.javabase.model.vo.LoginUserVO;
import com.hp.javabase.service.AuthService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * {@link AuthController} 控制器测试，覆盖请求体校验、成功返回和当前用户查询结果。
 */
class AuthControllerTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void loginShouldRejectBlankUsername() throws Exception {
        AuthService authService = Mockito.mock(AuthService.class);
        MockMvc mockMvc = createMockMvc(authService);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": " "
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("username must not be blank"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verifyNoInteractions(authService);
    }

    @Test
    void loginShouldReturnUserViewWhenUsernameValid() throws Exception {
        AuthService authService = Mockito.mock(AuthService.class);
        MockMvc mockMvc = createMockMvc(authService);
        LoginUserVO loginUserVO = new LoginUserVO(1L, "tester", "token-1");
        when(authService.login("tester")).thenReturn(loginUserVO);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "tester"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.username").value("tester"))
                .andExpect(jsonPath("$.data.token").value("token-1"));

        verify(authService).login("tester");
    }

    @Test
    void meShouldReturnCurrentUser() throws Exception {
        AuthService authService = Mockito.mock(AuthService.class);
        AuthController controller = new AuthController(authService);
        User user = new User();
        user.setId(7L);
        user.setUsername("current-user");
        when(authService.findCurrentUser()).thenReturn(user);

        assertSame(user, controller.me().getData());
        verify(authService).findCurrentUser();
    }

    @Test
    void loginShouldRejectMissingUsernameField() throws Exception {
        AuthService authService = Mockito.mock(AuthService.class);
        MockMvc mockMvc = createMockMvc(authService);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("username must not be blank"));

        verifyNoInteractions(authService);
    }

    private MockMvc createMockMvc(AuthService authService) {
        return MockMvcBuilders.standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }
}
