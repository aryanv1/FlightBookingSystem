package com.project.flightbooking.dto;

import java.time.LocalDateTime;

public class FlightResponse {
    private Long id;
    private String flightNumber;
    private String source;
    private String destination;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private Integer availableSeats;
    private Double currentPrice;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
}
