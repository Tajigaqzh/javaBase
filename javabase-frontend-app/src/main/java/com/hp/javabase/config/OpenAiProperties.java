package com.hp.javabase.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "openai")
public class OpenAiProperties {

    private String apiKey;

    private String baseUrl;

    private Chat chat = new Chat();

    private Image image = new Image();

    private Embedding embedding = new Embedding();

    @Getter
    @Setter
    public static class Chat {

        private String model;
    }

    @Getter
    @Setter
    public static class Image {

        private String model;
    }

    @Getter
    @Setter
    public static class Embedding {

        private String model;
    }
}
