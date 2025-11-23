# 🌌 Starlight Airways

Flight Booking Backend (Spring Boot)
Starlight Airways is a production-grade flight booking backend built with Spring Boot 3, designed to simulate a real airline reservation system.

It includes:
* JWT Authentication + Refresh Token (HttpOnly cookies)
* Role-based access (Admin/User)
* Flight management module (Admin only)
* Dynamic real-time pricing engine
* Concurrency-safe seat reservation & booking workflow
* Razorpay payment integration (order, capture, failure)
* Refund workflow (idempotent + webhook-driven)
* Reconciliation API for long downtime of server
* Email notifications (payment success, payment failure, refund events)
* Clean layered architecture with DTO mapping
* Fully modular, extensible, and scalable backend

## **🛠️ Tech Stack**

### Backend
* Java 17
* Spring Boot 3
* Spring MVC
* Spring Security + JWT
* Spring Data JPA (Hibernate)
* MySQL

### Integrations
* Razorpay (Orders + Refunds + Webhooks)
* SMTP Email Server
* Ngrok (for webhook testing)

### Dev Tools
* Maven
* Lombok
* IntelliJ IDEA

## **🏛 System Architecture**

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
             │          └----------▶│  EmailService  │   │JwtTokenProvider│ │
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

### External Integrations:
  * Razorpay Payments API
  * Razorpay Refunds Webhooks
  * SMTP Email Server
  * JWT Security Filters

## **🚀 Features**

### 1. Authentication & Authorization 

   * JWT Access Token
   * HttpOnly Secure Refresh Token
   * Role-based access (ADMIN, USER)
   * Password hashing with BCrypt

### 2. Flight Management (Admin Only)

   * Add / update / delete flights
   * Search flights with filters
   * Real-time seat availability

### 3. Dynamic Pricing Engine

   * Price increases as seats fill
   * Time-to-departure surge
   * Demand-based adjustments
   * Final fare = baseFare × multiplier

### 4. Booking Engine

   * Pessimistic locking → prevents overbooking
   * Seat reservation
   * Automatic seat release on payment failure
   * Booking confirmations and fetch APIs

### 5. Payment Module (Razorpay)

   * Razorpay Order creation
   * Payment success/failure via webhooks
   * Idempotent payment handling
   * Secure business-state updates

### 6. Refund Module

   * Policy-based refund % (time-based tiers)
   * Razorpay refund initiation
   * Webhook-based refund settlement
   * Idempotency + seat restoration

### 7. Email Notifications

   * Payment Successful
   * Payment Failed
   * Refund Initiated
   * Refund Successful

## **📡 API Endpoints**

#### AUTH

    POST /api/auth/register
    POST /api/auth/login
    POST /api/auth/refresh

#### ADMIN FLIGHTS

    POST   /api/admin/flights
    PUT    /api/admin/flights/{id}
    DELETE /api/admin/flights/{id}

#### FLIGHTS (Public)

    GET /api/flights
    GET /api/flights/{id}

#### BOOKINGS

    POST /api/bookings
    GET  /api/bookings/{bookingRef}

#### PAYMENTS

    POST /api/payments/create/{bookingRef}
    POST /api/payments/webhook

#### REFUNDS

    POST /api/refunds/initiate/{bookingRef}
    POST /api/refunds/webhook

## **🧪 Testing Flow**

### Booking + Payment
1.	Create booking → returns PENDING
2.	Create Razorpay Order
3.	Pay via Razorpay Checkout
4.	Razorpay → your webhook (ngrok URL)
5.	Booking becomes CONFIRMED

### Refund
1.	Call /api/refunds/initiate/{bookingRef}
2.	Razorpay → refund webhook
3.	Booking becomes REFUNDED
4.	Seats restored