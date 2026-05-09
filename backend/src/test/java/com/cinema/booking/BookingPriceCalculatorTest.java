package com.cinema.booking;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import com.cinema.showtime.ShowtimeSeat;

import org.junit.jupiter.api.Test;

class BookingPriceCalculatorTest {
    @Test
    void totalsSeatPricesOnTheBackend() {
        ShowtimeSeat regular = new ShowtimeSeat();
        regular.setPrice(new BigDecimal("18.00"));
        ShowtimeSeat premium = new ShowtimeSeat();
        premium.setPrice(new BigDecimal("22.00"));

        assertThat(new BookingPriceCalculator().total(List.of(regular, premium))).isEqualByComparingTo("40.00");
    }
}
