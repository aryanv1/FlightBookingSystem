package com.project.flightbooking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlightResponse {
    private Long id;
    private String flightNumber;
    private String airline;
    private String origin;
    private String destination;
    private ZonedDateTime departureTime;
    private ZonedDateTime arrivalTime;
    private Integer totalSeats;
    private Integer remainingSeats;
    private BigDecimal baseFare;
    private String status;
}