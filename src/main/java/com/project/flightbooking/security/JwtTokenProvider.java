package com.project.flightbooking.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${app.jwtSecret}")
    private String jwtSecret;

    @Value("${app.jwtExpirationMs}")
    private long jwtExpirationMs;

    private Key key;

    @PostConstruct
    public void init() {
        // Use raw bytes (must be at least 256 bits for HS256)
        byte[] secretBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(secretBytes);
    }

    public String generateAccessToken(String username, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtExpirationMs);
        // io.jsonwebtoken (JJWT) library to create and sign a JWT
        // 'JWT' has three parts:
        // 1. Header - Algorithm + Token type
        // 2. Payload - Claims(User data and metadata)
        // 3. Signature - Cryptographic proof of authenticity
        return Jwts.builder() // its jwt builder object stating i am about to build new token
                .setSubject(username) // claim stating unique identity of the user
                .claim("role", role) // adding custom claim to jwt payload
                // custom claims later used for authorization -> only admin can access certain endpoints
                .setIssuedAt(now) // timestamp of token created
                .setExpiration(expiry) // time after which token becomes invalid
                .signWith(key, SignatureAlgorithm.HS256) // digitally signs the token
                // key - the secret key used to sign and later verify the code
                // SignatureAlgorithm - The algorithm used
                // So the library takes your header + payload, generates
                // a hash using the secret key, and produces the signature.
                .compact();
                // assembles everything (header + payload + signature) into one encoded string
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public String getUsernameFromToken(String token) {
        Claims c = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
        return c.getSubject();
    }

    public String getRoleFromToken(String token) {
        Claims c = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
        Object role = c.get("role");
        return role != null ? role.toString() : null;
    }
}