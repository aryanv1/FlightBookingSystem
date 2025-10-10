package com.project.flightbooking.controller;

import com.project.flightbooking.dto.FlightResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/flights")
public class FlightController {

    @GetMapping
    public ResponseEntity<List<FlightResponse>> getAllFlights() {
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FlightResponse> getFlight(@PathVariable Long id) {
        return ResponseEntity.ok(new FlightResponse());
    }
}
