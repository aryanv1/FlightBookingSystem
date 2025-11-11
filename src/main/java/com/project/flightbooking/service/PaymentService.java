package com.project.flightbooking.service;

import com.project.flightbooking.model.Booking;
import com.project.flightbooking.model.Payment;
import com.project.flightbooking.model.Flight;
import com.project.flightbooking.enums.PaymentStatus;
import com.project.flightbooking.enums.BookingStatus;
import com.project.flightbooking.repository.BookingRepository;
import com.project.flightbooking.repository.PaymentRepository;
import com.project.flightbooking.repository.FlightRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import jakarta.transaction.Transactional;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * PaymentService handles all interactions related to payment creation,
 * success/failure updates, and syncing with Razorpay orders.
 *
 * Responsibilities:
 *  - Create Razorpay order for booking.
 *  - Update payment & booking records upon success/failure.
 *  - Safely release seats if payment fails.
 */
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final FlightRepository flightRepository; // Added to persist seat restoration on payment failure

    // These @Value annotations pull your secret keys from application.properties
    @Value("${razorpay.key_id}")
    private String razorpayKeyId;

    @Value("${razorpay.key_secret}")
    private String razorpayKeySecret;

    public PaymentService(PaymentRepository paymentRepository,
                          BookingRepository bookingRepository,
                          FlightRepository flightRepository) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.flightRepository = flightRepository;
    }

    /**
     * Method 1: Creates the order on Razorpay's side.
     * Called when user confirms booking and proceeds to payment.
     */
    @Transactional
    public JSONObject createRazorpayOrder(String bookingRef) throws Exception {
        // 1. Find the booking in your local database
        Booking booking = bookingRepository.findByBookingRef(bookingRef)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingRef));

        // 2. Create the official Razorpay client object with your keys
        RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

        // 3. Prepare the order details
        BigDecimal amount = booking.getTotalFare();
        // Razorpay (and most gateways) requires the amount in the smallest currency unit
        // Example: 100.50 Rupees becomes 10050 paise.
        int amountInPaise = amount.multiply(BigDecimal.valueOf(100)).intValueExact();

        // 4. Build the JSON "order request" object
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amountInPaise);
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", bookingRef); // Your internal booking ref as the receipt ID
        orderRequest.put("payment_capture", 1); // Auto-capture the payment

        // 5. Actually create the order by calling Razorpay's API
        Order order = client.Orders.create(orderRequest);

        // 6. CRITICAL: Save a record of this payment attempt in YOUR database
        Payment p = new Payment();
        p.setBooking(booking);
        p.setProviderOrderId(order.get("id")); // Save Razorpay's Order ID
        p.setAmount(amount);
        p.setStatus(PaymentStatus.INITIATED); // Mark as INITIATED
        p.setCurrency("INR");
        p.setProviderResponse(order.toString()); // Save the full response for debugging
        paymentRepository.save(p);

        System.out.println("Created Razorpay Order: " + order.get("id") + " for Booking: " + bookingRef);

        // 7. Build the JSON response to send back to YOUR frontend (or Postman)
        JSONObject response = new JSONObject();
        response.put("razorpayOrderId", (Object) order.get("id")); // The ID for the checkout
        response.put("amount", amountInPaise);
        response.put("currency", "INR");
        response.put("key", razorpayKeyId); // The frontend needs this key to open the modal
        response.put("bookingRef", bookingRef);

        return response;
    }

    /**
     * Method 2: Marks payment as SUCCESS after Razorpay webhook notifies success.
     * Updates booking status -> CONFIRMED.
     */
    @Transactional
    public void markPaymentSuccess(String orderId, String paymentId) {
        System.out.println("Payment Success Webhook received for order: " + orderId);

        // 1. Retrieve payment record
        Payment payment = paymentRepository.findByProviderOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        // 2. Update payment details
        payment.setProviderPaymentId(paymentId);
        payment.setStatus(PaymentStatus.SUCCESS);
        paymentRepository.save(payment);

        // 3. Update corresponding booking
        Booking booking = payment.getBooking();
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentStatus(PaymentStatus.SUCCESS);
        bookingRepository.save(booking);

        System.out.println("Booking " + booking.getBookingRef() + " confirmed successfully.");
    }

    /**
     * Method 3: Marks payment as FAILED after webhook or user failure.
     * Rolls back booking and restores seats to flight inventory.
     */
    @Transactional
    public void markPaymentFailed(String orderId, String paymentId, String reason) {
        System.out.println("Payment Failed Webhook received for order: " + orderId + " due to: " + reason);

        // 1. Fetch payment record by orderId
        Payment payment = paymentRepository.findByProviderOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        // 2. Update payment failure details
        payment.setProviderPaymentId(paymentId);
        payment.setStatus(PaymentStatus.FAILED);
        payment.setProviderResponse(reason);
        paymentRepository.save(payment);

        // 3. Update booking status -> CANCELLED
        Booking booking = payment.getBooking();
        booking.setPaymentStatus(PaymentStatus.FAILED);
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        // 4. Restore flight seats if payment fails
        Flight flight = booking.getFlight();
        if (flight != null) {
            Integer seatsToRestore = booking.getSeatCount();
            if (flight.getRemainingSeats() == null) flight.setRemainingSeats(0);
            flight.setRemainingSeats(flight.getRemainingSeats() + seatsToRestore);
            flightRepository.save(flight); // Persist seat restoration

            System.out.println("Seats restored: " + seatsToRestore +
                    " back to Flight ID: " + flight.getId() +
                    ". Total Available: " + flight.getRemainingSeats());
        } else {
            System.out.println("Warning: No flight found for failed booking!");
        }

        System.out.println("Booking " + booking.getBookingRef() + " cancelled due to payment failure.");
    }
}