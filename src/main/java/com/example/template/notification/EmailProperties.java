package com.example.template.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.email")
public record EmailProperties(
    boolean enabled,
    String testRecipient,
    String fromAddress
) {

    public boolean hasTestRecipient() {
        return testRecipient != null && !testRecipient.isBlank();
    }

    public boolean hasFromAddress() {
        return hasText(fromAddress);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
