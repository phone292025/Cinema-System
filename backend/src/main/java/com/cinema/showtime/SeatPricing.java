package com.cinema.showtime;

import java.math.BigDecimal;

import com.cinema.seat.SeatType;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SeatPricing {
    private final BigDecimal premiumSurcharge;

    public SeatPricing(@Value("${app.pricing.premium-surcharge:4.00}") BigDecimal premiumSurcharge) {
        if (premiumSurcharge.signum() < 0) {
            throw new IllegalStateException("app.pricing.premium-surcharge must not be negative.");
        }
        this.premiumSurcharge = premiumSurcharge;
    }

    public BigDecimal priceFor(BigDecimal basePrice, SeatType seatType) {
        return seatType == SeatType.PREMIUM ? basePrice.add(premiumSurcharge) : basePrice;
    }
}
