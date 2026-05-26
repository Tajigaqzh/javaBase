package com.hp.javabase.common.utils;

import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * RestClient 工具组件，统一封装三方 HTTP JSON 接口的基础调用逻辑。
 *
 * <p>当前组件主要提供 GET / POST JSON 调用能力，负责：
 * 1. 按 baseUrl 构建 RestClient；
 * 2. 统一设置请求头；
 * 3. 统一处理 JSON 请求体和响应体类型。
 *
 * <p>适用场景是普通三方 HTTP API 调用，调用方仍需自行处理业务参数校验、
 * 异常映射、响应字段解析和重试策略。
 */
@Component
public class RestClientUtils {

    /**
     * 发送 JSON POST 请求并按目标类型解析响应体。
     *
     * @param baseUrl 第三方服务基础地址，例如 https://api.openai.com/v1
     * @param path 请求路径，例如 /responses
     * @param headers 额外请求头，可为空
     * @param body JSON 请求体对象
     * @param responseType 目标响应类型
     * @param <T> 响应泛型
     * @return 解析后的响应对象
     */
    public <T> T postJson(
            String baseUrl,
            String path,
            Map<String, String> headers,
            Object body,
            Class<T> responseType) {
        RestClient.RequestBodySpec requestSpec = createClient(baseUrl)
                .post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON);
        applyHeaders(requestSpec, headers);
        return requestSpec.body(body)
                .retrieve()
                .body(responseType);
    }

    /**
     * 发送 GET 请求并按目标类型解析响应体。
     *
     * @param baseUrl 第三方服务基础地址
     * @param path 请求路径
     * @param headers 额外请求头，可为空
     * @param responseType 目标响应类型
     * @param <T> 响应泛型
     * @return 解析后的响应对象
     */
    public <T> T get(
            String baseUrl,
            String path,
            Map<String, String> headers,
            Class<T> responseType) {
        RestClient.RequestHeadersSpec<?> requestSpec = createClient(baseUrl)
                .get()
                .uri(path);
        applyHeaders(requestSpec, headers);
        return requestSpec.retrieve()
                .body(responseType);
    }

    private RestClient createClient(String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    private void applyHeaders(
            RestClient.RequestHeadersSpec<?> requestSpec,
            Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return;
        }
        headers.forEach(requestSpec::header);
        if (!headers.containsKey(HttpHeaders.CONTENT_TYPE)
                && requestSpec instanceof RestClient.RequestBodySpec requestBodySpec) {
            requestBodySpec.contentType(MediaType.APPLICATION_JSON);
        }
    }
}
