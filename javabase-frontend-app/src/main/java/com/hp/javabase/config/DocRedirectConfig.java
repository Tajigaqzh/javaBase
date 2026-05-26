package com.hp.javabase.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 文档访问路径配置，将历史文档入口重定向到当前 Scalar 页面。
 */
@Configuration
public class DocRedirectConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/doc.html", "/scalar");
        registry.addRedirectViewController("/swagger-ui.html", "/scalar");
    }
}
