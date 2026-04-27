package com.cinema.cinema;

import java.util.List;
import java.util.UUID;

import com.cinema.cinema.CinemaDtos.CinemaResponse;
import com.cinema.showtime.ShowtimeDtos.ShowtimeResponse;
import com.cinema.showtime.ShowtimeService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cinemas")
public class CinemaController {
    private final CinemaRepository cinemas;
    private final ShowtimeService showtimeService;

    public CinemaController(CinemaRepository cinemas, ShowtimeService showtimeService) {
        this.cinemas = cinemas;
        this.showtimeService = showtimeService;
    }

    @GetMapping
    List<CinemaResponse> list() {
        return cinemas.findAll().stream().map(CinemaResponse::from).toList();
    }

    @GetMapping("/{id}/showtimes")
    List<ShowtimeResponse> showtimes(@PathVariable UUID id) {
        return showtimeService.findByCinema(id);
    }
}
