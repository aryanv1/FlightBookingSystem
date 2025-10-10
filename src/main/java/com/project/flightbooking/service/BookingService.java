package com.project.flightbooking.service;

import com.project.flightbooking.model.Booking;
import com.project.flightbooking.repository.BookingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;

    public BookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Transactional
    public Booking createBooking(Booking booking) {
        return bookingRepository.save(booking);
    }
}
