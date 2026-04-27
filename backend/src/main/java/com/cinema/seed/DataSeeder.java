package com.cinema.seed;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import com.cinema.cinema.Cinema;
import com.cinema.cinema.CinemaRepository;
import com.cinema.cinema.Hall;
import com.cinema.cinema.HallRepository;
import com.cinema.cinema.Seat;
import com.cinema.cinema.SeatRepository;
import com.cinema.cinema.SeatType;
import com.cinema.movie.Movie;
import com.cinema.movie.MovieRepository;
import com.cinema.movie.MovieStatus;
import com.cinema.showtime.Showtime;
import com.cinema.showtime.ShowtimeRepository;
import com.cinema.showtime.ShowtimeSeat;
import com.cinema.showtime.ShowtimeSeatRepository;
import com.cinema.showtime.ShowtimeSeatStatus;
import com.cinema.showtime.ShowtimeStatus;
import com.cinema.user.User;
import com.cinema.user.UserRepository;
import com.cinema.user.UserRole;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataSeeder {
    @Bean
    CommandLineRunner seed(UserRepository users, MovieRepository movies, CinemaRepository cinemas, HallRepository halls,
            SeatRepository seats, ShowtimeRepository showtimes, ShowtimeSeatRepository showtimeSeats, PasswordEncoder encoder) {
        return args -> {
            if (!users.existsByEmailIgnoreCase("admin@cinema.test")) {
                users.save(user("Admin", "admin@cinema.test", "admin12345", UserRole.ADMIN, encoder));
            }
            if (!users.existsByEmailIgnoreCase("customer@cinema.test")) {
                users.save(user("Demo Customer", "customer@cinema.test", "customer12345", UserRole.CUSTOMER, encoder));
            }

            List<SeedMovie> catalog = topRatedCatalog();
            List<Movie> seededMovies = new ArrayList<>();
            for (SeedMovie seedMovie : catalog) {
                Movie movie = movies.findByTitleIgnoreCase(seedMovie.title()).orElseGet(Movie::new);
                applyMovie(movie, seedMovie);
                seededMovies.add(movies.save(movie));
            }
            archiveGeneratedDemoMovie(movies, "Aurora Run");
            archiveGeneratedDemoMovie(movies, "The Midnight Kitchen");

            Cinema cinema = cinemas.findAll().stream().findFirst().orElseGet(() -> {
                Cinema created = new Cinema();
                created.setName("Central Cineplex");
                created.setLocation("Downtown");
                created.setAddress("18 Market Street");
                created.setCity("Singapore");
                return cinemas.save(created);
            });

            Hall hall = halls.findByCinemaId(cinema.getId()).stream().findFirst().orElseGet(() -> {
                Hall created = new Hall();
                created.setCinema(cinema);
                created.setName("Hall 1");
                created.setType("IMAX");
                created.setTotalRows(6);
                created.setTotalColumns(8);
                return halls.save(created);
            });

            List<Seat> createdSeats = seats.findByHallIdOrderByRowLabelAscSeatNumberAsc(hall.getId());
            if (createdSeats.isEmpty()) {
                List<Seat> newSeats = new ArrayList<>();
                for (int row = 0; row < hall.getTotalRows(); row++) {
                    String rowLabel = String.valueOf((char) ('A' + row));
                    for (int number = 1; number <= hall.getTotalColumns(); number++) {
                        Seat seat = new Seat();
                        seat.setHall(hall);
                        seat.setRowLabel(rowLabel);
                        seat.setSeatNumber(number);
                        seat.setSeatType(row >= 4 ? SeatType.PREMIUM : SeatType.REGULAR);
                        newSeats.add(seat);
                    }
                }
                createdSeats = seats.saveAll(newSeats);
            }
            List<Seat> hallSeats = createdSeats;

            for (int index = 0; index < seededMovies.size(); index++) {
                Movie movie = seededMovies.get(index);
                if (movie.getStatus() == MovieStatus.ARCHIVED) {
                    continue;
                }
                if (!showtimes.findByMovieIdAndStartTimeAfterOrderByStartTimeAsc(movie.getId(), Instant.now()).isEmpty()) {
                    continue;
                }
                for (int i = 1; i <= 3; i++) {
                    Showtime showtime = new Showtime();
                    showtime.setMovie(movie);
                    showtime.setHall(hall);
                    long movieOffset = Math.min(index, 9);
                    showtime.setStartTime(Instant.now().plus(i * 3L + movieOffset, ChronoUnit.HOURS));
                    showtime.setEndTime(showtime.getStartTime().plus(movie.getDurationMinutes(), ChronoUnit.MINUTES));
                    showtime.setBasePrice(basePriceFor(movie));
                    showtime.setStatus(ShowtimeStatus.SCHEDULED);
                    showtimes.save(showtime);
                    showtimeSeats.saveAll(hallSeats.stream().map(seat -> showtimeSeat(showtime, seat)).toList());
                }
            }
        };
    }

    private List<SeedMovie> topRatedCatalog() {
        return List.of(
                seedMovie("The Shawshank Redemption",
                        "Two imprisoned men build an unlikely friendship across decades inside Shawshank State Penitentiary.",
                        142, "Drama", "English", "R",
                        "/posters/shawshank-redemption.jpg",
                        LocalDate.of(1994, 9, 23), new BigDecimal("9.3"), MovieStatus.NOW_SHOWING),
                seedMovie("The Godfather",
                        "The aging patriarch of a crime dynasty transfers control of his empire to his reluctant son.",
                        175, "Crime Drama", "English", "R",
                        "/posters/godfather.jpg",
                        LocalDate.of(1972, 3, 24), new BigDecimal("9.2"), MovieStatus.NOW_SHOWING),
                seedMovie("The Dark Knight",
                        "Batman faces a criminal mastermind who pushes Gotham City and its heroes into chaos.",
                        152, "Action Crime", "English", "PG-13",
                        "/posters/dark-knight.jpg",
                        LocalDate.of(2008, 7, 18), new BigDecimal("9.0"), MovieStatus.NOW_SHOWING),
                seedMovie("The Godfather Part II",
                        "The Corleone family expands its power while the past reveals Vito Corleone's rise.",
                        202, "Crime Drama", "English", "R",
                        "/posters/godfather-part-ii.jpg",
                        LocalDate.of(1974, 12, 20), new BigDecimal("9.0"), MovieStatus.NOW_SHOWING),
                seedMovie("12 Angry Men",
                        "A dissenting juror forces a tense room to reconsider the fate of a young defendant.",
                        96, "Legal Drama", "English", "Approved",
                        "/posters/12-angry-men.jpg",
                        LocalDate.of(1957, 4, 10), new BigDecimal("9.0"), MovieStatus.NOW_SHOWING),
                seedMovie("The Lord of the Rings: The Return of the King",
                        "The fellowship's final battle decides the fate of Middle-earth and the One Ring.",
                        201, "Fantasy Adventure", "English", "PG-13",
                        "/posters/return-of-the-king.jpg",
                        LocalDate.of(2003, 12, 17), new BigDecimal("9.0"), MovieStatus.NOW_SHOWING),
                seedMovie("Schindler's List",
                        "A German industrialist saves more than a thousand Jewish refugees during the Holocaust.",
                        195, "Historical Drama", "English", "R",
                        "/posters/schindlers-list.jpg",
                        LocalDate.of(1993, 12, 15), new BigDecimal("9.0"), MovieStatus.NOW_SHOWING),
                seedMovie("Pulp Fiction",
                        "Los Angeles criminals, boxers, and fixers collide in a fractured crime story.",
                        154, "Crime Drama", "English", "R",
                        "/posters/pulp-fiction.jpg",
                        LocalDate.of(1994, 10, 14), new BigDecimal("8.9"), MovieStatus.NOW_SHOWING),
                seedMovie("The Lord of the Rings: The Fellowship of the Ring",
                        "A hobbit joins a fellowship tasked with carrying the One Ring toward Mordor.",
                        178, "Fantasy Adventure", "English", "PG-13",
                        "/posters/fellowship-of-the-ring.jpg",
                        LocalDate.of(2001, 12, 19), new BigDecimal("8.9"), MovieStatus.NOW_SHOWING),
                seedMovie("The Good, the Bad and the Ugly",
                        "Three gunslingers hunt for buried gold during the American Civil War.",
                        161, "Western", "Italian", "R",
                        "/posters/good-bad-ugly.jpg",
                        LocalDate.of(1966, 12, 23), new BigDecimal("8.8"), MovieStatus.NOW_SHOWING),
                seedMovie("The Super Mario Bros. Movie",
                        "Brooklyn plumbers Mario and Luigi land in the Mushroom Kingdom and join Princess Peach to stop Bowser.",
                        92, "Family Animation", "English", "PG",
                        "/posters/super-mario-bros-movie.jpg",
                        LocalDate.of(2023, 4, 5), new BigDecimal("7.0"), MovieStatus.NOW_SHOWING),
                seedMovie("Tom & Jerry",
                        "Tom and Jerry bring their rivalry into a New York hotel before a high-profile wedding.",
                        101, "Family Comedy", "English", "PG",
                        "/posters/tom-and-jerry-2021.jpg",
                        LocalDate.of(2021, 2, 26), new BigDecimal("5.2"), MovieStatus.NOW_SHOWING),
                seedMovie("Project Hail Mary",
                        "Ryland Grace wakes alone on a deep-space mission and must solve a planet-saving mystery before Earth freezes.",
                        156, "Sci-Fi", "English", "PG-13",
                        "/posters/project-hail-mary.jpg",
                        LocalDate.of(2026, 3, 20), BigDecimal.ZERO, MovieStatus.COMING_SOON));
    }

    private SeedMovie seedMovie(String title, String description, int duration, String genre, String language, String rating,
            String posterUrl, LocalDate releaseDate, BigDecimal imdbRating, MovieStatus status) {
        return new SeedMovie(title, description, duration, genre, language, rating, posterUrl, releaseDate, imdbRating, status);
    }

    private void applyMovie(Movie movie, SeedMovie seedMovie) {
        movie.setTitle(seedMovie.title());
        movie.setDescription(seedMovie.description());
        movie.setDurationMinutes(seedMovie.durationMinutes());
        movie.setGenre(seedMovie.genre());
        movie.setLanguage(seedMovie.language());
        movie.setRating(seedMovie.rating());
        movie.setPosterUrl(seedMovie.posterUrl());
        movie.setReleaseDate(seedMovie.releaseDate());
        movie.setImdbRating(seedMovie.imdbRating());
        movie.setStatus(seedMovie.status());
    }

    private void archiveGeneratedDemoMovie(MovieRepository movies, String title) {
        movies.findByTitleIgnoreCase(title).ifPresent(movie -> {
            movie.setStatus(MovieStatus.ARCHIVED);
            movie.setImdbRating(BigDecimal.ZERO);
            movies.save(movie);
        });
    }

    private BigDecimal basePriceFor(Movie movie) {
        if ("Project Hail Mary".equals(movie.getTitle())) {
            return new BigDecimal("22.00");
        }
        return movie.getImdbRating().compareTo(new BigDecimal("9.0")) >= 0 ? new BigDecimal("18.00") : new BigDecimal("16.00");
    }

    private User user(String name, String email, String password, UserRole role, PasswordEncoder encoder) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash(encoder.encode(password));
        user.setRole(role);
        return user;
    }

    private ShowtimeSeat showtimeSeat(Showtime showtime, Seat seat) {
        ShowtimeSeat showtimeSeat = new ShowtimeSeat();
        showtimeSeat.setShowtime(showtime);
        showtimeSeat.setSeat(seat);
        showtimeSeat.setStatus(ShowtimeSeatStatus.AVAILABLE);
        showtimeSeat.setPrice(showtime.getBasePrice().add(seat.getSeatType() == SeatType.PREMIUM ? new BigDecimal("4.00") : BigDecimal.ZERO));
        return showtimeSeat;
    }

    private record SeedMovie(String title, String description, int durationMinutes, String genre, String language, String rating,
            String posterUrl, LocalDate releaseDate, BigDecimal imdbRating, MovieStatus status) {
    }
}
