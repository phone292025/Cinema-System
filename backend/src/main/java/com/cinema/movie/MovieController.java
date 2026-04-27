package com.cinema.movie;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.cinema.common.ApiException;
import com.cinema.movie.MovieDtos.MovieResponse;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/movies")
public class MovieController {
    private final MovieRepository movies;

    public MovieController(MovieRepository movies) {
        this.movies = movies;
    }

    @GetMapping
    List<MovieResponse> list() {
        return movies.findByStatusInOrderByImdbRatingDescTitleAsc(List.of(MovieStatus.NOW_SHOWING, MovieStatus.COMING_SOON)).stream()
                .map(MovieResponse::from)
                .toList();
    }

    @GetMapping("/{idOrSlug}")
    MovieResponse get(@PathVariable String idOrSlug) {
        return findByIdOrSlug(idOrSlug).map(MovieResponse::from)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Movie not found."));
    }

    private java.util.Optional<Movie> findByIdOrSlug(String idOrSlug) {
        try {
            return movies.findById(UUID.fromString(idOrSlug));
        } catch (IllegalArgumentException ignored) {
            String slug = slugify(idOrSlug);
            return movies.findAll().stream()
                    .filter(movie -> slugify(movie.getTitle()).equals(slug))
                    .findFirst();
        }
    }

    private String slugify(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}
