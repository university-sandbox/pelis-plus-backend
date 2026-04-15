package com.example.template.admin;

import com.example.template.domain.AppUser;
import com.example.template.domain.AppUserRepository;
import com.example.template.user.UserProfileDto;
import com.example.template.user.UserService;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AppUserRepository appUserRepository;
    private final UserService userService;

    public AdminUserController(AppUserRepository appUserRepository, UserService userService) {
        this.appUserRepository = appUserRepository;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<Page<UserProfileDto>> listUsers(
        @RequestParam(defaultValue = "1") int page
    ) {
        Page<UserProfileDto> users = appUserRepository.findAll(
            PageRequest.of(Math.max(0, page - 1), 20, Sort.by("createdAt").descending())
        ).map(u -> userService.getProfile(u.getId()));
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserProfileDto> getUser(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getProfile(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(
        @PathVariable UUID id,
        @RequestBody StatusRequest request
    ) {
        // In a real app, you'd have an active/banned field on user
        // For now, just return 200 OK
        appUserRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
        return ResponseEntity.ok().build();
    }

    public record StatusRequest(String status) {}
}
