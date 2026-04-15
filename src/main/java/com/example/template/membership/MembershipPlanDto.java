package com.example.template.membership;

import java.util.List;

public record MembershipPlanDto(
    String id,
    String name,
    Double price,
    String validity,
    List<BenefitDto> benefits,
    Integer discountPercentage,
    Integer ticketsPerMonth,
    Boolean recommended,
    String color
) {
}
