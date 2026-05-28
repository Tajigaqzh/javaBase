package com.hp.javabase.service.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.hp.javabase.common.exception.BusinessException;
import com.hp.javabase.common.utils.RestClientUtils;
import com.hp.javabase.config.FeishuBotProperties;
import java.net.URI;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 飞书机器人通知服务，负责通过自定义机器人 webhook 发送文本消息。
 *
 * <p>当前服务聚焦最基础的文本消息能力，主要流程包括：
 * 1. 校验机器人开关、webhook 和消息内容；
 * 2. 解析 webhook 的基础地址与路径；
 * 3. 组装飞书机器人文本消息请求体；
 * 4. 调用 {@link RestClientUtils} 发起 HTTP POST；
 * 5. 校验飞书返回码并在失败时抛出统一业务异常。
 */
@Service
public class FeishuBotService {

    private static final Logger log = LoggerFactory.getLogger(FeishuBotService.class);

    private final FeishuBotProperties feishuBotProperties;

    private final RestClientUtils restClientUtils;

    public FeishuBotService(
            FeishuBotProperties feishuBotProperties,
            RestClientUtils restClientUtils) {
        this.feishuBotProperties = feishuBotProperties;
        this.restClientUtils = restClientUtils;
    }

    /**
     * 发送飞书文本机器人消息。
     *
     * @param text 消息正文
     */
    public void sendTextMessage(String text) {
        if (!feishuBotProperties.isEnabled()) {
            throw new IllegalStateException("app.feishu.bot.enabled is false");
        }
        if (!StringUtils.hasText(text)) {
            throw new IllegalArgumentException("feishu message text must not be blank");
        }
        URI webhookUri = resolveWebhookUri();

        JsonNode responseBody = restClientUtils.postJson(
                buildBaseUrl(webhookUri),
                buildPath(webhookUri),
                null,
                Map.of(
                        "msg_type", "text",
                        "content", Map.of("text", text)),
                JsonNode.class);

        validateResponse(responseBody);
        log.info("feishu bot message sent successfully");
    }

    private URI resolveWebhookUri() {
        if (!StringUtils.hasText(feishuBotProperties.getWebhookUrl())) {
            throw new IllegalStateException("APP_FEISHU_BOT_WEBHOOK_URL is not configured");
        }
        URI webhookUri = URI.create(feishuBotProperties.getWebhookUrl());
        if (!StringUtils.hasText(webhookUri.getScheme()) || !StringUtils.hasText(webhookUri.getHost())) {
            throw new IllegalArgumentException("app.feishu.bot.webhook-url is invalid");
        }
        return webhookUri;
    }

    private String buildBaseUrl(URI webhookUri) {
        StringBuilder builder = new StringBuilder();
        builder.append(webhookUri.getScheme())
                .append("://")
                .append(webhookUri.getHost());
        if (webhookUri.getPort() > 0) {
            builder.append(":").append(webhookUri.getPort());
        }
        return builder.toString();
    }

    private String buildPath(URI webhookUri) {
        String path = webhookUri.getRawPath();
        if (webhookUri.getRawQuery() != null) {
            path = path + "?" + webhookUri.getRawQuery();
        }
        return path;
    }

    private void validateResponse(JsonNode responseBody) {
        if (responseBody == null) {
            throw new BusinessException("feishu bot send failed: empty response");
        }
        int code = responseBody.path("code").asInt(-1);
        if (code != 0) {
            String message = responseBody.path("msg").asText("unknown error");
            throw new BusinessException("feishu bot send failed: " + message);
        }
    }
}
