package com.hp.javabase.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 前台应用 OpenAPI 文档配置，统一声明接口分组、基本说明和 Sa-Token 鉴权头信息。
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "JavaBase Frontend API",
                version = "v1",
                description = "前台应用接口文档，覆盖认证与示例接口。",
                contact = @Contact(name = "JavaBase"),
                license = @License(name = "Internal Use Only")))
@SecurityScheme(
        name = "satoken",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        paramName = "satoken",
        description = "登录成功后，将返回的 token 放入 satoken 请求头。")
public class OpenApiDocConfig {

    @Bean
    public OpenAPI frontendOpenApi() {
        return new OpenAPI()
                .servers(List.of(
                        new Server().url("/").description("Current environment")))
                .info(new io.swagger.v3.oas.models.info.Info()
                        .title("JavaBase Frontend API")
                        .version("v1")
                        .description("前台接口文档，建议通过 Scalar 页面进行调试。")
                        .termsOfService("internal")
                        .contact(new io.swagger.v3.oas.models.info.Contact().name("JavaBase"))
                        .license(new io.swagger.v3.oas.models.info.License().name("Internal Use Only")));
    }
}
