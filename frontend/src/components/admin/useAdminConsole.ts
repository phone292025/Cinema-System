"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import type { FormEvent } from "react";

import { apiFetch, errorMessage } from "@/lib/api";
import type { AuditLog, Cinema, Dashboard, Hall, Movie, Showtime } from "@/lib/types";
import { emptyMovieForm } from "./model";
import type { CinemaForm, HallForm, MovieForm, ShowtimeForm } from "./model";
import { defaultShowtimeStart } from "./model";

function movieBody(form: MovieForm) {
  return JSON.stringify({
    ...form,
    durationMinutes: Number(form.durationMinutes),
    imdbRating: form.imdbRating ? Number(form.imdbRating) : null,
  });
}

function movieFormFrom(movie: Movie, status: Movie["status"] = movie.status): MovieForm {
  return {
    title: movie.title,
    description: movie.description ?? "",
    durationMinutes: String(movie.durationMinutes),
    genre: movie.genre ?? "",
    language: movie.language ?? "",
    rating: movie.rating ?? "",
    imdbRating: movie.imdbRating != null ? String(movie.imdbRating) : "",
    posterUrl: movie.posterUrl ?? "",
    releaseDate: movie.releaseDate?.slice(0, 10) ?? "",
    status,
  };
}

/** Data and actions behind the admin console; the page only decides which workspace to show. */
export function useAdminConsole(enabled: boolean, includeAuditLogs: boolean) {
  const [dashboard, setDashboard] = useState<Dashboard | null>(null);
  const [movies, setMovies] = useState<Movie[]>([]);
  const [cinemas, setCinemas] = useState<Cinema[]>([]);
  const [halls, setHalls] = useState<Hall[]>([]);
  const [showtimes, setShowtimes] = useState<Showtime[]>([]);
  const [auditLogs, setAuditLogs] = useState<AuditLog[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const [movieForm, setMovieForm] = useState<MovieForm>(emptyMovieForm);
  const [editingMovieId, setEditingMovieId] = useState<string | null>(null);
  const [cinemaForm, setCinemaForm] = useState<CinemaForm>({ name: "", location: "", address: "", city: "" });
  const [hallForm, setHallForm] = useState<HallForm>({
    cinemaId: "",
    name: "Hall 1",
    type: "Standard",
    totalRows: "6",
    totalColumns: "8",
    defaultSeatType: "REGULAR",
  });
  const [showtimeForm, setShowtimeForm] = useState<ShowtimeForm>(() => ({
    movieId: "",
    hallId: "",
    startTime: defaultShowtimeStart(),
    basePrice: "15.00",
  }));

  const loadHalls = useCallback((cinemaId: string) => {
    if (!cinemaId) {
      setHalls([]);
      setShowtimeForm((current) => ({ ...current, hallId: "" }));
      return;
    }

    apiFetch<Hall[]>(`/admin/cinemas/${cinemaId}/halls`)
      .then((response) => {
        setHalls(response);
        setShowtimeForm((current) => ({
          ...current,
          hallId: response.some((hall) => hall.id === current.hallId) ? current.hallId : response[0]?.id ?? "",
        }));
      })
      .catch((err) => setError(errorMessage(err, "Hall request failed.")));
  }, []);

  const load = useCallback(() => {
    setIsLoading(true);
    setError("");

    Promise.all([
      apiFetch<Dashboard>("/admin/dashboard"),
      apiFetch<Movie[]>("/admin/movies"),
      apiFetch<Cinema[]>("/admin/cinemas"),
      apiFetch<Showtime[]>("/admin/showtimes"),
      includeAuditLogs ? apiFetch<AuditLog[]>("/admin/audit-logs") : Promise.resolve([]),
    ])
      .then(([dashboardResponse, movieResponse, cinemaResponse, showtimeResponse, auditResponse]) => {
        setDashboard(dashboardResponse);
        setMovies(movieResponse);
        setCinemas(cinemaResponse);
        setShowtimes(showtimeResponse);
        setAuditLogs(auditResponse);

        setHallForm((current) => {
          const selectedCinemaId = current.cinemaId || cinemaResponse[0]?.id || "";
          if (selectedCinemaId) loadHalls(selectedCinemaId);
          return current.cinemaId === selectedCinemaId ? current : { ...current, cinemaId: selectedCinemaId };
        });

        if (movieResponse[0]) {
          setShowtimeForm((current) => ({ ...current, movieId: current.movieId || movieResponse[0].id }));
        }
      })
      .catch((err) => setError(errorMessage(err, "Dashboard request failed.")))
      .finally(() => setIsLoading(false));
  }, [includeAuditLogs, loadHalls]);

  useEffect(() => {
    if (!enabled) return undefined;
    const timer = window.setTimeout(load, 0);
    return () => window.clearTimeout(timer);
  }, [enabled, load]);

  const sortedShowtimes = useMemo(
    () => showtimes.slice().sort((a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime()),
    [showtimes],
  );

  async function run<T>(message: string, action: () => Promise<T>) {
    setError("");
    setSuccess("");
    setIsSaving(true);
    try {
      await action();
      setSuccess(message);
      load();
      return true;
    } catch (err) {
      setError(errorMessage(err, "Admin action failed."));
      return false;
    } finally {
      setIsSaving(false);
    }
  }

  async function saveMovie(event: FormEvent) {
    event.preventDefault();
    const editing = editingMovieId;
    const saved = await run(editing ? "Movie updated." : "Movie added to the catalog.", () =>
      apiFetch<Movie>(editing ? `/admin/movies/${editing}` : "/admin/movies", {
        method: editing ? "PUT" : "POST",
        body: movieBody(movieForm),
      }),
    );
    if (saved && editing) setEditingMovieId(null);
  }

  function startEditMovie(movie: Movie) {
    setEditingMovieId(movie.id);
    setMovieForm(movieFormFrom(movie));
  }

  function cancelEditMovie() {
    setEditingMovieId(null);
    setMovieForm(emptyMovieForm);
  }

  async function setMovieStatus(movie: Movie, status: Movie["status"]) {
    await run(status === "ARCHIVED" ? "Movie archived." : "Movie is back on sale.", () =>
      apiFetch<Movie>(`/admin/movies/${movie.id}`, { method: "PUT", body: movieBody(movieFormFrom(movie, status)) }),
    );
  }

  async function createCinema(event: FormEvent) {
    event.preventDefault();
    await run("Cinema branch created.", () =>
      apiFetch<Cinema>("/admin/cinemas", { method: "POST", body: JSON.stringify(cinemaForm) }),
    );
  }

  const generatedSeatCount = Math.max(0, (Number(hallForm.totalRows) || 0) * (Number(hallForm.totalColumns) || 0));

  async function createHall(event: FormEvent) {
    event.preventDefault();
    await run(`${hallForm.name} created with ${generatedSeatCount} seats.`, () =>
      apiFetch<Hall>(`/admin/cinemas/${hallForm.cinemaId}/halls`, {
        method: "POST",
        body: JSON.stringify({
          name: hallForm.name,
          type: hallForm.type,
          totalRows: Number(hallForm.totalRows),
          totalColumns: Number(hallForm.totalColumns),
          defaultSeatType: hallForm.defaultSeatType,
        }),
      }),
    );
  }

  async function createShowtime(event: FormEvent) {
    event.preventDefault();
    const start = new Date(showtimeForm.startTime);
    const movie = movies.find((item) => item.id === showtimeForm.movieId);
    const end = new Date(start.getTime() + (movie?.durationMinutes ?? 120) * 60_000);

    await run("Showtime scheduled.", () =>
      apiFetch<Showtime>("/admin/showtimes", {
        method: "POST",
        body: JSON.stringify({
          movieId: showtimeForm.movieId,
          hallId: showtimeForm.hallId,
          startTime: start.toISOString(),
          endTime: end.toISOString(),
          basePrice: Number(showtimeForm.basePrice),
          status: "SCHEDULED",
        }),
      }),
    );
  }

  return {
    dashboard,
    movies,
    cinemas,
    halls,
    showtimes: sortedShowtimes,
    auditLogs,
    isLoading,
    isSaving,
    error,
    success,
    load,
    loadHalls,
    movieForm,
    setMovieForm,
    editingMovieId,
    saveMovie,
    startEditMovie,
    cancelEditMovie,
    setMovieStatus,
    cinemaForm,
    setCinemaForm,
    createCinema,
    hallForm,
    setHallForm,
    generatedSeatCount,
    createHall,
    showtimeForm,
    setShowtimeForm,
    createShowtime,
  };
}
