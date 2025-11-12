package com.project.flightbooking.service;

import com.project.flightbooking.enums.BookingStatus;
import com.project.flightbooking.enums.RefundStatus;
import com.project.flightbooking.model.*;
import com.project.flightbooking.repository.BookingRepository;
import com.project.flightbooking.repository.FlightRepository;
import com.project.flightbooking.repository.PaymentRepository;
import com.project.flightbooking.repository.RefundRepository;
import com.razorpay.Refund;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import jakarta.transaction.Transactional;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * RefundService
 *
 * Responsibilities:
 *  - Compute refund percentage by cancellation policy (time-based)
 *  - Create RefundTransaction records (idempotent)
 *  - Call Razorpay Refund API and persist providerRefundId and status
 *  - Handle Razorpay refund webhooks (idempotent)
 *  - Restore seats when refund completes successfully
 */
@Service
public class RefundService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final FlightRepository flightRepository;

    @Value("${razorpay.key_id}")
    private String razorpayKeyId;

    @Value("${razorpay.key_secret}")
    private String razorpayKeySecret;

    public RefundService(BookingRepository bookingRepository,
                         PaymentRepository paymentRepository,
                         RefundRepository refundRepository,
                         FlightRepository flightRepository) {
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
        this.flightRepository = flightRepository;
    }

    /**
     * Default refund policy (hardcoded tiers).
     * Returns refund fraction (0.0 - 1.0).
     */
    private BigDecimal computeRefundPercent(ZonedDateTime departure, ZonedDateTime now) {
        long hours = Duration.between(now, departure).toHours();
        if (hours >= 72) return BigDecimal.valueOf(0.90);
        if (hours >= 24) return BigDecimal.valueOf(0.70);
        if (hours >= 6)  return BigDecimal.valueOf(0.40);
        if (hours >= 0)  return BigDecimal.valueOf(0.10);
        return BigDecimal.ZERO; // flight departed
    }

    /**
     * Initiate refund for bookingRef.
     * This method is idempotent:
     *  - If a RefundTransaction already exists for the booking in INITIATED/PROCESSING/SUCCESS, it returns that record or throws on impossible states.
     */
    @Transactional
    public RefundTransaction initiateRefund(String bookingRef) throws Exception {
        Booking booking = bookingRepository.findByBookingRef(bookingRef)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingRef));

        // Check booking eligibility
        BookingStatus bstatus = booking.getStatus();
        if (bstatus != BookingStatus.CONFIRMED && bstatus != BookingStatus.CANCELLED) {
            throw new IllegalStateException("Booking not eligible for refund. Current status: " + bstatus);
        }

        // Check if a refund already exists for this booking with relevant statuses (idempotency)
        List<RefundStatus> inProgress = Arrays.asList(RefundStatus.INITIATED, RefundStatus.PROCESSING, RefundStatus.SUCCESS);
        Optional<RefundTransaction> existing = refundRepository.findFirstByBookingIdAndStatusIn(booking.getId(), inProgress);
        if (existing.isPresent()) {
            System.out.println("Refund already exists for booking " + bookingRef + " with status " + existing.get().getStatus());
            return existing.get();
        }

        // Get provider Payment record for this booking
        Payment payment = paymentRepository.findByBookingId(booking.getId())
                .orElseThrow(() -> new IllegalStateException("Payment record not found for booking: " + bookingRef));

        if (payment.getProviderPaymentId() == null) {
            throw new IllegalStateException("Missing providerPaymentId; cannot initiate refund");
        }

        // Compute refund amount using policy
        ZonedDateTime departure = booking.getFlight().getDepartureTime();
        ZonedDateTime now = ZonedDateTime.now(departure.getZone());
        BigDecimal percent = computeRefundPercent(departure, now);

        if (percent.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("No refund allowed as per policy (flight departed or no eligible window).");
        }

        BigDecimal refundAmount = booking.getTotalFare().multiply(percent).setScale(2, RoundingMode.HALF_UP);

        // Create local RefundTransaction in INITIATED state (idempotent insert)
        RefundTransaction rt = new RefundTransaction();
        rt.setBooking(booking);
        rt.setProviderPaymentId(payment.getProviderPaymentId());
        rt.setAmount(refundAmount);
        rt.setStatus(RefundStatus.INITIATED);
        refundRepository.save(rt);

        // Call Razorpay refund API
        RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

        JSONObject req = new JSONObject();
        req.put("amount", refundAmount.multiply(BigDecimal.valueOf(100)).intValueExact()); // Razorpay expects paise
        req.put("speed", "normal");
        req.put("notes", new JSONObject().put("bookingRef", bookingRef));

        try {
            Refund refund = client.Payments.refund(payment.getProviderPaymentId(), req);

            // Update RefundTransaction from provider response
            rt.setProviderRefundId(refund.get("id"));
            rt.setStatus(RefundStatus.PROCESSING); // provider accepted request; final success via webhook
            rt.setProviderResponse(refund.toString());
            refundRepository.save(rt);

            // Optionally mark booking CANCELLED (if not already)
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);

            System.out.println("Refund initiated: providerRefundId=" + refund.get("id") + ", booking=" + bookingRef);
            return rt;
        } catch (RazorpayException e) {
            rt.setStatus(RefundStatus.FAILED);
            rt.setProviderResponse(e.getMessage());
            refundRepository.save(rt);

            System.out.println("Razorpay refund API failed: " + e.getMessage());
            throw new RuntimeException("Razorpay refund failed: " + e.getMessage(), e);
        }
    }

    /**
     * Handle refund webhook from provider (idempotent).
     * providerRefundId: Razorpay refund id from webhook payload.
     * eventPayload: raw JSON for auditing.
     *
     * On successful refund:
     *  - Mark refund SUCCESS
     *  - Mark booking REFUNDED (if not already)
     *  - Restore seats back to flight
     *
     * On failed refund:
     *  - Mark refund FAILED
     */
    @Transactional
    public void handleRefundWebhook(String providerRefundId, String eventPayload, boolean success) {
        Optional<RefundTransaction> opt = refundRepository.findByProviderRefundId(providerRefundId);
        if (opt.isEmpty()) {
            System.out.println("Refund webhook: transaction not found for providerRefundId: " + providerRefundId);
            return;
        }

        RefundTransaction rt = opt.get();

        // Prevent duplicate handling on webhook retries
        if (rt.getStatus() == RefundStatus.SUCCESS) {
            System.out.println("Refund webhook: already SUCCESS for providerRefundId: " + providerRefundId);
            return;
        }

        if (success) {
            rt.setStatus(RefundStatus.SUCCESS);
            rt.setProviderResponse(eventPayload);
            refundRepository.save(rt);

            Booking booking = rt.getBooking();

            // Restore seats only if booking wasn't already refunded
            if (booking.getStatus() != BookingStatus.REFUNDED) {
                booking.setStatus(BookingStatus.REFUNDED);
                bookingRepository.save(booking);

                Flight flight = booking.getFlight();
                if (flight != null) {
                    int restoreSeats = booking.getSeatCount() != null ? booking.getSeatCount() : 0;
                    flight.setRemainingSeats(flight.getRemainingSeats() + restoreSeats);
                    flightRepository.save(flight);

                    System.out.println("Seats restored: " + restoreSeats +
                            " -> Flight ID: " + flight.getId() + ", new remaining: " + flight.getRemainingSeats());
                } else {
                    System.out.println("Booking has no linked flight for refund seat restore: " + booking.getBookingRef());
                }
            } else {
                System.out.println("Booking already REFUNDED, skipping seat restore for: " + booking.getBookingRef());
            }

            System.out.println("Refund success processed for providerRefundId: " + providerRefundId);
        } else {
            rt.setStatus(RefundStatus.FAILED);
            rt.setProviderResponse(eventPayload);
            refundRepository.save(rt);

            System.out.println("Refund failed for providerRefundId: " + providerRefundId);
        }
    }
}