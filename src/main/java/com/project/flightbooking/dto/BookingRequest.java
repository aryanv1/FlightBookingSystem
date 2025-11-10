package com.project.flightbooking.dto;

import lombok.Data;

@Data
public class BookingRequest {
    private Long flightId;
    private Integer seatCount;
}