package com.project.flightbooking.service;

import com.project.flightbooking.dto.LoginRequest;
import com.project.flightbooking.dto.RegisterRequest;
import com.project.flightbooking.model.User;
import com.project.flightbooking.model.RefreshToken;
import com.project.flightbooking.repository.UserRepository;
import com.project.flightbooking.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service // Equivalent to writing @Component
// @Service has extra semantics telling that it contains business logic and not just random component
// Specialised form of @Component
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenService refreshTokenService;

    // Dependency injection through constructor
    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.refreshTokenService = refreshTokenService;
    }

    public User register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new IllegalArgumentException("Username already taken");
        }
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }
        User u = new User();
        u.setUsername(req.getUsername());
        u.setPassword(passwordEncoder.encode(req.getPassword()));
        u.setEmail(req.getEmail());
        u.setRole("USER");
        u.setIsActive(true);
        return userRepository.save(u);
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        // You don’t specify whether it was the username or password that was wrong —
        // this is intentional to prevent attackers from guessing which field failed.

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        // passwordEncoder.matches() does this:
		// Hashes the raw password using the same algorithm (e.g., BCrypt).
		// Compares the resulting hash to the stored hash.

        String accessToken = tokenProvider.generateAccessToken(user.getUsername(), user.getRole());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        // “Bearer” is a token type identifier
        // It means: “The client must send this token in the HTTP Authorization header using the format:
        // Authorization: Bearer <token>”
        // here we kept tokenType to indicate how client should use the token
        // Also helps in creating full header dynamically: "Authorization": `${response.tokenType} ${response.accessToken}`
        return new AuthResponse(accessToken, "Bearer", refreshToken.getToken());
    }

    public static class AuthResponse {
        private String accessToken;
        private String tokenType;
        private String refreshToken;

        public AuthResponse(String accessToken, String tokenType, String refreshToken) {
            this.accessToken = accessToken;
            this.tokenType = tokenType;
            this.refreshToken = refreshToken;
        }
        public String getAccessToken() { return accessToken; }
        public String getTokenType() { return tokenType; }
        public String getRefreshToken() { return refreshToken; }
    }
}