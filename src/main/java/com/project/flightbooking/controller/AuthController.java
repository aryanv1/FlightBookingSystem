package com.project.flightbooking.controller;

import com.project.flightbooking.dto.LoginRequest;
import com.project.flightbooking.dto.RegisterRequest;
import com.project.flightbooking.dto.JwtAuthResponse;
import com.project.flightbooking.dto.TokenRefreshRequest;
import com.project.flightbooking.dto.TokenRefreshResponse;
import com.project.flightbooking.service.AuthService;
import com.project.flightbooking.service.RefreshTokenService;
import com.project.flightbooking.model.RefreshToken;
import jakarta.servlet.http.Cookie;
import com.project.flightbooking.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

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
    // ResponseEntity<T> is a generic class in Spring that represents the entire HTTP response:
    // status code, headers and body
    // here '?' means the body can be of any type we are not specifying it explicitly
    // .ok means status code 200 -> and here we are sending string as type in ResponseEntity
    // ResponseEntity.ok(body) === new ResponseEntity<>(body, HttpStatus.OK)

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req, HttpServletResponse response) {
        // HttpServletResponse = “the raw HTTP response being built and sent back to the browser.”
        // It lets us add cookies -> It's a low-level servlet object if you need manual control.

        // AuthService.AuthResponse resp = authService.login(req);
        // JwtAuthResponse r = new JwtAuthResponse(resp.getAccessToken(), resp.getTokenType(), resp.getRefreshToken());
        // return ResponseEntity.ok(r);
        AuthService.AuthResponse resp = authService.login(req);

        // Set refresh token as HttpOnly cookie
        Cookie cookie = new Cookie("refreshToken", resp.getRefreshToken());
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // set to true if using HTTPS
        // Always set cookie.setSecure(true) when deploying over HTTPS. false is only for local testing.
        cookie.setPath("/"); // accessible to entire domain
        cookie.setMaxAge((int) Duration.ofDays(7).toSeconds());
        response.addCookie(cookie);

        // Send only access token back in JSON
        JwtAuthResponse r = new JwtAuthResponse(resp.getAccessToken(), resp.getTokenType(), null);
        return ResponseEntity.ok(r);
    }

    @Transactional
    @PostMapping("/refresh")
    // required = false -> avoid exception if cookie is missing -> we handle that case
    public ResponseEntity<?> refreshToken(@CookieValue(value = "refreshToken", required = false) String refreshTokenCookie,
                                          HttpServletResponse response) {
        // HttpServletResponse response — used to set a new cookie in the response.
        if (refreshTokenCookie == null) {
            return ResponseEntity.badRequest().body("Missing refresh token cookie");
            // 400 BadRequest
        }

        RefreshToken token = refreshTokenService.findByToken(refreshTokenCookie)
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));

        refreshTokenService.verifyExpiration(token);

        refreshTokenService.deleteByUserId(token.getUser().getId());
        RefreshToken newToken = refreshTokenService.createRefreshToken(token.getUser().getId());

        // Rotate cookie
        Cookie cookie = new Cookie("refreshToken", newToken.getToken());
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge((int) Duration.ofDays(7).toSeconds());
        response.addCookie(cookie);

        // Generate new access token
        String newAccessToken = jwtTokenProvider.generateAccessToken(
                token.getUser().getUsername(),
                token.getUser().getRole()
        );

        return ResponseEntity.ok(new TokenRefreshResponse(newAccessToken, null));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@CookieValue(value = "refreshToken", required = false) String refreshTokenCookie,
                                    HttpServletResponse response) {
        // If we want multi-device support, only delete the specific refresh token row
        // do not delete all user tokens.
        if (refreshTokenCookie != null) {
            RefreshToken token = refreshTokenService.findByToken(refreshTokenCookie).orElse(null);
            if (token != null) {
                refreshTokenService.deleteByUserId(token.getUser().getId());
            }
        }

        // Clear the cookie
        // To remove a cookie in the browser, set the same cookie name with an empty value
        // and Max-Age=0. Browser will delete it.
        Cookie cookie = new Cookie("refreshToken", "");
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        return ResponseEntity.ok("Logged out successfully");
    }
}