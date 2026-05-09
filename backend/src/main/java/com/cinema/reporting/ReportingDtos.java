package com.cinema.reporting;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class ReportingDtos {
    private ReportingDtos() {
    }

    public record DashboardResponse(long movies, long showtimes, long bookings, long paidBookings, long failedPayments,
            BigDecimal revenue, double occupancyRate) {
    }

    public record RevenuePoint(LocalDate date, BigDecimal revenue) {
    }

    public record OccupancyResponse(long totalSeats, long bookedSeats, long lockedSeats, double occupancyRate) {
    }

    public record TopMovieResponse(String movieTitle, long ticketsSold) {
    }
}
