package com.hp.javabase.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.hp.javabase.common.utils.RestClientUtils;
import com.hp.javabase.config.OpenAiProperties;
import com.hp.javabase.model.vo.ChatResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * OpenAI 对话服务，负责组装 OpenAI Responses API 请求并解析回答文本。
 *
 * <p>当前服务依赖：
 * 1. {@link OpenAiProperties} 读取 API Key、Base URL 和默认模型；
 * 2. {@link RestClientUtils} 执行第三方 HTTP 请求；
 * 3. Jackson {@link JsonNode} 解析响应结果。
 */
@Service
public class OpenAiChatService {

    private final OpenAiProperties openAiProperties;

    private final RestClientUtils restClientUtils;

    public OpenAiChatService(
            OpenAiProperties openAiProperties,
            RestClientUtils restClientUtils) {
        this.openAiProperties = openAiProperties;
        this.restClientUtils = restClientUtils;
    }

    /**
     * 调用 OpenAI Responses API 执行一次文本对话。
     *
     * <p>主要流程包括：
     * 1. 校验 API Key 和用户输入；
     * 2. 解析本次调用模型；
     * 3. 组装 OpenAI 请求体；
     * 4. 通过 {@link RestClientUtils} 发起 HTTP 请求；
     * 5. 提取输出文本并封装返回。
     *
     * @param message 用户输入文本
     * @param requestedModel 本次请求显式指定的模型，可为空
     * @return 对话响应结果
     */
    public ChatResponse chat(String message, String requestedModel) {
        if (!StringUtils.hasText(openAiProperties.getApiKey())) {
            throw new IllegalStateException("OPENAI_API_KEY is not configured");
        }
        if (!StringUtils.hasText(message)) {
            throw new IllegalArgumentException("message must not be blank");
        }
        String model = resolveChatModel(requestedModel);

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", model);
        requestBody.put("input", List.of(Map.of(
                "role", "user",
                "content", List.of(Map.of(
                        "type", "input_text",
                        "text", message
                ))
        )));

        JsonNode responseBody = restClientUtils.postJson(
                openAiProperties.getBaseUrl(),
                "/responses",
                Map.of(HttpHeaders.AUTHORIZATION, "Bearer " + openAiProperties.getApiKey()),
                requestBody,
                JsonNode.class);

        return new ChatResponse(model, extractOutputText(responseBody));
    }

    private String resolveChatModel(String requestedModel) {
        if (StringUtils.hasText(requestedModel)) {
            return requestedModel;
        }
        if (StringUtils.hasText(openAiProperties.getChat().getModel())) {
            return openAiProperties.getChat().getModel();
        }
        throw new IllegalStateException("openai.chat.model is not configured");
    }

    private String extractOutputText(JsonNode root) {
        if (root == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        JsonNode output = root.path("output");
        if (output.isArray()) {
            for (JsonNode item : output) {
                JsonNode content = item.path("content");
                if (!content.isArray()) {
                    continue;
                }
                for (JsonNode contentItem : content) {
                    if ("output_text".equals(contentItem.path("type").asText())) {
                        builder.append(contentItem.path("text").asText());
                    }
                }
            }
        }
        return builder.toString().trim();
    }
}
