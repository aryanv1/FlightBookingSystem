package com.project.flightbooking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlightRequest {
    private String flightNumber;
    private String airline;
    private String origin;
    private String destination;
    private String departureTime; // ISO string e.g. 2026-01-15T09:30:00+05:30
    private String arrivalTime;
    private Integer totalSeats;
    private BigDecimal baseFare;

}