package com.example.template.user;

public record ChangePasswordRequest(String currentPassword, String newPassword) {
}
