package com.cinema.cinema;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.cinema.audit.AuditLogService;
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
import com.cinema.seat.SeatType;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CinemaService {
    private final CinemaRepository cinemas;
    private final HallRepository halls;
    private final SeatRepository seats;
    private final AuditLogService auditLogs;

    public CinemaService(CinemaRepository cinemas, HallRepository halls, SeatRepository seats, AuditLogService auditLogs) {
        this.cinemas = cinemas;
        this.halls = halls;
        this.seats = seats;
        this.auditLogs = auditLogs;
    }

    @Transactional(readOnly = true)
    public List<CinemaResponse> listCinemas() {
        return cinemas.findAll().stream().map(CinemaResponse::from).toList();
    }

    @Transactional
    public CinemaResponse createCinema(CinemaRequest request) {
        Cinema cinema = cinemas.save(apply(new Cinema(), request));
        auditLogs.recordAdmin("CINEMA_CREATED", "Cinema", cinema.getId().toString(), cinema.getName());
        return CinemaResponse.from(cinema);
    }

    @Transactional
    public CinemaResponse updateCinema(UUID id, CinemaRequest request) {
        Cinema cinema = cinemas.save(apply(findCinema(id), request));
        auditLogs.recordAdmin("CINEMA_UPDATED", "Cinema", cinema.getId().toString(), cinema.getName());
        return CinemaResponse.from(cinema);
    }

    @Transactional
    public void deleteCinema(UUID id) {
        Cinema cinema = findCinema(id);
        if (cinemas.hasShowtimes(id)) {
            throw cinemaInUse();
        }
        try {
            cinemas.delete(cinema);
            cinemas.flush();
            auditLogs.recordAdmin("CINEMA_DELETED", "Cinema", id.toString(), cinema.getName());
        } catch (DataIntegrityViolationException ex) {
            throw cinemaInUse();
        }
    }

    @Transactional(readOnly = true)
    public List<HallResponse> listHalls(UUID cinemaId) {
        return halls.findByCinemaId(cinemaId).stream().map(HallResponse::from).toList();
    }

    @Transactional
    public HallResponse createHall(UUID cinemaId, HallRequest request) {
        Hall hall = new Hall();
        hall.setCinema(findCinema(cinemaId));
        hall.setName(request.name());
        hall.setType(request.type());
        hall.setTotalRows(request.totalRows());
        hall.setTotalColumns(request.totalColumns());
        Hall saved = halls.save(hall);
        if (request.defaultSeatType() != null) {
            seats.saveAll(layout(saved, request.totalRows(), request.totalColumns(), request.defaultSeatType()));
        }
        auditLogs.recordAdmin("HALL_CREATED", "Hall", saved.getId().toString(),
                saved.getName() + " " + request.totalRows() + "x" + request.totalColumns());
        return HallResponse.from(saved);
    }

    @Transactional
    public SeatResponse createSeat(UUID hallId, SeatRequest request) {
        Seat seat = new Seat();
        seat.setHall(findHall(hallId));
        seat.setRowLabel(request.rowLabel().toUpperCase(Locale.ROOT));
        seat.setSeatNumber(request.seatNumber());
        seat.setSeatType(request.seatType());
        try {
            return SeatResponse.from(seats.saveAndFlush(seat));
        } catch (DataIntegrityViolationException ex) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Seat " + seat.getRowLabel() + seat.getSeatNumber() + " already exists in this hall.");
        }
    }

    @Transactional
    public List<SeatResponse> createLayout(UUID hallId, BulkSeatLayoutRequest request) {
        Hall hall = findHall(hallId);
        try {
            return seats.saveAllAndFlush(layout(hall, request.rows(), request.columns(), request.defaultSeatType())).stream()
                    .map(SeatResponse::from)
                    .toList();
        } catch (DataIntegrityViolationException ex) {
            throw new ApiException(HttpStatus.CONFLICT, "This hall already has seats that overlap the requested layout.");
        }
    }

    @Transactional(readOnly = true)
    public List<SeatResponse> listSeats(UUID hallId) {
        return seats.findByHallIdOrderByRowLabelAscSeatNumberAsc(hallId).stream().map(SeatResponse::from).toList();
    }

    private List<Seat> layout(Hall hall, int rows, int columns, SeatType seatType) {
        List<Seat> created = new ArrayList<>(rows * columns);
        for (int row = 0; row < rows; row++) {
            String rowLabel = String.valueOf((char) ('A' + row));
            for (int number = 1; number <= columns; number++) {
                Seat seat = new Seat();
                seat.setHall(hall);
                seat.setRowLabel(rowLabel);
                seat.setSeatNumber(number);
                seat.setSeatType(seatType);
                created.add(seat);
            }
        }
        return created;
    }

    private Cinema findCinema(UUID id) {
        return cinemas.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Cinema not found."));
    }

    private Hall findHall(UUID id) {
        return halls.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Hall not found."));
    }

    private ApiException cinemaInUse() {
        return new ApiException(HttpStatus.CONFLICT,
                "This cinema's halls have showtimes, so it cannot be deleted without losing their booking history.");
    }

    private Cinema apply(Cinema cinema, CinemaRequest request) {
        cinema.setName(request.name());
        cinema.setLocation(request.location());
        cinema.setAddress(request.address());
        cinema.setCity(request.city());
        return cinema;
    }
}
