package com.cinema.movie;

import java.util.List;
import java.util.UUID;

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
@PreAuthorize("hasRole('ADMIN')")
public class AdminMovieController {
    private final MovieService movieService;

    public AdminMovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @GetMapping
    List<MovieResponse> list() {
        return movieService.listAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    MovieResponse create(@Valid @RequestBody MovieRequest request) {
        return movieService.create(request);
    }

    @PutMapping("/{id}")
    MovieResponse update(@PathVariable UUID id, @Valid @RequestBody MovieRequest request) {
        return movieService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID id) {
        movieService.delete(id);
    }
}
