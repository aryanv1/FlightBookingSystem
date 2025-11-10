package com.project.flightbooking.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BookingResponse {
    private String bookingRef;
    private Long flightId;
    private Integer seatCount;
    private BigDecimal totalFare;
    private String status;
    private LocalDateTime createdAt;
}