package com.project.flightbooking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefundDto {
    private Long id;
    private String providerRefundId;
    private String providerPaymentId;
    private BigDecimal amount;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}