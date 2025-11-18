package com.project.flightbooking.service;

import com.project.flightbooking.model.Flight;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.ZonedDateTime;

/**
 * PricingService
 * --------------
 * Calculates dynamic fare for a flight based on:
 *  1. Seat availability (demand factor)
 *  2. Time left to departure (time factor)
 *  3. Surge factor for very low seats
 *
 * finalFare = baseFare * demandFactor * timeFactor * surgeFactor
 */
@Service
public class PricingService {

    // -----------------------------
    // MAIN ENTRY POINT
    // -----------------------------
    public BigDecimal calculateDynamicFare(Flight flight) {

        BigDecimal baseFare = flight.getBaseFare(); // Stored in DB

        BigDecimal demandFactor = calculateDemandFactor(flight);
        BigDecimal timeFactor = calculateTimeFactor(flight);
        BigDecimal surgeFactor = calculateSurgeFactor(flight);

        BigDecimal finalFare =
                baseFare
                        .multiply(demandFactor)
                        .multiply(timeFactor)
                        .multiply(surgeFactor)
                        .setScale(2, RoundingMode.HALF_UP);

        return finalFare;
    }

    // -----------------------------
    // FACTOR 1 – DEMAND / SEAT AVAILABILITY
    // -----------------------------
    private BigDecimal calculateDemandFactor(Flight flight) {
        int total = flight.getTotalSeats();
        int remaining = flight.getRemainingSeats();

        double percent = (remaining * 100.0) / total;

        if (percent > 70) return BigDecimal.valueOf(1.00);
        if (percent > 40) return BigDecimal.valueOf(1.10);
        if (percent > 20) return BigDecimal.valueOf(1.25);
        return BigDecimal.valueOf(1.50);
    }

    // -----------------------------
    // FACTOR 2 – TIME TO DEPARTURE
    // -----------------------------
    private BigDecimal calculateTimeFactor(Flight flight) {
        ZonedDateTime departure = flight.getDepartureTime();
        ZonedDateTime now = ZonedDateTime.now(departure.getZone());

        long hours = Duration.between(now, departure).toHours();

        if (hours > 72) return BigDecimal.valueOf(1.00);
        if (hours > 24) return BigDecimal.valueOf(1.15);
        if (hours > 6)  return BigDecimal.valueOf(1.30);
        return BigDecimal.valueOf(1.50);
    }

    // -----------------------------
    // FACTOR 3 – SURGE (VERY LOW SEATS)
    // -----------------------------
    private BigDecimal calculateSurgeFactor(Flight flight) {
        if (flight.getRemainingSeats() < 10)
            return BigDecimal.valueOf(1.25);
        return BigDecimal.valueOf(1.00);
    }
}