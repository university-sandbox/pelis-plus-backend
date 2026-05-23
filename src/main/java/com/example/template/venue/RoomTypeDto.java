package com.example.template.venue;

public record RoomTypeDto(
    String id,
    String code,
    String name,
    String description,
    Boolean active
) {
}
