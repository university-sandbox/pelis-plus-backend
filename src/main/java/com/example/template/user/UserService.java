package com.example.template.user;

import com.example.template.domain.AppUser;
import com.example.template.domain.AppUserRepository;
import com.example.template.membership.ActiveMembershipDto;
import com.example.template.membership.ActiveMembershipRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final AppUserRepository appUserRepository;
    private final ActiveMembershipRepository activeMembershipRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
        AppUserRepository appUserRepository,
        ActiveMembershipRepository activeMembershipRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.appUserRepository = appUserRepository;
        this.activeMembershipRepository = activeMembershipRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserProfileDto updateProfile(UUID userId, String name, String email) {
        AppUser user = appUserRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (name != null && !name.isBlank()) {
            user.setName(name.trim());
        }
        if (email != null && !email.isBlank()) {
            String normalized = email.trim().toLowerCase(Locale.ROOT);
            if (!normalized.equals(user.getEmail()) && appUserRepository.existsByEmail(normalized)) {
                throw new IllegalArgumentException("Email already in use");
            }
            user.setEmail(normalized);
        }

        AppUser saved = appUserRepository.save(user);
        return toProfileDto(saved);
    }

    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        AppUser user = appUserRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        appUserRepository.save(user);
    }

    public UserProfileDto getProfile(UUID userId) {
        AppUser user = appUserRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));
        return toProfileDto(user);
    }

    private UserProfileDto toProfileDto(AppUser user) {
        ActiveMembershipDto membershipDto = activeMembershipRepository.findByUserId(user.getId())
            .map(m -> new ActiveMembershipDto(
                m.getPlan().getId().toString(),
                m.getPlan().getName(),
                m.getExpiresAt().toString(),
                m.getTicketsUsed(),
                m.getPlan().getTicketsPerMonth(),
                m.getDiscountUsed() != null ? m.getDiscountUsed().doubleValue() : 0.0
            ))
            .orElse(null);

        return new UserProfileDto(
            user.getId().toString(),
            user.getName(),
            user.getEmail(),
            null,
            membershipDto,
            user.getCreatedAt() != null ? user.getCreatedAt().toString() : null
        );
    }
}
