package com.example.template.analytics.dto;

import java.math.BigDecimal;

public record KpiOverviewDto(
    BigDecimal totalRevenue,
    long totalTicketsSold,
    BigDecimal averageOrderValue,
    long totalRegisteredUsers,
    long activeMemberships,
    double averageOccupancyRate
) {}
