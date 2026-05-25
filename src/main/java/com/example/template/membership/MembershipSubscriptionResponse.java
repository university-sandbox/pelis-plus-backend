package com.example.template.membership;

public record MembershipSubscriptionResponse(
    String planId,
    String checkoutSessionId,
    String checkoutUrl
) {
}
