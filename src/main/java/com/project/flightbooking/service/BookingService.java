package com.project.flightbooking.service;

import com.project.flightbooking.dto.BookingRequest;
import com.project.flightbooking.model.Booking;
import com.project.flightbooking.enums.BookingStatus;
import com.project.flightbooking.model.Flight;
import com.project.flightbooking.enums.PaymentStatus;
import com.project.flightbooking.model.User;
import com.project.flightbooking.repository.BookingRepository;
import com.project.flightbooking.repository.FlightRepository;
import com.project.flightbooking.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class BookingService {

    private final FlightRepository flightRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    public BookingService(FlightRepository flightRepository,
                          BookingRepository bookingRepository,
                          UserRepository userRepository) {
        this.flightRepository = flightRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
    }

    /**
     * Reserve seats atomically and create a PENDING booking.
     * Uses pessimistic lock on Flight row to prevent overbooking.
     */
    @Transactional
    public Booking reserveSeats(String username, BookingRequest req) {
        if (req.getSeatCount() == null || req.getSeatCount() <= 0) {
            throw new IllegalArgumentException("seatCount must be > 0");
        }
        // load user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        // lock flight row
        Flight flight = flightRepository.findByIdForUpdate(req.getFlightId())
                .orElseThrow(() -> new IllegalArgumentException("Flight not found: " + req.getFlightId()));

        if (!"SCHEDULED".equalsIgnoreCase(flight.getStatus())) {
            throw new IllegalStateException("Flight is not available for booking: " + flight.getStatus());
        }

        int seatsRequested = req.getSeatCount();
        if (flight.getRemainingSeats() == null) flight.setRemainingSeats(flight.getTotalSeats());
        if (flight.getRemainingSeats() < seatsRequested) {
            throw new IllegalStateException("Not enough seats available. remaining=" + flight.getRemainingSeats());
        }

        // compute farePerSeat via simple baseFare (hook for pricing engine later)
        BigDecimal farePerSeat = flight.getBaseFare();
        Booking booking = Booking.create(user, flight, seatsRequested, farePerSeat);

        // decrement remaining seats and persist
        flight.setRemainingSeats(flight.getRemainingSeats() - seatsRequested);
        // save both flight and booking within same transaction
        flightRepository.save(flight);
        bookingRepository.save(booking);

        return booking;
    }

    /**
     * Confirm booking (simulate payment). This should be called after payment success or webhook.
     */
    @Transactional
    public Booking confirmBooking(String bookingRef) {
        Booking b = bookingRepository.findByBookingRef(bookingRef)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingRef));

        if (b.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("Booking not in PENDING state");
        }
        b.setPaymentStatus(PaymentStatus.SUCCESS);
        b.setStatus(BookingStatus.CONFIRMED);

        bookingRepository.save(b);
        return b;
    }

    public Optional<Booking> findByRef(String bookingRef) {
        return bookingRepository.findByBookingRef(bookingRef);
    }
}