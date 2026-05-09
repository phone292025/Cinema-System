package com.cinema.showtime;

import java.util.List;
import java.util.UUID;

import com.cinema.showtime.ShowtimeDtos.ShowtimeRequest;
import com.cinema.showtime.ShowtimeDtos.ShowtimeResponse;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/showtimes")
@PreAuthorize("hasRole('ADMIN')")
public class AdminShowtimeController {
    private final ShowtimeService showtimeService;

    public AdminShowtimeController(ShowtimeService showtimeService) {
        this.showtimeService = showtimeService;
    }

    @GetMapping
    List<ShowtimeResponse> list() {
        return showtimeService.findAllForAdmin();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ShowtimeResponse create(@Valid @RequestBody ShowtimeRequest request) {
        return showtimeService.create(request);
    }

    @GetMapping("/{id}")
    ShowtimeResponse get(@PathVariable UUID id) {
        return showtimeService.get(id);
    }
}
