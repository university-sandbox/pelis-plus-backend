package com.example.template.seat;

import java.util.List;

public record SeatMapDto(
    String screeningId,
    List<String> rows,
    List<Integer> cols,
    List<List<SeatDto>> seats
) {
}
