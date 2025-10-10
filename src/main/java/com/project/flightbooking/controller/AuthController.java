package com.project.flightbooking.controller;

import com.project.flightbooking.dto.LoginRequest;
import com.project.flightbooking.dto.RegisterRequest;
import com.project.flightbooking.dto.JwtAuthResponse;
import com.project.flightbooking.dto.TokenRefreshRequest;
import com.project.flightbooking.dto.TokenRefreshResponse;
import com.project.flightbooking.service.AuthService;
import com.project.flightbooking.service.RefreshTokenService;
import com.project.flightbooking.model.RefreshToken;
import com.project.flightbooking.security.JwtTokenProvider;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Data
@AllArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        var user = authService.register(req);
        return ResponseEntity.ok("User registered: " + user.getUsername());
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        AuthService.AuthResponse resp = authService.login(req);
        JwtAuthResponse r = new JwtAuthResponse(resp.getAccessToken(), resp.getTokenType(), resp.getRefreshToken());
        return ResponseEntity.ok(r);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody TokenRefreshRequest request) {
        String requestRefreshToken = request.getRefreshToken();
        RefreshToken token = refreshTokenService.findByToken(requestRefreshToken)
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));

        // verify expiration - will throw if expired and deletes token
        refreshTokenService.verifyExpiration(token);

        // rotate: delete all user tokens and create a new refresh token
        refreshTokenService.deleteByUserId(token.getUser().getId());
        RefreshToken newToken = refreshTokenService.createRefreshToken(token.getUser().getId());

        // issue new access token
        String newAccessToken = jwtTokenProvider.generateAccessToken(token.getUser().getUsername(), token.getUser().getRole());

        TokenRefreshResponse resp = new TokenRefreshResponse(newAccessToken, newToken.getToken());
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody TokenRefreshRequest request) {
        String requestRefreshToken = request.getRefreshToken();
        RefreshToken token = refreshTokenService.findByToken(requestRefreshToken)
                .orElse(null);
        if (token != null) {
            refreshTokenService.deleteByUserId(token.getUser().getId());
        }
        return ResponseEntity.ok("Logged out successfully");
    }
}