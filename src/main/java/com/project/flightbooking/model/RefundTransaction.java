package com.project.flightbooking.model;

import com.project.flightbooking.enums.RefundStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * RefundTransaction
 * -----------------
 * Records a refund attempt for a booking.
 *
 * Fields:
 *  - booking: the booking being refunded
 *  - providerPaymentId: original provider payment id (Razorpay payment id)
 *  - providerRefundId: provider's refund id once provider returns it
 *  - amount: amount requested for refund (in currency units, e.g. INR)
 *  - status: lifecycle (INITIATED, PROCESSING, SUCCESS, FAILED)
 *  - providerResponse: raw provider response (JSON string) for audit
 *
 * This entity is intentionally simple and audit-friendly.
 */
@Entity
@Table(name = "refund_transactions", indexes = {
        @Index(name = "idx_refund_provider_id", columnList = "providerRefundId"),
        @Index(name = "idx_refund_booking", columnList = "booking_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefundTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Link to booking
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    // Original provider payment id (Razorpay payment id)
    @Column(nullable = false)
    private String providerPaymentId;

    // Provider refund id (Razorpay refund id), may be null until provider responds
    @Column(nullable = true, unique = true)
    private String providerRefundId;

    // Amount to refund (currency units, stored with 2 decimal scale)
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RefundStatus status;

    // Raw provider response (JSON or message) for auditing/debugging
    @Column(columnDefinition = "TEXT")
    private String providerResponse;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}