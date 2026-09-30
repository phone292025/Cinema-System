package com.cinema.movie;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class MovieDtos {
    private MovieDtos() {
    }

    public record MovieRequest(
            @NotBlank @Size(max = 220) String title,
            @NotBlank String description,
            @Min(1) int durationMinutes,
            @NotBlank @Size(max = 120) String genre,
            @NotBlank @Size(max = 80) String language,
            @NotBlank @Size(max = 30) String rating,
            String posterUrl,
            @NotNull LocalDate releaseDate,
            @DecimalMin("0.0") @DecimalMax("10.0") BigDecimal imdbRating,
            @NotNull MovieStatus status) {
    }

    public record MovieResponse(UUID id, String title, String description, int durationMinutes, String genre, String language,
            String rating, String posterUrl, LocalDate releaseDate, BigDecimal imdbRating, MovieStatus status) {
        public static MovieResponse from(Movie movie) {
            return new MovieResponse(movie.getId(), movie.getTitle(), movie.getDescription(), movie.getDurationMinutes(), movie.getGenre(),
                    movie.getLanguage(), movie.getRating(), movie.getPosterUrl(), movie.getReleaseDate(), movie.getImdbRating(), movie.getStatus());
        }
    }
}
