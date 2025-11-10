package com.project.flightbooking.security;

import com.project.flightbooking.security.JwtAuthenticationFilter;
import com.project.flightbooking.security.JwtTokenProvider;
import com.project.flightbooking.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    // It is spring security policy definition
    // It tells spring:
    // Which endpoints are public? Which require authentication? What filters should I use?
    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(JwtTokenProvider jwtTokenProvider, CustomUserDetailsService userDetailsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // CSRF protection only applies to web sessions using cookies.
                // our API uses stateless JWTs in headers → no CSRF risk -> Hence disabled
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // path-based matchers
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")   // <--- only ADMIN
                        .anyRequest().authenticated()
                )
                // Tells Spring Security:
                // “Run my custom JWT filter before your built-in username/password filter.”
                // Other filter of spring include UsernamePasswordAuthenticationFilter and many other
                // This allows Spring Security to process JWTs instead of form-based logins.
                .addFilterBefore(jwtAuthenticationFilter(), org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Password hashing algorithm
        return new BCryptPasswordEncoder();
    }
}