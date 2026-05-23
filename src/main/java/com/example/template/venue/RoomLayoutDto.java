package com.example.template.venue;

public record RoomLayoutDto(
    String id,
    String name,
    Integer rows,
    Integer cols,
    Integer capacity,
    String seatMap,
    Boolean active
) {
}
