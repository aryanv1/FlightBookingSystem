package com.project.flightbooking.controller;

import com.project.flightbooking.service.PaymentService;
import com.project.flightbooking.service.RefundService;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * RazorpayWebhookController
 * --------------------------
 * Handles webhooks (server-to-server callbacks) sent by Razorpay
 * after a payment succeeds or fails.
 *
 *  Flow:
 *  - Razorpay sends a POST to /api/payments/webhook
 *  - Verify signature using HMAC SHA256 (HEX format)
 *  - Parse event type and call appropriate PaymentService method
 */
@RestController
@RequestMapping("/api/payments")
public class RazorpayWebhookController {

    // This secret is configured in Razorpay Dashboard → Webhooks → Secret
    @Value("${razorpay.webhook_secret}")
    private String webhookSecret;

    private final PaymentService paymentService;
    private final RefundService refundService;

    public RazorpayWebhookController(PaymentService paymentService, RefundService refundService) {
        this.paymentService = paymentService;
        this.refundService = refundService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestHeader("X-Razorpay-Signature") String signature, // Razorpay's cryptographic signature
            @RequestBody String payload) { // Raw JSON body (the entire event)

        try {
            // 1. SECURITY: Verify authenticity of the payload
            if (!verifySignature(payload, signature, webhookSecret)) {
                System.out.println("Invalid Razorpay signature. Possible spoofed request!");
                return ResponseEntity.status(400).body("Invalid signature");
            }

            // 2. Parse payload into JSON for further analysis
            JSONObject json = new JSONObject(payload);
            String event = json.optString("event", "unknown");

            System.out.println("Razorpay Webhook Event: " + event);

            // 3. Handle events based on type
            switch (event) {
                case "payment.captured" -> {
                    JSONObject paymentEntity = json.getJSONObject("payload")
                            .getJSONObject("payment").getJSONObject("entity");
                    String orderId = paymentEntity.getString("order_id");
                    String paymentId = paymentEntity.getString("id");

                    paymentService.markPaymentSuccess(orderId, paymentId);
                }

                case "payment.failed" -> {
                    JSONObject paymentEntity = json.getJSONObject("payload")
                            .getJSONObject("payment").getJSONObject("entity");
                    String orderId = paymentEntity.getString("order_id");
                    String paymentId = paymentEntity.getString("id");
                    String reason = paymentEntity.optString("error_description", "Unknown error");

                    paymentService.markPaymentFailed(orderId, paymentId, reason);
                }

                case "refund.processed", "refund.updated" -> {
                    JSONObject refundEntity = json.getJSONObject("payload")
                            .getJSONObject("refund").getJSONObject("entity");
                    String providerRefundId = refundEntity.getString("id");
                    // Some payloads include status; determine success by status or event type
                    String status = refundEntity.optString("status", "processed");
                    boolean success = "processed".equalsIgnoreCase(status) || "success".equalsIgnoreCase(status);
                    refundService.handleRefundWebhook(providerRefundId, payload, success);
                }

                default -> System.out.println("Unhandled event type: " + event);
            }

            return ResponseEntity.ok("Webhook processed successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error processing webhook: " + e.getMessage());
        }
    }

    /**
     * verifySignature()
     * -----------------
     * Ensures that the webhook was genuinely sent by Razorpay
     * and not forged by anyone else.
     * Razorpay computes an HMAC-SHA256 hash of the request body using your webhook secret.
     * You must recompute it here and compare it to their header.
     */
    private boolean verifySignature(String payload, String expectedSignature, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(), "HmacSHA256"));
        byte[] hash = mac.doFinal(payload.getBytes());
        String generatedSignature = bytesToHex(hash); // Razorpay uses HEX encoding, not Base64
        return generatedSignature.equals(expectedSignature);
    }

    /**
     * Helper: Converts bytes to lowercase hexadecimal string.
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}