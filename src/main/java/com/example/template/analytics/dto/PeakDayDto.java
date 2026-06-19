package com.example.template.analytics.dto;

public record PeakDayDto(
    String dayOfWeek,
    long ticketsSold
) {}
