package com.project.flightbooking.dto;

public class BookingRequest {
    private Long flightId;
    private Integer seats;
    public Long getFlightId() { return flightId; }
    public void setFlightId(Long flightId) { this.flightId = flightId; }
    public Integer getSeats() { return seats; }
    public void setSeats(Integer seats) { this.seats = seats; }
}
