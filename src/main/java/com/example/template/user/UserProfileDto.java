package com.example.template.user;

import com.example.template.membership.ActiveMembershipDto;

public record UserProfileDto(
    String id,
    String name,
    String email,
    String avatar,
    ActiveMembershipDto membership,
    String createdAt
) {
}
