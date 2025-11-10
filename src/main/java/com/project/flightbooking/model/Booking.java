package com.project.flightbooking.model;

import com.project.flightbooking.enums.BookingStatus;
import com.project.flightbooking.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bookings", indexes = {
        @Index(name = "idx_booking_ref", columnList = "bookingRef")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String bookingRef; // e.g., BK-20251008-ABC123

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id", nullable = false)
    private Flight flight;

    @Column(nullable = false)
    private Integer seatCount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal farePerSeat;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalFare;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status; // PENDING, CONFIRMED, CANCELLED, REFUNDED

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus paymentStatus; // INIT, SUCCESS, FAILED

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // helper factory
    public static Booking create(User user, Flight flight, int seatCount, BigDecimal farePerSeat) {
        Booking b = new Booking();
        b.setBookingRef("BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        b.setUser(user);
        b.setFlight(flight);
        b.setSeatCount(seatCount);
        b.setFarePerSeat(farePerSeat);
        b.setTotalFare(farePerSeat.multiply(BigDecimal.valueOf(seatCount)));
        b.setStatus(BookingStatus.PENDING);
        b.setPaymentStatus(PaymentStatus.INITIATED);
        return b;
    }
}