package com.example.template.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.email")
public record EmailProperties(
    boolean enabled,
    String testRecipient
) {

    public boolean hasTestRecipient() {
        return testRecipient != null && !testRecipient.isBlank();
    }
}
