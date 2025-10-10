package com.project.flightbooking.controller;

import com.project.flightbooking.dto.BookingRequest;
import com.project.flightbooking.dto.BookingResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(@RequestBody BookingRequest request) {
        BookingResponse resp = new BookingResponse();
        resp.setBookingCode("SAMPLE123");
        resp.setTotalAmount(1000.0);
        return ResponseEntity.ok(resp);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> cancelBooking(@PathVariable Long id) {
        return ResponseEntity.ok("cancelled (stub)");
    }
}
