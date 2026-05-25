package com.example.template.order;

public record CreateOrderResponse(
    String orderId,
    String formToken,
    String checkoutSessionId,
    String checkoutUrl,
    OrderDto order,
    Boolean requiresPayment,
    Integer membershipTicketsApplied,
    Double paymentAmount
) {
}
