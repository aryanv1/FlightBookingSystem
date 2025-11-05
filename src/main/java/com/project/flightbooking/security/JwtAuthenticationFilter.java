package com.project.flightbooking.security;

import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.project.flightbooking.service.CustomUserDetailsService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.IOException;
import java.util.Collections;

// OncePerRequest ensures this filter runs exactly once per request
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    // custom Spring Security filter that runs before your controllers.
    // Intercept every HTTP request, extract the JWT from the Authorization header, validate it, and if valid, tell Spring:
    // “Hey, this request belongs to user X, authenticated successfully.”
    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, CustomUserDetailsService userDetailsService) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);
            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                String username = tokenProvider.getUsernameFromToken(jwt);
                String role = tokenProvider.getRoleFromToken(jwt);
                // UserDetails object (Spring’s standard representation of an authenticated user)
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // Wraps the UserDetails into an Authentication object that Spring Security understands.
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
                );

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                // Tells Spring Security:
                // This request belongs to this user — consider them authenticated.
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            // log optionally
        }
        // Passes control to the next filter (or controller) in the chain.
        // If authentication succeeded, controllers execute normally.
        // If it failed (e.g., token missing/invalid), the request stops at the security layer (401).
        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest req) {
        String bearer = req.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}