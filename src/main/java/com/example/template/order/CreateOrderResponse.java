package com.example.template.order;

public record CreateOrderResponse(
    String orderId,
    String formToken,
    OrderDto order,
    Boolean requiresPayment,
    Integer membershipTicketsApplied,
    Double paymentAmount
) {
}
