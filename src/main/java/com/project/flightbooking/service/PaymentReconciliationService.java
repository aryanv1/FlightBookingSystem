package com.project.flightbooking.service;

import com.project.flightbooking.enums.PaymentStatus;
import com.project.flightbooking.model.Payment;
import com.project.flightbooking.repository.PaymentRepository;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import jakarta.transaction.Transactional;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/*
    This is a scheduled Cron Job which will run every 1.5hrs
    To just ensure that if any Webhook is missed or not due to server failure
 */

@Service
public class PaymentReconciliationService {

    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    @Value("${razorpay.key_id}")
    private String razorpayKeyId;

    @Value("${razorpay.key_secret}")
    private String razorpayKeySecret;

    public PaymentReconciliationService(PaymentRepository paymentRepository,
                                        PaymentService paymentService) {
        this.paymentRepository = paymentRepository;
        this.paymentService = paymentService;
    }

    /*
     * Runs every 100 minutes
     * Finds payments stuck in INITIATED state and checks Razorpay for final status.
     */
    @Scheduled(fixedDelay = 6000000) // 100 min
    @Transactional
    public void reconcilePayments() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(100);
        List<Payment> stuckPayments =
                paymentRepository.findByStatusAndCreatedAtBefore(PaymentStatus.INITIATED, cutoff);

        for (Payment p : stuckPayments) {
            try {
                reconcileSinglePayment(p);
            } catch (Exception e) {
                System.out.println("Reconciliation failed for order: " + p.getProviderOrderId());
            }
        }
    }

    private void reconcileSinglePayment(Payment p) throws RazorpayException {
        RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

        List<com.razorpay.Payment> arr = client.Orders.fetchPayments(p.getProviderOrderId());

        if (arr.isEmpty()) return;

        JSONObject paymentObj = arr.getFirst().toJson();

        String status = paymentObj.getString("status");
        String paymentId = paymentObj.getString("id");

        switch (status) {
            case "captured" -> paymentService.markPaymentSuccess(p.getProviderOrderId(), paymentId);
            case "failed" -> paymentService.markPaymentFailed(p.getProviderOrderId(), paymentId, "Reconciliation failure");
        }
    }
}