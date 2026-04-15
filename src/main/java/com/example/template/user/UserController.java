package com.example.template.user;

import com.example.template.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileDto> updateProfile(
        @RequestBody UpdateProfileRequest request,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(userService.updateProfile(
            principal.getUser().getId(),
            request.name(),
            request.email()
        ));
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(
        @RequestBody ChangePasswordRequest request,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        userService.changePassword(
            principal.getUser().getId(),
            request.currentPassword(),
            request.newPassword()
        );
        return ResponseEntity.noContent().build();
    }
}
