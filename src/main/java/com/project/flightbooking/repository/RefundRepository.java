package com.project.flightbooking.repository;

import com.project.flightbooking.model.RefundTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefundRepository extends JpaRepository<RefundTransaction, Long> {
    Optional<RefundTransaction> findByProviderRefundId(String providerRefundId);

    // Useful for idempotency: find refund for booking in these statuses
    Optional<RefundTransaction> findFirstByBookingIdAndStatusIn(Long bookingId, java.util.List<com.project.flightbooking.enums.RefundStatus> statuses);
}