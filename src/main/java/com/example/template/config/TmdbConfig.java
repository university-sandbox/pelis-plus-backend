package com.example.template.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(TmdbProperties.class)
public class TmdbConfig {

    @Bean
    public RestClient tmdbRestClient(TmdbProperties properties) {
        return RestClient.builder()
            .baseUrl(properties.resolvedBaseUrl())
            .defaultHeader("accept", "application/json")
            .build();
    }
}
