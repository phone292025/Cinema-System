package com.cinema.booking;

import java.math.BigDecimal;
import java.util.List;

import com.cinema.showtime.ShowtimeSeat;

public class BookingPriceCalculator {
    public BigDecimal total(List<ShowtimeSeat> seats) {
        return seats.stream()
                .map(ShowtimeSeat::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
