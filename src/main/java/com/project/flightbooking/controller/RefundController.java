package com.project.flightbooking.controller;

import com.project.flightbooking.model.RefundTransaction;
import com.project.flightbooking.service.RefundService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/refunds")
public class RefundController {

    private final RefundService refundService;
    public RefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    /**
     * Initiate a refund for a bookingRef.
     * This endpoint is idempotent: calling multiple times returns the existing refund in progress.
     */
    @PostMapping("/initiate/{bookingRef}")
    public ResponseEntity<?> initiateRefund(@PathVariable String bookingRef) {
        try {
            RefundTransaction rt = refundService.initiateRefund(bookingRef);
            return ResponseEntity.ok(rt);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}