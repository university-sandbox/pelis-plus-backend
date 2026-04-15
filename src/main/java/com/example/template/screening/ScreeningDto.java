package com.example.template.screening;

import com.example.template.venue.RoomDto;
import com.example.template.venue.VenueDto;

public record ScreeningDto(
    String id,
    Long movieId,
    String movieTitle,
    VenueDto venue,
    RoomDto room,
    String date,
    String time,
    String format,
    Double price,
    Integer availableSeats,
    Integer totalSeats,
    String status
) {
}
