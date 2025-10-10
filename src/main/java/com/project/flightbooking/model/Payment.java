package com.project.flightbooking.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.project.flightbooking.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String transactionId; // unique internal or gateway reference

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false, length = 20)
    private String method; // e.g. CARD, UPI, NETBANKING, WALLET

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    private LocalDateTime initiatedAt;
    private LocalDateTime completedAt;
}
