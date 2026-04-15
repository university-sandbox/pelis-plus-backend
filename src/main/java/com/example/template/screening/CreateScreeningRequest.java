package com.example.template.screening;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record CreateScreeningRequest(
    Long movieId,
    UUID roomId,
    LocalDate date,
    LocalTime time,
    String format,
    BigDecimal price
) {
}
