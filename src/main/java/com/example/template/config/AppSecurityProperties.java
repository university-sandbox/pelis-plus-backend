package com.example.template.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record AppSecurityProperties(
    Jwt jwt,
    Cors cors
) {
    public record Jwt(String secret, long expirationMinutes, String issuer) {}
    public record Cors(String allowedOrigins) {}
}
