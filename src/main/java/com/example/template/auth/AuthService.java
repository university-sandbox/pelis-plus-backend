package com.example.template.auth;

import com.example.template.domain.AppUser;
import com.example.template.domain.AppUserRepository;
import com.example.template.domain.Role;
import com.example.template.security.JwtService;
import com.example.template.security.UserPrincipal;
import java.util.Locale;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(
        AppUserRepository appUserRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        AuthenticationManager authenticationManager
    ) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);

        if (appUserRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Email already registered");
        }

        AppUser user = new AppUser();
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);

        AppUser savedUser = appUserRepository.save(user);
        UserPrincipal principal = new UserPrincipal(savedUser);
        String token = jwtService.generateToken(principal);

        return new AuthResponse(
            token,
            "Bearer",
            jwtService.extractExpiration(token),
            savedUser.getId(),
            savedUser.getEmail(),
            savedUser.getRole()
        );
    }

    public AuthResponse login(AuthRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);

        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(normalizedEmail, request.password())
        );

        AppUser user = appUserRepository.findByEmail(normalizedEmail)
            .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        UserPrincipal principal = new UserPrincipal(user);
        String token = jwtService.generateToken(principal);

        return new AuthResponse(
            token,
            "Bearer",
            jwtService.extractExpiration(token),
            user.getId(),
            user.getEmail(),
            user.getRole()
        );
    }
}
