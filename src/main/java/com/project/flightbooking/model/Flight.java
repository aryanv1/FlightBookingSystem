package com.project.flightbooking.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "flights", indexes = {
        @Index(name = "idx_flight_flight_number", columnList = "flightNumber"),
        @Index(name = "idx_flight_origin_dest", columnList = "origin,destination")
})
public class Flight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30, unique = true)
    private String flightNumber; // e.g., "AI101"

    @Column(nullable = false, length = 100)
    private String airline; // e.g., "AirX"

    @Column(nullable = false, length = 10)
    private String origin; // e.g., "DEL"

    @Column(nullable = false, length = 10)
    private String destination; // e.g., "BLR"

    // store as ISO zoned date-time
    @Column(nullable = false)
    private ZonedDateTime departureTime;

    @Column(nullable = false)
    private ZonedDateTime arrivalTime;

    @Column(nullable = false)
    private Integer totalSeats;

    @Column(nullable = false)
    private Integer remainingSeats;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal baseFare;

    @Column(nullable = false, length = 20)
    private String status = "SCHEDULED"; // SCHEDULED, CANCELLED, DEPARTED

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}