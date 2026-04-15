package com.example.template.auth;

import java.time.Instant;
import java.util.UUID;

public record AuthResponse(
    String token,
    String tokenType,
    Instant expiresAt,
    UUID userId,
    String name,
    String email,
    String role
) {
}
