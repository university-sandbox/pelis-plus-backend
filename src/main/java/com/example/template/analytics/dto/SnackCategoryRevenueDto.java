package com.example.template.analytics.dto;

import java.math.BigDecimal;

public record SnackCategoryRevenueDto(
    String category,
    BigDecimal revenue
) {}
