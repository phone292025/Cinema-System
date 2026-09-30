package com.cinema.movie;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "movies")
@Getter
@Setter
@NoArgsConstructor
public class Movie {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String title;

    @Setter(AccessLevel.NONE)
    private String slug;

    private String description;
    private Integer durationMinutes;
    private String genre;
    private String language;
    private String rating;
    private String posterUrl;
    private LocalDate releaseDate;
    private BigDecimal imdbRating;

    @Enumerated(EnumType.STRING)
    private MovieStatus status;

    public static String slugify(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    @PrePersist
    @PreUpdate
    void syncSlug() {
        slug = title == null ? null : slugify(title);
    }
}
