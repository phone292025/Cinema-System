package com.cinema.movie;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.cinema.audit.AuditLogService;
import com.cinema.common.ApiException;
import com.cinema.movie.MovieDtos.MovieRequest;
import com.cinema.movie.MovieDtos.MovieResponse;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MovieService {
    private static final List<MovieStatus> LISTED_STATUSES = List.of(MovieStatus.NOW_SHOWING, MovieStatus.COMING_SOON);

    private final MovieRepository movies;
    private final AuditLogService auditLogs;

    public MovieService(MovieRepository movies, AuditLogService auditLogs) {
        this.movies = movies;
        this.auditLogs = auditLogs;
    }

    @Transactional(readOnly = true)
    public List<MovieResponse> listPublic() {
        return movies.findByStatusInOrderByImdbRatingDescTitleAsc(LISTED_STATUSES).stream()
                .map(MovieResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public MovieResponse getByIdOrSlug(String idOrSlug) {
        return findByIdOrSlug(idOrSlug).map(MovieResponse::from)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Movie not found."));
    }

    @Transactional(readOnly = true)
    public List<MovieResponse> listAll() {
        return movies.findAll().stream().map(MovieResponse::from).toList();
    }

    @Transactional
    public MovieResponse create(MovieRequest request) {
        Movie movie = movies.save(apply(new Movie(), request));
        auditLogs.recordAdmin("MOVIE_CREATED", "Movie", movie.getId().toString(), movie.getTitle());
        return MovieResponse.from(movie);
    }

    @Transactional
    public MovieResponse update(UUID id, MovieRequest request) {
        Movie movie = movies.save(apply(find(id), request));
        auditLogs.recordAdmin("MOVIE_UPDATED", "Movie", movie.getId().toString(), movie.getTitle() + " (" + movie.getStatus() + ")");
        return MovieResponse.from(movie);
    }

    @Transactional
    public void delete(UUID id) {
        Movie movie = find(id);
        if (movies.hasShowtimes(id)) {
            throw movieInUse();
        }
        try {
            movies.delete(movie);
            movies.flush();
            auditLogs.recordAdmin("MOVIE_DELETED", "Movie", id.toString(), movie.getTitle());
        } catch (DataIntegrityViolationException ex) {
            throw movieInUse();
        }
    }

    private ApiException movieInUse() {
        return new ApiException(HttpStatus.CONFLICT,
                "This movie has showtimes and cannot be deleted. Archive it instead by setting its status to ARCHIVED.");
    }

    private Movie find(UUID id) {
        return movies.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Movie not found."));
    }

    private Optional<Movie> findByIdOrSlug(String idOrSlug) {
        UUID id = parseUuid(idOrSlug);
        if (id != null) {
            return movies.findById(id);
        }
        String slug = Movie.slugify(idOrSlug);
        return slug.isEmpty() ? Optional.empty() : movies.findFirstBySlugOrderByReleaseDateDesc(slug);
    }

    private UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
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
