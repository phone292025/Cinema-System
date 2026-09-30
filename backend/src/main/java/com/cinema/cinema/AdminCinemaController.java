package com.cinema.cinema;

import java.util.List;
import java.util.UUID;

import com.cinema.cinema.CinemaDtos.BulkSeatLayoutRequest;
import com.cinema.cinema.CinemaDtos.CinemaRequest;
import com.cinema.cinema.CinemaDtos.CinemaResponse;
import com.cinema.cinema.CinemaDtos.HallRequest;
import com.cinema.cinema.CinemaDtos.HallResponse;
import com.cinema.cinema.CinemaDtos.SeatRequest;
import com.cinema.cinema.CinemaDtos.SeatResponse;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCinemaController {
    private final CinemaService cinemaService;

    public AdminCinemaController(CinemaService cinemaService) {
        this.cinemaService = cinemaService;
    }

    @GetMapping("/cinemas")
    List<CinemaResponse> cinemas() {
        return cinemaService.listCinemas();
    }

    @PostMapping("/cinemas")
    @ResponseStatus(HttpStatus.CREATED)
    CinemaResponse createCinema(@Valid @RequestBody CinemaRequest request) {
        return cinemaService.createCinema(request);
    }

    @PutMapping("/cinemas/{id}")
    CinemaResponse updateCinema(@PathVariable UUID id, @Valid @RequestBody CinemaRequest request) {
        return cinemaService.updateCinema(id, request);
    }

    @DeleteMapping("/cinemas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteCinema(@PathVariable UUID id) {
        cinemaService.deleteCinema(id);
    }

    @GetMapping("/cinemas/{cinemaId}/halls")
    List<HallResponse> halls(@PathVariable UUID cinemaId) {
        return cinemaService.listHalls(cinemaId);
    }

    @PostMapping("/cinemas/{cinemaId}/halls")
    @ResponseStatus(HttpStatus.CREATED)
    HallResponse createHall(@PathVariable UUID cinemaId, @Valid @RequestBody HallRequest request) {
        return cinemaService.createHall(cinemaId, request);
    }

    @PostMapping("/halls/{hallId}/seats")
    @ResponseStatus(HttpStatus.CREATED)
    SeatResponse createSeat(@PathVariable UUID hallId, @Valid @RequestBody SeatRequest request) {
        return cinemaService.createSeat(hallId, request);
    }

    @PostMapping("/halls/{hallId}/seat-layout")
    List<SeatResponse> createLayout(@PathVariable UUID hallId, @Valid @RequestBody BulkSeatLayoutRequest request) {
        return cinemaService.createLayout(hallId, request);
    }

    @GetMapping("/halls/{hallId}/seats")
    List<SeatResponse> seats(@PathVariable UUID hallId) {
        return cinemaService.listSeats(hallId);
    }
}
