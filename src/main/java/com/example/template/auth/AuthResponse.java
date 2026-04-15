package com.example.template.auth;

import com.example.template.domain.Role;
import java.time.Instant;
import java.util.UUID;

public record AuthResponse(
    String accessToken,
    String tokenType,
    Instant expiresAt,
    UUID userId,
    String email,
    Role role
) {
}
