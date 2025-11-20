`🌌 Starlight Airways – Flight Booking Backend (Spring Boot)
Starlight Airways is a production-grade flight booking backend built with Spring Boot 3, designed to simulate a real airline reservation system.

It includes:
1. JWT Authentication + Refresh Token (HttpOnly cookies)
2. Role-based access (Admin/User)
3. Flight management module (Admin only)
4. Dynamic real-time pricing engine
5. Concurrency-safe seat reservation & booking workflow
6. Razorpay payment integration (order, capture, failure)
7. Refund workflow (idempotent + webhook-driven)
8. Email notifications (payment success, payment failure, refund events)
9. Clean layered architecture with DTO mapping
10. Fully modular, extensible, and scalable backend

🛠️ Tech Stack

Backend
1. Java 17
2. Spring Boot 3
3. Spring MVC
4. Spring Security + JWT
5. Spring Data JPA (Hibernate)
6. MySQL

Integrations
1. Razorpay (Orders + Refunds + Webhooks)
2. SMTP Email Server
3. Ngrok (for webhook testing)

Dev Tools
1. Maven
2. Lombok
3. IntelliJ IDEA

🏛 System Architecture

                           ┌───────────────────────────┐
                           │   Client / Frontend /     │
                           │     Postman / Mobile      │
                           └─────────────┬─────────────┘
                                         │  HTTPS (REST)
                                         ▼
                            ┌────────────────────────┐
                            │  Spring Boot Backend   │
                            │  (Starlight Airways)   │
                            └─────────────┬──────────┘
                                          │
                     ┌────────────────────┼────────────────────┐
                     ▼                    ▼                    ▼
           ┌────────────────┐   ┌──────────────────┐   ┌──────────────────┐
           │  Controller    │   │  Security Layer  │   │ Global Exception │
           │  Layer (REST)  │   │ (JWT, Filters)   │   │  Handling        │
           └────────────────┘   └──────────────────┘   └──────────────────┘
                     │
                     ▼
             ┌─────────────────────── Service Layer ────────────────────────┐
             │                                                              │
             │  ┌────────────────┐   ┌────────────────┐   ┌───────────────┐ │
             │  │ AuthService    │   │ FlightService  │   │ PricingService│ │
             │  │ (login, JWT)   │   │ (CRUD flights) │   │ (dynamic fare)│ │
             │  └────────────────┘   └────────────────┘   └───────────────┘ │
             │          │                        │                ▲         │
             │          │                        │                │         │
             │  ┌────────────────┐   ┌────────────────┐   ┌───────────────┐ │
             │  │ BookingService │   │ PaymentService │   │ RefundService │ │
             │  │ (reserve, lock │   │ (create order, │   │ (initiate,    │ │
             │  │  seats)        │   │  handle status)│   │  handle       │ │
             │  └────────────────┘   └────────────────┘   │  webhooks)    │ │
             │          │                     │           └───────────────┘ │
             │          │                     │                    │        │
             │          │                     │                    ▼        │
             │          │           ┌────────────────┐   ┌────────────────┐ │
             │          └----------▶ EmailService   │   │ JwtTokenProvider│ │
             │                      └────────────────┘   └────────────────┘ │
             └──────────────────────────────────────────────────────────────┘
                                           │
                                           ▼
                          ┌──────────────────────────────────┐
                          │      Repository Layer (JPA)      │
                          │  UserRepo, FlightRepo, Booking   │
                          │  PaymentRepo, RefundRepo, etc.   │
                          └─────────────────┬────────────────┘
                                            │
                                            ▼
                                   ┌────────────────┐
                                   │   MySQL DB     │
                                   │ (users,        │
                                   │  flights,      │
                                   │  bookings,     │
                                   │  payments,     │
                                   │  refunds)      │
                                   └────────────────┘

External Integrations:
1. Razorpay Payments API
2. Razorpay Refunds Webhooks
3. SMTP Email Server
4. JWT Security Filters

🚀 Features

1. Authentication & Authorization
    1. JWT Access Token
    2. HttpOnly Secure Refresh Token
    3. Role-based access (ADMIN, USER)
    4. Password hashing with BCrypt

2. Flight Management (Admin Only)
    1. Add / update / delete flights
    2. Search flights with filters
    3. Real-time seat availability

3. Dynamic Pricing Engine
    1. Price increases as seats fill
    2. Time-to-departure surge
    3. Demand-based adjustments
    4. Final fare = baseFare × multiplier

4. Booking Engine
    1. Pessimistic locking → prevents overbooking
    2. Seat reservation (PENDING)
    3. Automatic seat release on payment failure
    4. Booking confirmations and fetch APIs

5. Payment Module (Razorpay)
    1. Razorpay Order creation
    2. Payment success/failure via webhooks
    3. Idempotent payment handling
    4. Secure business-state updates

6. Refund Module
    1. Policy-based refund % (time-based tiers)
    2. Razorpay refund initiation
    3. Webhook-based refund settlement
    4. Idempotency + seat restoration

7. Email Notifications
    1. Payment Successful
    2. Payment Failed
    3. Refund Initiated
    4. Refund Successful

📡 API Endpoints

AUTH

    POST /api/auth/register
    POST /api/auth/login
    POST /api/auth/refresh

ADMIN FLIGHTS

    POST   /api/admin/flights
    PUT    /api/admin/flights/{id}
    DELETE /api/admin/flights/{id}

FLIGHTS (Public)

    GET /api/flights
    GET /api/flights/{id}

BOOKINGS

    POST /api/bookings
    GET  /api/bookings/{bookingRef}

PAYMENTS

    POST /api/payments/create/{bookingRef}
    POST /api/payments/webhook

REFUNDS

    POST /api/refunds/initiate/{bookingRef}
    POST /api/refunds/webhook

🧪 Testing Flow

Booking + Payment
1.	Create booking → returns PENDING
2.	Create Razorpay Order
3.	Pay via Razorpay Checkout
4.	Razorpay → your webhook (ngrok URL)
5.	Booking becomes CONFIRMED

Refund
1.	Call /api/refunds/initiate/{bookingRef}
2.	Razorpay → refund webhook
3.	Booking becomes REFUNDED
4.	Seats restored`