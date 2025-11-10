package com.project.flightbooking.repository;

import com.project.flightbooking.model.Flight;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public interface FlightRepository extends JpaRepository<Flight, Long> {

    // findByIdForUpdate acquires a DB lock on the flight row until the transaction ends,
    // preventing race conditions across concurrent reservations.
    // Use a dedicated locked read to reserve seats atomically
    // Tells JPA to fetch the entity with a pessimistic write lock.
    // Effect: JPA will ask the database for an exclusive lock on the selected row(s)
    // until the current transaction commits or rolls back.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    // JPQL(Jakarta Persistence Query Language) Query
    @Query("select f from Flight f where f.id = :id")
    Optional<Flight> findByIdForUpdate(@Param("id") Long id);
    // We must run this inside the @Transactional block
    // Should use Pessimistic Write to prevent concurrent updates to same row
    // It can lead to deadlock if lock is held for longer time
    // @Transactional = ensures “everything I do inside here succeeds or fails together.”
    // @Lock = ensures “no one else messes with the same data while I’m doing it.”
    // A lock only exists as long as the database transaction that created it remains open.
    // We can't hold a lock outside a transaction
    // If we do not use @Transactional, then JPA will create a tiny transactional itself
    // As soon as the query finishes, this tiny transaction will end and lock will be release
    // So operations after it like repo.save() will be out of lock -> Which will creates issues.
    // So a lock without transaction is meaningless -> May lead to double booking

    Page<Flight> findByOriginAndDestinationAndDepartureTimeBetween(
            String origin, String destination, ZonedDateTime from, ZonedDateTime to, Pageable pageable);

    List<Flight> findByOriginAndDestination(String origin, String destination);
}