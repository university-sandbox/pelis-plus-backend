package com.example.template.analytics.dto;

import java.math.BigDecimal;

public record TopSnackDto(
    String snackName,
    long totalQuantity,
    BigDecimal totalRevenue
) {}
