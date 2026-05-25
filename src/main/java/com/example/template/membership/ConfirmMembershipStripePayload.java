package com.example.template.membership;

import jakarta.validation.constraints.NotBlank;

public record ConfirmMembershipStripePayload(
    @NotBlank String sessionId
) {
}
