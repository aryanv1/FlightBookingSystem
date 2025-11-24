package com.project.flightbooking.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI flightBookingOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Flight Booking System API")
                        .description("REST API documentation for Flight Booking System")
                        .version("1.0.0")
                        .contact(new Contact().name("Aryan Vagh").email("avagh66@gmail.com"))
                        .license(new License().name("Apache 2.0")) );
    }
}

// Link: http://localhost:8080/swagger-ui/index.html -> If running locally
// We use swagger for:
// 1. API documentation
// 2. API discoverability
// 3. Developer testing (instead of building UI)
// 4. Demonstration for resume/interviews
// 5. Postman replacement for simple flows
// Swagger only documents and calls APIs.