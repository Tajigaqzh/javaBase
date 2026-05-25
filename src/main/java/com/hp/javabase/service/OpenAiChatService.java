package com.hp.javabase.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.hp.javabase.config.OpenAiProperties;
import com.hp.javabase.model.vo.ChatResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OpenAiChatService {

    private final OpenAiProperties openAiProperties;

    private final RestClient restClient;

    public OpenAiChatService(OpenAiProperties openAiProperties) {
        this.openAiProperties = openAiProperties;
        this.restClient = RestClient.builder().baseUrl(openAiProperties.getBaseUrl()).build();
    }

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

        JsonNode responseBody = restClient.post()
                .uri("/responses")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAiProperties.getApiKey())
                .body(requestBody)
                .retrieve()
                .body(JsonNode.class);

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
