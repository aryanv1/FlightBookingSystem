package com.project.flightbooking.controller;

import com.project.flightbooking.service.PaymentService;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/create/{bookingRef}")
    public ResponseEntity<String> createPaymentOrder(@PathVariable String bookingRef) {
        try {
            JSONObject response = paymentService.createRazorpayOrder(bookingRef);
            return ResponseEntity.ok(response.toString());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error creating order: " + e.getMessage());
        }
    }
}