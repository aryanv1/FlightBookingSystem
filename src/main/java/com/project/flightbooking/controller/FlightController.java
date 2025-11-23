package com.project.flightbooking.controller;

import com.project.flightbooking.dto.FlightResponse;
import com.project.flightbooking.model.Flight;
import com.project.flightbooking.service.FlightService;
import com.project.flightbooking.service.PricingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/flights")
public class FlightController {

    private final FlightService flightService;
    private final PricingService pricingService;

    public FlightController(FlightService flightService, PricingService pricingService) {
        this.flightService = flightService;
        this.pricingService = pricingService;
    }

    /**
     * Fetch all flights with dynamic fare applied.
     */
    @GetMapping
    public ResponseEntity<List<FlightResponse>> getAllFlights() {
        List<Flight> flights = flightService.findAll();

        List<FlightResponse> response = flights.stream()
                // Maps every flight with flightResponse Dto by adjusting the fare
                .map(flight -> {
                    BigDecimal dynamicFare = pricingService.calculateDynamicFare(flight);
                    return toFlightResponse(flight, dynamicFare);
                })
                .collect(Collectors.toList());
                // Collects result back to list

        return ResponseEntity.ok(response);
    }

    /**
     * Fetch single flight by ID and return its dynamic fare.
     */
    @GetMapping("/{id}")
    public ResponseEntity<FlightResponse> getFlight(@PathVariable Long id) {
        return flightService.findById(id)
                // Maps each flight with adjusted fare Dto
                .map(flight -> {
                    BigDecimal dynamicFare = pricingService.calculateDynamicFare(flight);
                    FlightResponse dto = toFlightResponse(flight, dynamicFare);
                    return ResponseEntity.ok(dto);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    public static FlightResponse toFlightResponse(Flight f, BigDecimal dynamicFare) {
        FlightResponse dto = new FlightResponse();
        dto.setId(f.getId());
        dto.setFlightNumber(f.getFlightNumber());
        dto.setAirline(f.getAirline());
        dto.setOrigin(f.getOrigin());
        dto.setDestination(f.getDestination());
        dto.setDepartureTime(f.getDepartureTime());
        dto.setArrivalTime(f.getArrivalTime());
        dto.setTotalSeats(f.getTotalSeats());
        dto.setRemainingSeats(f.getRemainingSeats());
        dto.setBaseFare(f.getBaseFare());
        dto.setDynamicFare(dynamicFare);
        dto.setStatus(f.getStatus());
        return dto;
    }
}