package com.project.flightbooking.controller;

import com.project.flightbooking.dto.FlightRequest;
import com.project.flightbooking.dto.FlightResponse;
import com.project.flightbooking.model.Flight;
import com.project.flightbooking.service.FlightService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/flights")
public class AdminFlightController {

    private final FlightService flightService;

    public AdminFlightController(FlightService flightService) {
        this.flightService = flightService;
    }

    // method-level annotations
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    // ensure only ADMIN can call; SecurityConfig + @EnableMethodSecurity allows @PreAuthorize
    // If you want a method-level guard, add: @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FlightResponse> createFlight(@RequestBody FlightRequest request) {
        Flight f = flightService.createFlight(request);
        FlightResponse resp = toResponse(f);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FlightResponse> getFlight(@PathVariable Long id) {
        return flightService.findById(id)
                .map(f -> ResponseEntity.ok(toResponse(f)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private FlightResponse toResponse(Flight f) {
        FlightResponse r = new FlightResponse();
        r.setId(f.getId());
        r.setFlightNumber(f.getFlightNumber());
        r.setAirline(f.getAirline());
        r.setOrigin(f.getOrigin());
        r.setDestination(f.getDestination());
        r.setDepartureTime(f.getDepartureTime());
        r.setArrivalTime(f.getArrivalTime());
        r.setTotalSeats(f.getTotalSeats());
        r.setRemainingSeats(f.getRemainingSeats());
        r.setBaseFare(f.getBaseFare());
        r.setStatus(f.getStatus());
        return r;
    }
}