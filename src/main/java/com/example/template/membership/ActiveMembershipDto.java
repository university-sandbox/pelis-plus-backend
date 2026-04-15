package com.example.template.membership;

public record ActiveMembershipDto(
    String planId,
    String planName,
    String expiresAt,
    Integer ticketsUsed,
    Integer ticketsTotal,
    Double discountUsed
) {
}
