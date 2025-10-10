package com.project.flightbooking.repository;

import com.project.flightbooking.model.Flight;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FlightRepository extends JpaRepository<Flight, Long> {
    List<Flight> findBySourceAndDestination(String source, String destination);
}
