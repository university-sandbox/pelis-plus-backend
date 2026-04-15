package com.example.template.order;

import java.util.List;

public record OrderDto(
    String id,
    String userId,
    List<CartTicketDto> tickets,
    List<CartSnackItemDto> snacks,
    Double subtotal,
    Double discount,
    Double total,
    String status,
    String paymentStatus,
    String createdAt
) {
}
