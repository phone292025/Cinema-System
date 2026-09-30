package com.cinema.movie;

import java.util.List;

import com.cinema.movie.MovieDtos.MovieResponse;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/movies")
public class MovieController {
    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @GetMapping
    List<MovieResponse> list() {
        return movieService.listPublic();
    }

    @GetMapping("/{idOrSlug}")
    MovieResponse get(@PathVariable String idOrSlug) {
        return movieService.getByIdOrSlug(idOrSlug);
    }
}
