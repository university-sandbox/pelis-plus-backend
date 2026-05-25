package com.example.template.payment;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.payments.stripe")
public record StripePaymentProperties(
    String secretKey,
    String currency,
    String successUrl,
    String cancelUrl,
    String membershipSuccessUrl,
    String membershipCancelUrl
) {
    public boolean isConfigured() {
        return secretKey != null && !secretKey.isBlank();
    }
}
