package com.example.template.analytics.dto;

import java.math.BigDecimal;

public record RevenueSeriesDto(
    String label,
    BigDecimal revenue
) {}
