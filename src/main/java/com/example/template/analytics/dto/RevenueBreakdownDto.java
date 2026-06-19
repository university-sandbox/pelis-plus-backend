package com.example.template.analytics.dto;

import java.math.BigDecimal;

public record RevenueBreakdownDto(
    BigDecimal ticketRevenue,
    BigDecimal snackRevenue,
    BigDecimal totalDiscount
) {}
