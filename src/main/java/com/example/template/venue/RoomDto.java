package com.example.template.venue;

public record RoomDto(
    String id,
    String venueId,
    String name,
    int capacity,
    int rows,
    int cols,
    Boolean active,
    RoomTypeDto roomType,
    RoomLayoutDto roomLayout
) {
}
