package com.cinema.booking;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BookingDomainConfig {
    @Bean
    BookingStateMachine bookingStateMachine() {
        return new BookingStateMachine();
    }

    @Bean
    BookingPriceCalculator bookingPriceCalculator() {
        return new BookingPriceCalculator();
    }
}
