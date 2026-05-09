package com.cinema.cinema;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.cinema.cinema.CinemaDtos.BulkSeatLayoutRequest;
import com.cinema.cinema.CinemaDtos.CinemaRequest;
import com.cinema.cinema.CinemaDtos.CinemaResponse;
import com.cinema.cinema.CinemaDtos.HallRequest;
import com.cinema.cinema.CinemaDtos.HallResponse;
import com.cinema.cinema.CinemaDtos.SeatRequest;
import com.cinema.cinema.CinemaDtos.SeatResponse;
import com.cinema.common.ApiException;
import com.cinema.hall.Hall;
import com.cinema.hall.HallRepository;
import com.cinema.seat.Seat;
import com.cinema.seat.SeatRepository;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
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
    private final CinemaRepository cinemas;
    private final HallRepository halls;
    private final SeatRepository seats;

    public AdminCinemaController(CinemaRepository cinemas, HallRepository halls, SeatRepository seats) {
        this.cinemas = cinemas;
        this.halls = halls;
        this.seats = seats;
    }

    @GetMapping("/cinemas")
    List<CinemaResponse> cinemas() {
        return cinemas.findAll().stream().map(CinemaResponse::from).toList();
    }

    @PostMapping("/cinemas")
    @ResponseStatus(HttpStatus.CREATED)
    CinemaResponse createCinema(@Valid @RequestBody CinemaRequest request) {
        return CinemaResponse.from(cinemas.save(apply(new Cinema(), request)));
    }

    @PutMapping("/cinemas/{id}")
    CinemaResponse updateCinema(@PathVariable UUID id, @Valid @RequestBody CinemaRequest request) {
        Cinema cinema = cinemas.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Cinema not found."));
        return CinemaResponse.from(cinemas.save(apply(cinema, request)));
    }

    @DeleteMapping("/cinemas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteCinema(@PathVariable UUID id) {
        cinemas.deleteById(id);
    }

    @GetMapping("/cinemas/{cinemaId}/halls")
    List<HallResponse> halls(@PathVariable UUID cinemaId) {
        return halls.findByCinemaId(cinemaId).stream().map(HallResponse::from).toList();
    }

    @PostMapping("/cinemas/{cinemaId}/halls")
    @ResponseStatus(HttpStatus.CREATED)
    HallResponse createHall(@PathVariable UUID cinemaId, @Valid @RequestBody HallRequest request) {
        Cinema cinema = cinemas.findById(cinemaId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Cinema not found."));
        Hall hall = new Hall();
        hall.setCinema(cinema);
        hall.setName(request.name());
        hall.setType(request.type());
        hall.setTotalRows(request.totalRows());
        hall.setTotalColumns(request.totalColumns());
        return HallResponse.from(halls.save(hall));
    }

    @PostMapping("/halls/{hallId}/seats")
    @ResponseStatus(HttpStatus.CREATED)
    SeatResponse createSeat(@PathVariable UUID hallId, @Valid @RequestBody SeatRequest request) {
        Hall hall = halls.findById(hallId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Hall not found."));
        Seat seat = new Seat();
        seat.setHall(hall);
        seat.setRowLabel(request.rowLabel().toUpperCase());
        seat.setSeatNumber(request.seatNumber());
        seat.setSeatType(request.seatType());
        return SeatResponse.from(seats.save(seat));
    }

    @PostMapping("/halls/{hallId}/seat-layout")
    @Transactional
    List<SeatResponse> createLayout(@PathVariable UUID hallId, @Valid @RequestBody BulkSeatLayoutRequest request) {
        Hall hall = halls.findById(hallId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Hall not found."));
        List<Seat> created = new ArrayList<>();
        for (int row = 0; row < request.rows(); row++) {
            String rowLabel = String.valueOf((char) ('A' + row));
            for (int number = 1; number <= request.columns(); number++) {
                Seat seat = new Seat();
                seat.setHall(hall);
                seat.setRowLabel(rowLabel);
                seat.setSeatNumber(number);
                seat.setSeatType(request.defaultSeatType());
                created.add(seat);
            }
        }
        return seats.saveAll(created).stream().map(SeatResponse::from).toList();
    }

    @GetMapping("/halls/{hallId}/seats")
    List<SeatResponse> seats(@PathVariable UUID hallId) {
        return seats.findByHallIdOrderByRowLabelAscSeatNumberAsc(hallId).stream().map(SeatResponse::from).toList();
    }

    private Cinema apply(Cinema cinema, CinemaRequest request) {
        cinema.setName(request.name());
        cinema.setLocation(request.location());
        cinema.setAddress(request.address());
        cinema.setCity(request.city());
        return cinema;
    }
}
