package com.example.template.order;

import jakarta.validation.constraints.NotBlank;

public record ConfirmStripeCheckoutPayload(
    @NotBlank String sessionId
) {
}
