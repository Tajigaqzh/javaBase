package com.hp.javabase.service.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.javabase.common.exception.BusinessException;
import com.hp.javabase.common.utils.RestClientUtils;
import com.hp.javabase.config.FeishuBotProperties;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * {@link FeishuBotService} 单元测试，覆盖配置校验、请求组装和失败响应处理。
 */
class FeishuBotServiceTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void sendTextMessageShouldPostWebhookWhenEnabled() throws Exception {
        FeishuBotProperties properties = createEnabledProperties();
        RestClientUtils restClientUtils = Mockito.mock(RestClientUtils.class);
        FeishuBotService service = new FeishuBotService(properties, restClientUtils);
        JsonNode responseNode = objectMapper.readTree("""
                {
                  "code": 0,
                  "msg": "success"
                }
                """);
        when(restClientUtils.postJson(
                eq("https://open.feishu.cn"),
                eq("/open-apis/bot/v2/hook/test-hook"),
                eq(null),
                any(),
                eq(JsonNode.class))).thenReturn(responseNode);

        service.sendTextMessage("test notification");

        verify(restClientUtils).postJson(
                eq("https://open.feishu.cn"),
                eq("/open-apis/bot/v2/hook/test-hook"),
                eq(null),
                eq(Map.of(
                        "msg_type", "text",
                        "content", Map.of("text", "test notification"))),
                eq(JsonNode.class));
    }

    @Test
    void sendTextMessageShouldRejectWhenDisabled() {
        FeishuBotProperties properties = new FeishuBotProperties();
        properties.setWebhookUrl("https://open.feishu.cn/open-apis/bot/v2/hook/test-hook");
        RestClientUtils restClientUtils = Mockito.mock(RestClientUtils.class);
        FeishuBotService service = new FeishuBotService(properties, restClientUtils);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.sendTextMessage("hello"));

        assertEquals("app.feishu.bot.enabled is false", exception.getMessage());
    }

    @Test
    void sendTextMessageShouldRejectBlankText() {
        FeishuBotProperties properties = createEnabledProperties();
        RestClientUtils restClientUtils = Mockito.mock(RestClientUtils.class);
        FeishuBotService service = new FeishuBotService(properties, restClientUtils);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.sendTextMessage(" "));

        assertEquals("feishu message text must not be blank", exception.getMessage());
    }

    @Test
    void sendTextMessageShouldRejectFailedResponse() throws Exception {
        FeishuBotProperties properties = createEnabledProperties();
        RestClientUtils restClientUtils = Mockito.mock(RestClientUtils.class);
        FeishuBotService service = new FeishuBotService(properties, restClientUtils);
        JsonNode responseNode = objectMapper.readTree("""
                {
                  "code": 19024,
                  "msg": "invalid request"
                }
                """);
        when(restClientUtils.postJson(
                eq("https://open.feishu.cn"),
                eq("/open-apis/bot/v2/hook/test-hook"),
                eq(null),
                any(),
                eq(JsonNode.class))).thenReturn(responseNode);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.sendTextMessage("hello"));

        assertEquals("feishu bot send failed: invalid request", exception.getMessage());
    }

    private FeishuBotProperties createEnabledProperties() {
        FeishuBotProperties properties = new FeishuBotProperties();
        properties.setEnabled(true);
        properties.setWebhookUrl("https://open.feishu.cn/open-apis/bot/v2/hook/test-hook");
        return properties;
    }
}
