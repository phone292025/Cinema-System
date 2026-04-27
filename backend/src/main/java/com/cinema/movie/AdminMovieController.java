package com.cinema.movie;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.cinema.common.ApiException;
import com.cinema.movie.MovieDtos.MovieRequest;
import com.cinema.movie.MovieDtos.MovieResponse;

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
@RequestMapping("/admin/movies")
@PreAuthorize("hasAnyRole('ADMIN','STAFF')")
public class AdminMovieController {
    private final MovieRepository movies;

    public AdminMovieController(MovieRepository movies) {
        this.movies = movies;
    }

    @GetMapping
    List<MovieResponse> list() {
        return movies.findAll().stream().map(MovieResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    MovieResponse create(@Valid @RequestBody MovieRequest request) {
        return MovieResponse.from(movies.save(apply(new Movie(), request)));
    }

    @PutMapping("/{id}")
    MovieResponse update(@PathVariable UUID id, @Valid @RequestBody MovieRequest request) {
        Movie movie = movies.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Movie not found."));
        return MovieResponse.from(movies.save(apply(movie, request)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID id) {
        movies.deleteById(id);
    }

    private Movie apply(Movie movie, MovieRequest request) {
        movie.setTitle(request.title());
        movie.setDescription(request.description());
        movie.setDurationMinutes(request.durationMinutes());
        movie.setGenre(request.genre());
        movie.setLanguage(request.language());
        movie.setRating(request.rating());
        movie.setPosterUrl(request.posterUrl());
        movie.setReleaseDate(request.releaseDate());
        movie.setImdbRating(request.imdbRating() == null ? BigDecimal.ZERO : request.imdbRating());
        movie.setStatus(request.status());
        return movie;
    }
}
