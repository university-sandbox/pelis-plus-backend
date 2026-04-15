package com.example.template.order;

public record CartTicketDto(
    String id,
    String screeningId,
    String seatId,
    String movieTitle,
    String venueName,
    String roomName,
    String screeningDate,
    String screeningTime,
    String format,
    Double price,
    String rowLabel,
    Integer colNum
) {
}
