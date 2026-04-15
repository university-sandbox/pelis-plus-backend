package com.example.template.order;

import java.util.List;
import java.util.Map;

/**
 * Mirrors the frontend's CreateOrderPayload / CartTicket / CartSnackItem shapes.
 */
public record CreateOrderPayload(
    List<CartTicketPayload> tickets,
    List<CartSnackItemPayload> snacks,
    Double membershipDiscount
) {
    public record CartTicketPayload(
        String screeningId,
        String movieId,
        String movieTitle,
        String moviePosterPath,
        String date,
        String time,
        String venue,
        String room,
        String format,
        CartSeatPayload seat,
        Double price
    ) {}

    public record CartSeatPayload(
        String id,
        String row,
        Integer col,
        String type
    ) {}

    public record CartSnackItemPayload(
        SnackPayload snack,
        Integer quantity,
        Map<String, String> selectedOptions
    ) {}

    /** Minimal snack shape — only id is required for lookup. */
    public record SnackPayload(
        String id,
        String name,
        String description,
        String category,
        Double price,
        String image,
        String status
    ) {}
}
