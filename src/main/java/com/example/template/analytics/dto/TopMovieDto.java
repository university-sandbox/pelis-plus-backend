package com.example.template.analytics.dto;

import java.math.BigDecimal;

public record TopMovieDto(
    String movieTitle,
    long ticketsSold,
    BigDecimal revenue
) {}
