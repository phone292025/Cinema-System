"use client";

import Image from "next/image";
import Link from "next/link";
import { CalendarDays, ChevronLeft, ChevronRight } from "lucide-react";
import { useEffect, useRef, useState } from "react";

import { AppShell } from "@/components/AppShell";
import { StatusBadge } from "@/components/StatusBadge";
import { apiFetch } from "@/lib/api";
import { demoMovies } from "@/lib/demo-data";
import type { Movie } from "@/lib/types";

type MovieTab = "NOW_SHOWING" | "KIDS" | "COMING_SOON";

const movieTabs: { id: MovieTab; label: string }[] = [
  { id: "NOW_SHOWING", label: "Now showing" },
  { id: "KIDS", label: "Kids" },
  { id: "COMING_SOON", label: "Coming soon" },
];

const heroSlides = [
  {
    title: "The Shawshank Redemption",
    eyebrow: "Top IMDb-rated release",
    description: "A sharp prison-drama classic with the same showtime and chair selection flow.",
    href: "/movies/the-shawshank-redemption",
    image: "/posters/shawshank-hero-hd.jpg",
  },
  {
    title: "The Godfather",
    eyebrow: "Crime drama classic",
    description: "Browse the Corleone family epic, then continue into live showtimes and seat locking.",
    href: "/movies/the-godfather",
    image: "/posters/godfather-hero-hd.jpg",
  },
  {
    title: "The Dark Knight",
    eyebrow: "Action crime feature",
    description: "Pick a Gotham showtime, choose your chair, and finish checkout from one flow.",
    href: "/movies/the-dark-knight",
    image: "/posters/dark-knight-hero-hd.jpg",
  },
  {
    title: "The Return of the King",
    eyebrow: "Fantasy adventure",
    description: "A big-screen journey with premium seats, real poster art, and fast booking.",
    href: "/movies/the-return-of-the-king",
    image: "/posters/return-king-hero-hd.jpg",
  },
  {
    title: "Pulp Fiction",
    eyebrow: "Cult crime classic",
    description: "Jump into a sharp Los Angeles crime story and reserve seats from the same carousel.",
    href: "/movies/pulp-fiction",
    image: "/posters/pulp-fiction-hero-hd.png",
  },
  {
    title: "The Good, the Bad and the Ugly",
    eyebrow: "Western landmark",
    description: "A widescreen classic with real poster art, showtimes, and quick chair selection.",
    href: "/movies/the-good-the-bad-and-the-ugly",
    image: "/posters/good-bad-ugly-hero-hd.jpg",
  },
];

function isKidsMovie(movie: Movie) {
  const categoryText = `${movie.title} ${movie.genre}`.toLowerCase();
  return ["family", "animation", "kids", "mario", "tom & jerry"].some((keyword) => categoryText.includes(keyword));
}

export default function MoviesPage() {
  const [movies, setMovies] = useState<Movie[]>([]);
  const [error, setError] = useState("");
  const [activeTab, setActiveTab] = useState<MovieTab>("NOW_SHOWING");
  const [activeHeroIndex, setActiveHeroIndex] = useState(0);
  const movieRowRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    apiFetch<Movie[]>("/movies")
      .then(setMovies)
      .catch(() => {
        setMovies(demoMovies);
        setError("Showing the demo catalog until the backend API is running.");
      });
  }, []);

  useEffect(() => {
    const timer = window.setInterval(() => {
      setActiveHeroIndex((current) => (current + 1) % heroSlides.length);
    }, 3000);

    return () => window.clearInterval(timer);
  }, []);

  const activeHero = heroSlides[activeHeroIndex];

  const visibleMovies = movies.filter((movie) => {
    if (activeTab === "KIDS") {
      return isKidsMovie(movie);
    }
    if (activeTab === "COMING_SOON") {
      return movie.status === "COMING_SOON";
    }
    return movie.status === "NOW_SHOWING";
  });

  function scrollMovieRow(direction: "left" | "right") {
    const row = movieRowRef.current;
    if (!row) return;

    const distance = Math.max(300, Math.floor(row.clientWidth * 0.76));
    row.scrollBy({
      left: direction === "left" ? -distance : distance,
      behavior: "smooth",
    });
  }

  return (
    <AppShell>
      <section className="relative min-h-[68vh] overflow-hidden border-b border-line">
        <div className="absolute inset-0 flex transition-transform duration-700 ease-out" style={{ transform: `translateX(-${activeHeroIndex * 100}%)` }}>
          {heroSlides.map((slide, index) => (
            <div key={slide.image} className="relative h-full w-full shrink-0">
              <Image src={slide.image} alt={`${slide.title} high resolution movie hero`} fill priority={index === 0} quality={95} className="object-cover" />
            </div>
          ))}
        </div>
        <div className="absolute inset-0 bg-[linear-gradient(90deg,rgba(13,15,18,0.96)_0%,rgba(13,15,18,0.74)_43%,rgba(13,15,18,0.18)_100%)]" />
        <div className="absolute inset-x-0 bottom-0 h-40 bg-gradient-to-t from-background to-transparent" />
        <div className="relative mx-auto flex min-h-[68vh] max-w-7xl flex-col justify-center px-4 py-14 sm:px-6">
          <p className="font-mono text-xs uppercase text-accent">{activeHero.eyebrow}</p>
          <h1 className="mt-3 max-w-3xl text-5xl font-semibold sm:text-7xl">{activeHero.title}</h1>
          <p className="mt-5 max-w-2xl text-lg leading-8 text-muted">{activeHero.description}</p>
          <Link href={activeHero.href} className="mt-8 flex w-fit items-center gap-2 rounded-md bg-accent px-5 py-3 text-sm font-semibold text-background">
            See showtimes
            <CalendarDays size={17} aria-hidden />
          </Link>
          <div className="mt-8 flex gap-3">
            {heroSlides.map((slide, index) => (
              <button
                key={slide.title}
                type="button"
                aria-label={`Show ${slide.title}`}
                onClick={() => setActiveHeroIndex(index)}
                className={`h-2.5 rounded-full ${index === activeHeroIndex ? "w-9 bg-accent" : "w-2.5 bg-foreground/35 hover:bg-foreground/70"}`}
              />
            ))}
          </div>
        </div>
      </section>
      <section className="relative overflow-hidden border-b border-line bg-background py-10">
        <div className="mx-auto max-w-[1540px] px-4 sm:px-6">
          <div className="mb-8 flex flex-col justify-between gap-5 lg:flex-row lg:items-start">
            <div className="flex min-w-0 flex-wrap items-center gap-x-8 gap-y-3">
              {movieTabs.map((movieTab) => {
                const active = movieTab.id === activeTab;
                return (
                  <button
                    key={movieTab.id}
                    type="button"
                    aria-pressed={active}
                    onClick={() => setActiveTab(movieTab.id)}
                    className={`relative pb-3 text-left text-xl font-semibold uppercase tracking-[0.08em] sm:text-2xl ${
                      active ? "text-foreground after:absolute after:inset-x-0 after:-bottom-0 after:h-1 after:bg-accent-strong" : "text-muted hover:text-foreground"
                    }`}
                  >
                    {movieTab.label}
                  </button>
                );
              })}
            </div>
            <Link
              href="/movies"
              className="flex w-fit items-center gap-2 rounded-md px-1 py-2 text-base font-semibold text-foreground hover:text-accent"
              aria-label="View all now showing movies"
            >
              View all
              <ChevronRight size={22} aria-hidden />
            </Link>
          </div>
        </div>
        {error && (
          <div className="mx-auto mb-5 max-w-[1540px] px-4 sm:px-6">
            <p className="rounded-md border border-accent/40 bg-accent/10 p-4 text-accent">{error}</p>
          </div>
        )}
        <div className="relative">
          <div ref={movieRowRef} className="cinema-scrollbar-none overflow-x-auto scroll-smooth px-4 pb-4 sm:px-6">
            <div className="flex w-max snap-x snap-mandatory gap-5 pr-20">
              {visibleMovies.map((movie) => (
                <Link
                  key={movie.id}
                  href={`/movies/${movie.id}`}
                  className="group w-[230px] shrink-0 snap-start overflow-hidden rounded-lg border border-accent-strong/55 bg-panel shadow-[0_18px_40px_rgba(0,0,0,0.28)] sm:w-[260px] lg:w-[286px]"
                >
                  <div className="relative aspect-[2/3] bg-background">
                    <Image
                      src={movie.posterUrl || "/cinema-hero.png"}
                      alt={movie.title}
                      fill
                      quality={92}
                      sizes="(max-width: 640px) 230px, (max-width: 1024px) 260px, 286px"
                      className="object-cover transition duration-300 group-hover:scale-[1.04]"
                    />
                    <div className="absolute left-4 top-4">
                      <StatusBadge status={movie.status} />
                    </div>
                    {movie.imdbRating ? (
                      <div className="absolute bottom-4 left-4 rounded-md bg-background/85 px-3 py-2 font-mono text-xs font-semibold text-accent backdrop-blur">
                        IMDb {Number(movie.imdbRating).toFixed(1)}
                      </div>
                    ) : null}
                  </div>
                  <div className="flex min-h-[132px] flex-col justify-center bg-[linear-gradient(180deg,rgba(23,27,32,0.96),rgba(13,15,18,0.98))] p-4 text-center">
                    <h3 className="line-clamp-2 text-lg font-semibold leading-7">{movie.title}</h3>
                    <div className="mt-3 flex flex-wrap justify-center gap-x-3 gap-y-1 text-xs text-muted">
                      <span>{movie.genre}</span>
                      <span>{movie.rating}</span>
                      <span>{movie.durationMinutes} min</span>
                    </div>
                  </div>
                </Link>
              ))}
            </div>
          </div>
          <div className="pointer-events-none absolute inset-y-0 left-0 hidden w-28 bg-gradient-to-r from-background via-background/75 to-transparent lg:block" />
          <div className="pointer-events-none absolute inset-y-0 right-0 hidden w-36 bg-gradient-to-l from-background via-background/85 to-transparent lg:block" />
          <button
            type="button"
            onClick={() => scrollMovieRow("left")}
            className="absolute left-5 top-1/2 hidden -translate-y-1/2 rounded-full border border-foreground/20 bg-background/80 p-3 text-foreground shadow-xl hover:border-accent hover:bg-accent hover:text-background lg:block"
            aria-label="Scroll movies left"
          >
            <ChevronLeft size={24} aria-hidden />
          </button>
          <button
            type="button"
            onClick={() => scrollMovieRow("right")}
            className="absolute right-5 top-1/2 hidden -translate-y-1/2 rounded-full border border-foreground/20 bg-background/80 p-3 text-foreground shadow-xl hover:border-accent hover:bg-accent hover:text-background lg:block"
            aria-label="Scroll movies right"
          >
            <ChevronRight size={24} aria-hidden />
          </button>
        </div>
      </section>
    </AppShell>
  );
}
