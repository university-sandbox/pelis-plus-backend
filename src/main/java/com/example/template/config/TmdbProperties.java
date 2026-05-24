package com.example.template.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.tmdb")
public record TmdbProperties(
    Boolean enabled,
    String baseUrl,
    String accessToken,
    String apiKey,
    String language,
    String region,
    Integer maxPagesPerList,
    String syncCron,
    String timeZone
) {
    private static final String DEFAULT_BASE_URL = "https://api.themoviedb.org/3";
    private static final String DEFAULT_LANGUAGE = "es-PE";
    private static final String DEFAULT_REGION = "PE";
    private static final int DEFAULT_MAX_PAGES_PER_LIST = 3;

    public boolean isEnabled() {
        return enabled == null || enabled;
    }

    public String resolvedBaseUrl() {
        return hasText(baseUrl) ? baseUrl : DEFAULT_BASE_URL;
    }

    public String resolvedLanguage() {
        return hasText(language) ? language : DEFAULT_LANGUAGE;
    }

    public String resolvedRegion() {
        return hasText(region) ? region : DEFAULT_REGION;
    }

    public int resolvedMaxPagesPerList() {
        return maxPagesPerList == null || maxPagesPerList < 1 ? DEFAULT_MAX_PAGES_PER_LIST : maxPagesPerList;
    }

    public boolean hasCredentials() {
        return hasText(accessToken) || hasText(apiKey);
    }

    public boolean hasAccessToken() {
        return hasText(accessToken);
    }

    public boolean hasApiKey() {
        return hasText(apiKey);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
