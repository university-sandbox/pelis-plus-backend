package com.example.template.ticket;

public record TicketDto(
    String id,
    String orderId,
    String bookingCode,
    String userName,
    String movie,
    String moviePosterPath,
    String venue,
    String room,
    String date,
    String time,
    String seat,
    String format,
    Double totalPaid,
    String qrData,
    String issuedAt
) {
}
