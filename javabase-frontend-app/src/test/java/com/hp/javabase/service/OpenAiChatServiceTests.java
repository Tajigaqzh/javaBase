package com.hp.javabase.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.javabase.common.utils.RestClientUtils;
import com.hp.javabase.config.OpenAiProperties;
import com.hp.javabase.model.vo.ChatResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;

/**
 * {@link OpenAiChatService} 单元测试，聚焦模型解析、参数校验和第三方调用组装行为。
 */
class OpenAiChatServiceTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void chatShouldUseRequestedModelWhenProvided() throws Exception {
        OpenAiProperties properties = createProperties();
        RestClientUtils restClientUtils = Mockito.mock(RestClientUtils.class);
        OpenAiChatService service = new OpenAiChatService(properties, restClientUtils);
        JsonNode responseNode = objectMapper.readTree("""
                {
                  "output": [
                    {
                      "content": [
                        {
                          "type": "output_text",
                          "text": "hello from openai"
                        }
                      ]
                    }
                  ]
                }
                """);
        when(restClientUtils.postJson(
                eq("https://api.openai.com/v1"),
                eq("/responses"),
                any(),
                any(),
                eq(JsonNode.class))).thenReturn(responseNode);

        ChatResponse response = service.chat("hello", "gpt-custom");

        assertEquals("gpt-custom", response.getModel());
        assertEquals("hello from openai", response.getAnswer());
        verify(restClientUtils).postJson(
                eq("https://api.openai.com/v1"),
                eq("/responses"),
                eq(java.util.Map.of(HttpHeaders.AUTHORIZATION, "Bearer test-key")),
                any(),
                eq(JsonNode.class));
    }

    @Test
    void chatShouldFallbackToConfiguredModel() throws Exception {
        OpenAiProperties properties = createProperties();
        RestClientUtils restClientUtils = Mockito.mock(RestClientUtils.class);
        OpenAiChatService service = new OpenAiChatService(properties, restClientUtils);
        JsonNode responseNode = objectMapper.readTree("""
                { "output": [] }
                """);
        when(restClientUtils.postJson(
                eq("https://api.openai.com/v1"),
                eq("/responses"),
                any(),
                any(),
                eq(JsonNode.class))).thenReturn(responseNode);

        ChatResponse response = service.chat("hello", null);

        assertEquals("gpt-default", response.getModel());
        assertEquals("", response.getAnswer());
    }

    @Test
    void chatShouldRejectBlankMessage() {
        OpenAiProperties properties = createProperties();
        RestClientUtils restClientUtils = Mockito.mock(RestClientUtils.class);
        OpenAiChatService service = new OpenAiChatService(properties, restClientUtils);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.chat(" ", null));

        assertEquals("message must not be blank", exception.getMessage());
    }

    @Test
    void chatShouldRejectMissingApiKey() {
        OpenAiProperties properties = createProperties();
        properties.setApiKey("");
        RestClientUtils restClientUtils = Mockito.mock(RestClientUtils.class);
        OpenAiChatService service = new OpenAiChatService(properties, restClientUtils);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.chat("hello", null));

        assertEquals("OPENAI_API_KEY is not configured", exception.getMessage());
    }

    private OpenAiProperties createProperties() {
        OpenAiProperties properties = new OpenAiProperties();
        properties.setApiKey("test-key");
        properties.setBaseUrl("https://api.openai.com/v1");
        properties.getChat().setModel("gpt-default");
        return properties;
    }
}
