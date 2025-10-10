package com.project.flightbooking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class LoginRequest {
    private String username;
    private String password;
}