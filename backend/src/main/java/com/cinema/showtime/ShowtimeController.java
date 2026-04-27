package com.cinema.showtime;

import java.util.List;
import java.util.UUID;

import com.cinema.showtime.ShowtimeDtos.SeatAvailabilityResponse;
import com.cinema.showtime.ShowtimeDtos.ShowtimeResponse;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/showtimes")
public class ShowtimeController {
    private final ShowtimeService showtimeService;

    public ShowtimeController(ShowtimeService showtimeService) {
        this.showtimeService = showtimeService;
    }

    @GetMapping("/{id}")
    ShowtimeResponse get(@PathVariable UUID id) {
        return showtimeService.get(id);
    }

    @GetMapping
    List<ShowtimeResponse> byMovie(@RequestParam UUID movieId) {
        return showtimeService.findByMovie(movieId);
    }

    @GetMapping("/{id}/seats")
    SeatAvailabilityResponse seats(@PathVariable UUID id) {
        return showtimeService.seats(id);
    }
}
