"use client";

import Image from "next/image";
import Link from "next/link";
import { useParams } from "next/navigation";
import { ArrowLeft, CalendarDays, Clock3, MapPin, Ticket } from "lucide-react";
import { useMemo, useState } from "react";

import { AppShell } from "@/components/AppShell";
import { ErrorState } from "@/components/Feedback";
import { ApiError, apiFetch } from "@/lib/api";
import { backdropFor, skipImageOptimization } from "@/lib/showcase";
import type { Movie, Showtime } from "@/lib/types";
import { useApiQuery } from "@/lib/useApiQuery";

async function loadMovie(idOrSlug: string) {
  const movie = await apiFetch<Movie>(`/movies/${idOrSlug}`);
  const showtimes = await apiFetch<Showtime[]>(`/showtimes?movieId=${movie.id}`);
  return { movie, showtimes };
}

const noShowtimes: Showtime[] = [];

function statusLabel(status: Movie["status"]) {
  return status.replace("_", " ").toLowerCase();
}

function formatDuration(minutes: number) {
  const hours = Math.floor(minutes / 60);
  const remaining = minutes % 60;
  if (hours === 0) {
    return `${remaining} min`;
  }

  return `${hours} hr${hours > 1 ? "s" : ""}${remaining ? ` ${remaining} min` : ""}`;
}

function localDateKey(value: string | Date) {
  const date = typeof value === "string" ? new Date(value) : value;
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function buildDateTabs(showtimes: Showtime[]) {
  const anchor = showtimes[0] ? new Date(showtimes[0].startTime) : new Date();
  const start = new Date(anchor.getFullYear(), anchor.getMonth(), anchor.getDate());

  return Array.from({ length: 5 }, (_, index) => {
    const date = new Date(start);
    date.setDate(start.getDate() + index);
    const isToday = localDateKey(date) === localDateKey(new Date());

    return {
      key: localDateKey(date),
      weekday: isToday ? "Today" : date.toLocaleDateString("en-US", { weekday: "short" }),
      day: date.toLocaleDateString("en-US", { day: "2-digit", month: "short" }).toUpperCase(),
    };
  });
}

export default function MovieDetailPage() {
  const params = useParams<{ id: string }>();
  const { data, error, loading, reload } = useApiQuery(params.id, loadMovie);
  const [selectedDate, setSelectedDate] = useState("");
  const movie = data?.movie ?? null;
  const showtimes = data?.showtimes ?? noShowtimes;
  const notFound = error instanceof ApiError && error.status === 404;

  const dateTabs = useMemo(() => buildDateTabs(showtimes), [showtimes]);
  const activeDate = dateTabs.some((tab) => tab.key === selectedDate) ? selectedDate : dateTabs[0]?.key ?? "";
  const visibleShowtimes = showtimes.filter((showtime) => localDateKey(showtime.startTime) === activeDate);
  const backdrop = movie ? backdropFor(movie) : null;

  return (
    <AppShell>
      <section className="relative min-h-[calc(100dvh-66px)] overflow-hidden border-b border-line bg-background">
        {movie && backdrop ? (
          <Image
            src={backdrop}
            alt={`${movie.title} cinema backdrop`}
            fill
            priority
            quality={96}
            sizes="100vw"
            unoptimized={skipImageOptimization(backdrop)}
            className="object-cover object-center"
          />
        ) : null}
        <div className="absolute inset-0 bg-[linear-gradient(90deg,rgba(8,9,10,0.96)_0%,rgba(8,9,10,0.82)_38%,rgba(8,9,10,0.46)_68%,rgba(8,9,10,0.74)_100%)]" />
        <div className="absolute inset-0 bg-[linear-gradient(180deg,rgba(8,9,10,0.20)_0%,rgba(8,9,10,0.18)_42%,#0d0f12_100%)]" />
        <div className="absolute inset-x-0 bottom-0 h-44 bg-gradient-to-t from-background to-transparent" />

        <div className="relative mx-auto flex min-h-[calc(100dvh-66px)] max-w-7xl flex-col px-4 pb-8 pt-8 sm:px-6 lg:pt-12">
          <Link href="/movies" className="flex w-fit items-center gap-3 rounded-md px-1 py-2 text-base font-semibold text-foreground hover:text-accent">
            <ArrowLeft size={20} aria-hidden />
            Back
          </Link>

          {error && !movie ? (
            <ErrorState
              className="mt-6 max-w-xl bg-background/75"
              title={notFound ? "We couldn't find this movie" : "We couldn't load this movie"}
              message={notFound ? "It may have been taken off sale. Browse what is showing now instead." : error.message}
              onRetry={notFound ? undefined : reload}
              retrying={loading}
            >
              <Link href="/movies" className="text-sm font-semibold text-muted hover:text-accent">
                Browse movies
              </Link>
            </ErrorState>
          ) : null}

          {movie ? (
            <div className="mt-auto max-w-6xl pb-2 pt-20">
              <p className="text-sm font-semibold uppercase text-accent">{statusLabel(movie.status)}</p>
              <h1 className="mt-5 max-w-5xl text-balance text-5xl font-semibold leading-[0.96] text-foreground sm:text-7xl lg:text-8xl">
                {movie.title}
              </h1>

              <div className="mt-7 flex flex-wrap items-center gap-x-4 gap-y-3 text-base font-medium text-foreground/88 sm:text-lg">
                <span className="rounded-md border border-foreground/30 px-2.5 py-1 text-sm font-semibold">{movie.rating}</span>
                <span>{movie.genre}</span>
                <span className="text-foreground/40" aria-hidden>
                  |
                </span>
                <span>{formatDuration(movie.durationMinutes)}</span>
                <span className="text-foreground/40" aria-hidden>
                  |
                </span>
                <span>{movie.language}</span>
                {movie.imdbRating ? (
                  <>
                    <span className="text-foreground/40" aria-hidden>
                      |
                    </span>
                    <span className="text-accent">IMDb {Number(movie.imdbRating).toFixed(1)}</span>
                  </>
                ) : null}
              </div>

              <p id="movie-info" className="mt-6 max-w-3xl text-lg leading-8 text-foreground/78 sm:text-xl">
                {movie.description}
              </p>

              <div className="mt-12 flex max-w-3xl gap-8 overflow-x-auto pb-2 cinema-scrollbar-none" role="group" aria-label="Select show date">
                {dateTabs.map((tab) => {
                  const active = tab.key === activeDate;
                  return (
                    <button
                      type="button"
                      key={tab.key}
                      aria-pressed={active}
                      onClick={() => setSelectedDate(tab.key)}
                      className={`min-w-24 border-b-4 pb-2 text-left transition duration-300 ${
                        active ? "border-accent-strong text-foreground" : "border-transparent text-muted hover:text-foreground"
                      }`}
                    >
                      <span className="block text-lg font-semibold">{tab.weekday}</span>
                      <span className="block text-xl font-bold">{tab.day}</span>
                    </button>
                  );
                })}
              </div>
            </div>
          ) : error ? null : (
            <div role="status" aria-label="Loading movie" className="mt-auto max-w-4xl pb-16 pt-24">
              <div className="h-5 w-36 animate-pulse rounded-md bg-panel" />
              <div className="mt-5 h-20 max-w-3xl animate-pulse rounded-md bg-panel" />
              <div className="mt-5 h-6 max-w-xl animate-pulse rounded-md bg-panel" />
            </div>
          )}
        </div>
      </section>

      {movie ? (
        <section id="showtimes" className="bg-background px-4 py-12 sm:px-6">
          <div className="mx-auto max-w-7xl">
            <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
              <div>
                <p className="text-sm font-semibold uppercase text-accent">Choose your session</p>
                <h2 className="mt-2 flex items-center gap-3 text-3xl font-semibold text-foreground">
                  <CalendarDays size={28} className="text-accent" aria-hidden />
                  Showtimes
                </h2>
              </div>
              <p className="text-sm text-muted">
                Selected date: <span className="font-semibold text-foreground">{dateTabs.find((tab) => tab.key === activeDate)?.day ?? "Today"}</span>
              </p>
            </div>

            <div className="mt-6">
              <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
                {visibleShowtimes.map((showtime) => (
                  <Link
                    key={showtime.id}
                    href={`/showtimes/${showtime.id}/seats`}
                    className="group rounded-lg border border-line bg-panel/80 p-5 hover:border-accent hover:bg-panel"
                  >
                    <div className="flex items-start justify-between gap-4">
                      <div>
                        <p className="text-2xl font-semibold text-foreground">
                          {new Date(showtime.startTime).toLocaleTimeString("en-US", { hour: "numeric", minute: "2-digit" })}
                        </p>
                        <p className="mt-2 flex items-center gap-2 text-sm text-muted">
                          <MapPin size={15} aria-hidden />
                          {showtime.cinemaName}, {showtime.hallName}
                        </p>
                      </div>
                      <span className="rounded-md bg-accent/15 px-3 py-1 text-sm font-semibold text-accent">${Number(showtime.basePrice).toFixed(2)}</span>
                    </div>
                    <div className="mt-5 flex items-center justify-between border-t border-line pt-4 text-sm text-muted">
                      <span className="flex items-center gap-2">
                        <Clock3 size={15} aria-hidden />
                        {formatDuration(movie.durationMinutes)}
                      </span>
                      <span className="flex items-center gap-2 font-semibold text-foreground group-hover:text-accent">
                        Select seats
                        <Ticket size={16} aria-hidden />
                      </span>
                    </div>
                  </Link>
                ))}
                {visibleShowtimes.length === 0 ? (
                  <p className="rounded-lg border border-line bg-panel p-5 text-muted md:col-span-2 xl:col-span-3">No showtimes for this date. Pick another day above.</p>
                ) : null}
              </div>
            </div>
          </div>
        </section>
      ) : null}
    </AppShell>
  );
}
