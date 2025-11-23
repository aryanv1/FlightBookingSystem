package com.project.flightbooking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FlightBookingSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(FlightBookingSystemApplication.class, args);
    }
}
