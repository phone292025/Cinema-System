package com.cinema.admin;

import java.util.List;

import com.cinema.reporting.ReportingDtos.DashboardResponse;
import com.cinema.reporting.ReportingDtos.OccupancyResponse;
import com.cinema.reporting.ReportingDtos.RevenuePoint;
import com.cinema.reporting.ReportingDtos.TopMovieResponse;
import com.cinema.reporting.ReportingService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {
    private final ReportingService reporting;

    public AdminDashboardController(ReportingService reporting) {
        this.reporting = reporting;
    }

    @GetMapping
    DashboardResponse dashboard() {
        return reporting.summary();
    }

    @GetMapping("/summary")
    DashboardResponse summary() {
        return reporting.summary();
    }

    @GetMapping("/revenue")
    List<RevenuePoint> revenue() {
        return reporting.revenue();
    }

    @GetMapping("/occupancy")
    OccupancyResponse occupancy() {
        return reporting.occupancy();
    }

    @GetMapping("/top-movies")
    List<TopMovieResponse> topMovies() {
        return reporting.topMovies();
    }
}
