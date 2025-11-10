package com.project.flightbooking.service;

import com.project.flightbooking.dto.FlightRequest;
import com.project.flightbooking.model.Flight;
import com.project.flightbooking.repository.FlightRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Optional;

@Service
public class FlightService {

    private final FlightRepository flightRepository;

    public FlightService(FlightRepository flightRepository) {
        this.flightRepository = flightRepository;
    }

    public Flight createFlight(FlightRequest req) {
        Flight f = new Flight();
        f.setFlightNumber(req.getFlightNumber());
        f.setAirline(req.getAirline());
        f.setOrigin(req.getOrigin());
        f.setDestination(req.getDestination());

        try {
            f.setDepartureTime(ZonedDateTime.parse(req.getDepartureTime()));
            f.setArrivalTime(ZonedDateTime.parse(req.getArrivalTime()));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid ISO date time format for departure/arrival");
        }

        f.setTotalSeats(req.getTotalSeats());
        f.setRemainingSeats(req.getTotalSeats()); // initialize remaining seats equal to total
        f.setBaseFare(req.getBaseFare() == null ? BigDecimal.ZERO : req.getBaseFare());
        f.setStatus("SCHEDULED");
        return flightRepository.save(f);
    }

    public Optional<Flight> findById(Long id) {
        return flightRepository.findById(id);
    }

    public Page<Flight> search(String origin, String destination, ZonedDateTime from, ZonedDateTime to, Pageable pageable) {
        return flightRepository.findByOriginAndDestinationAndDepartureTimeBetween(origin, destination, from, to, pageable);
    }
}