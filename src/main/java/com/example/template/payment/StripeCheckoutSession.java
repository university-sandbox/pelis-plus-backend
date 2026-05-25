package com.example.template.payment;

public record StripeCheckoutSession(
    String id,
    String url,
    String paymentStatus
) {
}
