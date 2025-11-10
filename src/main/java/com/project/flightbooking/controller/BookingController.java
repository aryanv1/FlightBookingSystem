package com.project.flightbooking.controller;

import com.project.flightbooking.dto.BookingRequest;
import com.project.flightbooking.dto.BookingResponse;
import com.project.flightbooking.model.Booking;
import com.project.flightbooking.service.BookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * Reserve seats (creates a PENDING booking and decrements flight remaining seats).
     * Requires authenticated user.
     */
    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody BookingRequest req) {

        String username = userDetails.getUsername();
        Booking booking = bookingService.reserveSeats(username, req);
        BookingResponse resp = toResponse(booking);
        return ResponseEntity.ok(resp);
    }

    /**
     * Confirm after payment success (mock). In real flow this is invoked by payment gateway webhook
     */
    @PostMapping("/{bookingRef}/confirm")
    public ResponseEntity<BookingResponse> confirmBooking(@PathVariable String bookingRef) {
        Booking b = bookingService.confirmBooking(bookingRef);
        return ResponseEntity.ok(toResponse(b));
    }

    @GetMapping("/{bookingRef}")
    public ResponseEntity<BookingResponse> getBooking(@PathVariable String bookingRef) {
        return bookingService.findByRef(bookingRef)
                .map(b -> ResponseEntity.ok(toResponse(b)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private BookingResponse toResponse(Booking b) {
        BookingResponse r = new BookingResponse();
        r.setBookingRef(b.getBookingRef());
        r.setFlightId(b.getFlight().getId());
        r.setSeatCount(b.getSeatCount());
        r.setTotalFare(b.getTotalFare());
        r.setStatus(b.getStatus().name());
        r.setCreatedAt(b.getCreatedAt());
        return r;
    }
}