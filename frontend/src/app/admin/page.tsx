"use client";

import {
  Activity,
  Armchair,
  CalendarClock,
  CalendarPlus,
  CheckCircle2,
  ChevronRight,
  CircleDollarSign,
  Film,
  MapPinned,
  Plus,
  Rows3,
  Search,
  Theater,
  Ticket,
  UsersRound,
} from "lucide-react";
import { FormEvent, useCallback, useEffect, useMemo, useState } from "react";

import { AppShell } from "@/components/AppShell";
import { StatusBadge } from "@/components/StatusBadge";
import { apiFetch, getStoredUser } from "@/lib/api";
import type { Cinema, Dashboard, Movie, Showtime, User } from "@/lib/types";

type Hall = {
  id: string;
  cinemaId: string;
  name: string;
  type: string;
  totalRows: number;
  totalColumns: number;
};

type PanelKey = "movie" | "cinema" | "hall" | "showtime";

const panelMeta: Record<PanelKey, { title: string; helper: string; icon: React.ReactNode }> = {
  movie: {
    title: "Movie",
    helper: "Add poster, rating, runtime, and catalog status.",
    icon: <Film size={17} aria-hidden />,
  },
  cinema: {
    title: "Cinema",
    helper: "Create a branch used by halls and showtimes.",
    icon: <MapPinned size={17} aria-hidden />,
  },
  hall: {
    title: "Hall and seats",
    helper: "Generate the hall and its default chair layout.",
    icon: <Rows3 size={17} aria-hidden />,
  },
  showtime: {
    title: "Showtime",
    helper: "Schedule a movie into a hall with a base price.",
    icon: <CalendarPlus size={17} aria-hidden />,
  },
};

export default function AdminPage() {
  const [user, setUser] = useState<User | null>(null);
  const [dashboard, setDashboard] = useState<Dashboard | null>(null);
  const [movies, setMovies] = useState<Movie[]>([]);
  const [cinemas, setCinemas] = useState<Cinema[]>([]);
  const [halls, setHalls] = useState<Hall[]>([]);
  const [showtimes, setShowtimes] = useState<Showtime[]>([]);
  const [activePanel, setActivePanel] = useState<PanelKey>("showtime");
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState("");
  const [movieForm, setMovieForm] = useState({
    title: "",
    description: "",
    durationMinutes: "120",
    genre: "Drama",
    language: "English",
    rating: "PG-13",
    posterUrl: "/cinema-hero.png",
    releaseDate: "2026-04-24",
    status: "NOW_SHOWING",
  });
  const [cinemaForm, setCinemaForm] = useState({ name: "", location: "", address: "", city: "" });
  const [hallForm, setHallForm] = useState({ cinemaId: "", name: "Hall 1", type: "Standard", totalRows: "6", totalColumns: "8" });
  const [showtimeForm, setShowtimeForm] = useState({
    movieId: "",
    hallId: "",
    startTime: "2026-04-25T18:00",
    basePrice: "15.00",
  });

  const canManage = user?.role === "ADMIN" || user?.role === "STAFF";

  const loadHalls = useCallback((cinemaId: string) => {
    if (!cinemaId) {
      setHalls([]);
      return;
    }

    apiFetch<Hall[]>(`/admin/cinemas/${cinemaId}/halls`)
      .then((response) => {
        setHalls(response);
        if (response[0]) {
          setShowtimeForm((current) => ({ ...current, hallId: current.hallId || response[0].id }));
        }
      })
      .catch((err) => setError(err instanceof Error ? err.message : "Hall request failed."));
  }, []);

  const load = useCallback(() => {
    setIsLoading(true);
    setError("");
    Promise.all([
      apiFetch<Dashboard>("/admin/dashboard"),
      apiFetch<Movie[]>("/admin/movies"),
      apiFetch<Cinema[]>("/admin/cinemas"),
      apiFetch<Showtime[]>("/admin/showtimes"),
    ])
      .then(([dashboardResponse, movieResponse, cinemaResponse, showtimeResponse]) => {
        setDashboard(dashboardResponse);
        setMovies(movieResponse);
        setCinemas(cinemaResponse);
        setShowtimes(showtimeResponse);
        if (cinemaResponse[0]) {
          setHallForm((current) => ({ ...current, cinemaId: current.cinemaId || cinemaResponse[0].id }));
          loadHalls(cinemaResponse[0].id);
        }
        if (movieResponse[0]) {
          setShowtimeForm((current) => ({ ...current, movieId: current.movieId || movieResponse[0].id }));
        }
      })
      .catch((err) => setError(err instanceof Error ? err.message : "Dashboard request failed."))
      .finally(() => setIsLoading(false));
  }, [loadHalls]);

  useEffect(() => {
    const sync = () => setUser(getStoredUser());
    sync();
    window.addEventListener("cinema-auth", sync);
    window.addEventListener("storage", sync);
    return () => {
      window.removeEventListener("cinema-auth", sync);
      window.removeEventListener("storage", sync);
    };
  }, []);

  useEffect(() => {
    if (canManage) {
      const loadTimer = window.setTimeout(() => {
        void load();
      }, 0);
      return () => window.clearTimeout(loadTimer);
    }
    return undefined;
  }, [canManage, load]);

  const occupancyEstimate = useMemo(() => {
    if (!dashboard || dashboard.showtimes === 0) {
      return "0%";
    }

    const paidShare = Math.min(96, Math.round((dashboard.paidBookings / Math.max(1, dashboard.showtimes * 4)) * 100));
    return `${paidShare}%`;
  }, [dashboard]);

  const nextShowtime = showtimes
    .slice()
    .sort((a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime())[0];

  async function createMovie(event: FormEvent) {
    event.preventDefault();
    await run(() =>
      apiFetch<Movie>("/admin/movies", {
        method: "POST",
        body: JSON.stringify({
          ...movieForm,
          durationMinutes: Number(movieForm.durationMinutes),
        }),
      }),
    );
  }

  async function createCinema(event: FormEvent) {
    event.preventDefault();
    await run(() => apiFetch<Cinema>("/admin/cinemas", { method: "POST", body: JSON.stringify(cinemaForm) }));
  }

  async function createHall(event: FormEvent) {
    event.preventDefault();
    await run(() =>
      apiFetch<Hall>(`/admin/cinemas/${hallForm.cinemaId}/halls`, {
        method: "POST",
        body: JSON.stringify({
          name: hallForm.name,
          type: hallForm.type,
          totalRows: Number(hallForm.totalRows),
          totalColumns: Number(hallForm.totalColumns),
        }),
      }).then(async (hall) => {
        await apiFetch(`/admin/halls/${hall.id}/seat-layout`, {
          method: "POST",
          body: JSON.stringify({
            rows: Number(hallForm.totalRows),
            columns: Number(hallForm.totalColumns),
            defaultSeatType: "REGULAR",
          }),
        });
        return hall;
      }),
    );
  }

  async function createShowtime(event: FormEvent) {
    event.preventDefault();
    const start = new Date(showtimeForm.startTime);
    const movie = movies.find((item) => item.id === showtimeForm.movieId);
    const end = new Date(start.getTime() + (movie?.durationMinutes ?? 120) * 60_000);
    await run(() =>
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

  async function run<T>(action: () => Promise<T>) {
    setError("");
    try {
      await action();
      load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Admin action failed.");
    }
  }

  return (
    <AppShell>
      <section className="relative overflow-hidden bg-background px-4 py-8 sm:px-6">
        <div className="absolute inset-x-0 top-0 h-80 bg-[radial-gradient(circle_at_20%_20%,rgba(244,184,96,0.18),transparent_32%),radial-gradient(circle_at_82%_0%,rgba(68,194,141,0.11),transparent_30%)]" />
        <div className="relative mx-auto max-w-7xl">
          <div className="grid gap-6 lg:grid-cols-[1fr_360px]">
            <div className="rounded-lg border border-line bg-panel/70 p-6 shadow-[0_24px_80px_rgba(0,0,0,0.22)]">
              <p className="font-mono text-xs uppercase text-accent">Cinema operations</p>
              <h1 className="mt-4 max-w-3xl text-5xl font-semibold leading-none text-foreground sm:text-6xl">
                Admin command center
              </h1>
              <p className="mt-5 max-w-2xl text-lg leading-8 text-muted">
                Manage movies, branches, halls, seats, schedules, and the booking pulse from one focused dashboard.
              </p>

              <div className="mt-8 grid gap-3 sm:grid-cols-3">
                <SignalCard icon={<CheckCircle2 size={18} aria-hidden />} label="System" value={canManage ? "Ready" : "Locked"} detail="Admin and staff only" />
                <SignalCard icon={<CalendarClock size={18} aria-hidden />} label="Next session" value={nextShowtime ? formatTime(nextShowtime.startTime) : "No queue"} detail={nextShowtime?.movieTitle ?? "Create a showtime"} />
                <SignalCard icon={<Activity size={18} aria-hidden />} label="Occupancy" value={occupancyEstimate} detail="Based on paid bookings" />
              </div>
            </div>

            <div className="rounded-lg border border-line bg-background/80 p-5 shadow-[0_24px_80px_rgba(0,0,0,0.22)]">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-muted">Signed in as</p>
                  <p className="mt-1 text-xl font-semibold">{user?.name ?? "Guest"}</p>
                </div>
                <span className="grid size-11 place-items-center rounded-md bg-accent/15 text-accent">
                  <UsersRound size={20} aria-hidden />
                </span>
              </div>
              <div className="mt-5 rounded-md border border-line bg-panel p-4">
                <p className="text-sm text-muted">Role</p>
                <p className="mt-1 text-2xl font-semibold text-accent">{user?.role ?? "Not logged in"}</p>
              </div>
              <button
                type="button"
                onClick={() => canManage && load()}
                disabled={!canManage || isLoading}
                className="mt-4 flex w-full items-center justify-between rounded-md bg-accent px-4 py-3 font-semibold text-background disabled:cursor-not-allowed disabled:opacity-50"
              >
                {isLoading ? "Refreshing" : "Refresh dashboard"}
                <ChevronRight size={18} aria-hidden />
              </button>
            </div>
          </div>

          {!canManage ? (
            <AccessRequired />
          ) : (
            <>
              {error ? <p className="mt-6 rounded-md border border-danger/40 bg-danger/10 p-4 text-danger">{error}</p> : null}

              <div className="mt-6 grid gap-3 md:grid-cols-5">
                <MetricCard icon={<Film size={18} aria-hidden />} label="Movies" value={dashboard?.movies ?? movies.length} detail="Catalog records" />
                <MetricCard icon={<CalendarClock size={18} aria-hidden />} label="Showtimes" value={dashboard?.showtimes ?? showtimes.length} detail="Scheduled sessions" />
                <MetricCard icon={<Ticket size={18} aria-hidden />} label="Bookings" value={dashboard?.bookings ?? 0} detail="Total reservations" />
                <MetricCard icon={<CheckCircle2 size={18} aria-hidden />} label="Paid" value={dashboard?.paidBookings ?? 0} detail="Confirmed orders" />
                <MetricCard icon={<CircleDollarSign size={18} aria-hidden />} label="Revenue" value={`$${Number(dashboard?.revenue ?? 0).toFixed(2)}`} detail="Captured payments" />
              </div>

              <div className="mt-8 grid gap-6 xl:grid-cols-[360px_1fr]">
                <div className="space-y-3">
                  {(Object.keys(panelMeta) as PanelKey[]).map((key) => {
                    const item = panelMeta[key];
                    const active = activePanel === key;
                    return (
                      <button
                        type="button"
                        key={key}
                        onClick={() => setActivePanel(key)}
                        className={`group w-full rounded-lg border p-4 text-left transition duration-300 ease-[cubic-bezier(0.32,0.72,0,1)] active:scale-[0.99] ${
                          active ? "border-accent bg-accent/12 text-foreground" : "border-line bg-panel/70 text-muted hover:border-accent/60 hover:text-foreground"
                        }`}
                      >
                        <span className="flex items-center justify-between gap-4">
                          <span className="flex items-center gap-3">
                            <span className="grid size-10 place-items-center rounded-md bg-background text-accent">{item.icon}</span>
                            <span>
                              <span className="block font-semibold">{item.title}</span>
                              <span className="mt-1 block text-sm">{item.helper}</span>
                            </span>
                          </span>
                          <ChevronRight size={18} className="transition duration-300 group-hover:translate-x-1" aria-hidden />
                        </span>
                      </button>
                    );
                  })}
                </div>

                <div className="rounded-lg border border-line bg-panel/70 p-5 shadow-[0_24px_80px_rgba(0,0,0,0.22)]">
                  <div className="mb-5 flex flex-col justify-between gap-3 sm:flex-row sm:items-center">
                    <div>
                      <p className="text-sm text-accent">Create record</p>
                      <h2 className="mt-1 text-3xl font-semibold">{panelMeta[activePanel].title}</h2>
                    </div>
                    <span className="flex items-center gap-2 rounded-md border border-line bg-background px-3 py-2 text-sm text-muted">
                      <Armchair size={16} aria-hidden />
                      Seat layout ready
                    </span>
                  </div>

                  {activePanel === "movie" ? (
                    <AdminForm onSubmit={createMovie} buttonLabel="Add movie" icon={<Plus size={17} aria-hidden />}>
                      <div className="grid gap-3 lg:grid-cols-2">
                        <Input label="Title" value={movieForm.title} onChange={(value) => setMovieForm((current) => ({ ...current, title: value }))} />
                        <Input label="Poster URL" value={movieForm.posterUrl} onChange={(value) => setMovieForm((current) => ({ ...current, posterUrl: value }))} />
                      </div>
                      <Input label="Description" value={movieForm.description} onChange={(value) => setMovieForm((current) => ({ ...current, description: value }))} />
                      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
                        <Input label="Duration" value={movieForm.durationMinutes} onChange={(value) => setMovieForm((current) => ({ ...current, durationMinutes: value }))} />
                        <Input label="Genre" value={movieForm.genre} onChange={(value) => setMovieForm((current) => ({ ...current, genre: value }))} />
                        <Input label="Language" value={movieForm.language} onChange={(value) => setMovieForm((current) => ({ ...current, language: value }))} />
                        <Input label="Rating" value={movieForm.rating} onChange={(value) => setMovieForm((current) => ({ ...current, rating: value }))} />
                      </div>
                      <div className="grid gap-3 sm:grid-cols-2">
                        <Input label="Release date" type="date" value={movieForm.releaseDate} onChange={(value) => setMovieForm((current) => ({ ...current, releaseDate: value }))} />
                        <Select
                          label="Status"
                          value={movieForm.status}
                          onChange={(value) => setMovieForm((current) => ({ ...current, status: value }))}
                          options={[
                            ["NOW_SHOWING", "Now showing"],
                            ["COMING_SOON", "Coming soon"],
                            ["ARCHIVED", "Archived"],
                          ]}
                        />
                      </div>
                    </AdminForm>
                  ) : null}

                  {activePanel === "cinema" ? (
                    <AdminForm onSubmit={createCinema} buttonLabel="Add cinema" icon={<Plus size={17} aria-hidden />}>
                      <div className="grid gap-3 lg:grid-cols-2">
                        <Input label="Name" value={cinemaForm.name} onChange={(value) => setCinemaForm((current) => ({ ...current, name: value }))} />
                        <Input label="City" value={cinemaForm.city} onChange={(value) => setCinemaForm((current) => ({ ...current, city: value }))} />
                      </div>
                      <Input label="Location" value={cinemaForm.location} onChange={(value) => setCinemaForm((current) => ({ ...current, location: value }))} />
                      <Input label="Address" value={cinemaForm.address} onChange={(value) => setCinemaForm((current) => ({ ...current, address: value }))} />
                    </AdminForm>
                  ) : null}

                  {activePanel === "hall" ? (
                    <AdminForm onSubmit={createHall} buttonLabel="Add hall" icon={<Theater size={17} aria-hidden />}>
                      <Select
                        label="Cinema"
                        value={hallForm.cinemaId}
                        onChange={(value) => {
                          setHallForm((current) => ({ ...current, cinemaId: value }));
                          loadHalls(value);
                        }}
                        options={cinemas.map((cinema) => [cinema.id, cinema.name])}
                      />
                      <div className="grid gap-3 sm:grid-cols-2">
                        <Input label="Hall name" value={hallForm.name} onChange={(value) => setHallForm((current) => ({ ...current, name: value }))} />
                        <Input label="Type" value={hallForm.type} onChange={(value) => setHallForm((current) => ({ ...current, type: value }))} />
                      </div>
                      <div className="grid gap-3 sm:grid-cols-2">
                        <Input label="Rows" value={hallForm.totalRows} onChange={(value) => setHallForm((current) => ({ ...current, totalRows: value }))} />
                        <Input label="Columns" value={hallForm.totalColumns} onChange={(value) => setHallForm((current) => ({ ...current, totalColumns: value }))} />
                      </div>
                    </AdminForm>
                  ) : null}

                  {activePanel === "showtime" ? (
                    <AdminForm onSubmit={createShowtime} buttonLabel="Add showtime" icon={<CalendarPlus size={17} aria-hidden />}>
                      <Select label="Movie" value={showtimeForm.movieId} onChange={(value) => setShowtimeForm((current) => ({ ...current, movieId: value }))} options={movies.map((movie) => [movie.id, movie.title])} />
                      <Select label="Hall" value={showtimeForm.hallId} onChange={(value) => setShowtimeForm((current) => ({ ...current, hallId: value }))} options={halls.map((hall) => [hall.id, `${hall.name} - ${hall.type}`])} />
                      <div className="grid gap-3 sm:grid-cols-2">
                        <Input label="Start time" type="datetime-local" value={showtimeForm.startTime} onChange={(value) => setShowtimeForm((current) => ({ ...current, startTime: value }))} />
                        <Input label="Base price" value={showtimeForm.basePrice} onChange={(value) => setShowtimeForm((current) => ({ ...current, basePrice: value }))} />
                      </div>
                    </AdminForm>
                  ) : null}
                </div>
              </div>

              <div className="mt-8 grid gap-6 xl:grid-cols-[1fr_420px]">
                <section className="rounded-lg border border-line bg-panel/70 p-5">
                  <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-center">
                    <div>
                      <p className="text-sm text-accent">Live schedule</p>
                      <h2 className="mt-1 text-2xl font-semibold">Recent showtimes</h2>
                    </div>
                    <span className="flex items-center gap-2 rounded-md border border-line bg-background px-3 py-2 text-sm text-muted">
                      <Search size={16} aria-hidden />
                      {showtimes.length} sessions
                    </span>
                  </div>

                  <div className="mt-5 grid gap-3">
                    {showtimes.slice(0, 8).map((showtime) => (
                      <div key={showtime.id} className="grid gap-3 rounded-md border border-line bg-background p-4 md:grid-cols-[1fr_auto] md:items-center">
                        <div>
                          <p className="font-semibold">{showtime.movieTitle}</p>
                          <p className="mt-1 text-sm text-muted">
                            {showtime.cinemaName}, {showtime.hallName} - {new Date(showtime.startTime).toLocaleString()}
                          </p>
                        </div>
                        <div className="flex items-center gap-3">
                          <span className="font-mono text-sm text-accent">${Number(showtime.basePrice).toFixed(2)}</span>
                          <StatusBadge status={showtime.status} />
                        </div>
                      </div>
                    ))}
                    {showtimes.length === 0 ? <EmptyState label="No showtimes yet" helper="Create your first showtime from the console above." /> : null}
                  </div>
                </section>

                <section className="rounded-lg border border-line bg-panel/70 p-5">
                  <p className="text-sm text-accent">Current inventory</p>
                  <h2 className="mt-1 text-2xl font-semibold">Catalog and venues</h2>

                  <div className="mt-5 space-y-4">
                    <InventoryBlock title="Top movies" count={movies.length} icon={<Film size={17} aria-hidden />}>
                      {movies.slice(0, 5).map((movie) => (
                        <div key={movie.id} className="flex items-center justify-between gap-3 text-sm">
                          <span className="truncate text-foreground">{movie.title}</span>
                          <span className="shrink-0 text-muted">{movie.rating}</span>
                        </div>
                      ))}
                    </InventoryBlock>

                    <InventoryBlock title="Cinemas" count={cinemas.length} icon={<MapPinned size={17} aria-hidden />}>
                      {cinemas.slice(0, 5).map((cinema) => (
                        <div key={cinema.id} className="text-sm">
                          <p className="truncate text-foreground">{cinema.name}</p>
                          <p className="truncate text-muted">{cinema.city || cinema.location}</p>
                        </div>
                      ))}
                    </InventoryBlock>
                  </div>
                </section>
              </div>
            </>
          )}
        </div>
      </section>
    </AppShell>
  );
}

function AccessRequired() {
  return (
    <div className="mt-8 rounded-lg border border-line bg-panel p-6">
      <p className="text-sm text-danger">Admin access required</p>
      <h2 className="mt-2 text-3xl font-semibold">This dashboard is protected</h2>
      <p className="mt-3 max-w-2xl text-muted">Login with an admin or staff account to manage movies, cinemas, halls, seats, and showtimes.</p>
    </div>
  );
}

function SignalCard({ icon, label, value, detail }: { icon: React.ReactNode; label: string; value: string; detail: string }) {
  return (
    <div className="rounded-md border border-line bg-background/80 p-4">
      <div className="flex items-center gap-2 text-accent">
        {icon}
        <span className="text-sm text-muted">{label}</span>
      </div>
      <p className="mt-3 text-2xl font-semibold">{value}</p>
      <p className="mt-1 truncate text-sm text-muted">{detail}</p>
    </div>
  );
}

function MetricCard({ icon, label, value, detail }: { icon: React.ReactNode; label: string; value: React.ReactNode; detail: string }) {
  return (
    <div className="rounded-lg border border-line bg-panel p-4 transition duration-300 ease-[cubic-bezier(0.32,0.72,0,1)] hover:-translate-y-1 hover:border-accent/60">
      <div className="flex items-center justify-between gap-3">
        <p className="text-sm text-muted">{label}</p>
        <span className="grid size-9 place-items-center rounded-md bg-background text-accent">{icon}</span>
      </div>
      <p className="mt-3 font-mono text-3xl font-semibold text-accent">{value}</p>
      <p className="mt-1 text-sm text-muted">{detail}</p>
    </div>
  );
}

function AdminForm({ onSubmit, buttonLabel, icon, children }: { onSubmit: (event: FormEvent) => void; buttonLabel: string; icon: React.ReactNode; children: React.ReactNode }) {
  return (
    <form onSubmit={onSubmit}>
      <div className="grid gap-4">{children}</div>
      <button className="group mt-5 flex w-full items-center justify-between rounded-md bg-accent px-4 py-3 font-semibold text-background active:scale-[0.99]">
        <span className="flex items-center gap-2">
          {icon}
          {buttonLabel}
        </span>
        <span className="grid size-8 place-items-center rounded-md bg-background/15 transition duration-300 group-hover:translate-x-1">
          <ChevronRight size={17} aria-hidden />
        </span>
      </button>
    </form>
  );
}

function Input({ label, value, onChange, type = "text" }: { label: string; value: string; onChange: (value: string) => void; type?: string }) {
  return (
    <label className="block text-sm font-medium text-muted">
      {label}
      <input
        type={type}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="mt-2 w-full rounded-md border border-line bg-background px-3 py-3 text-foreground outline-none transition duration-300 focus:border-accent focus:ring-2 focus:ring-accent/20"
      />
    </label>
  );
}

function Select({ label, value, onChange, options }: { label: string; value: string; onChange: (value: string) => void; options: string[][] }) {
  return (
    <label className="block text-sm font-medium text-muted">
      {label}
      <select
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="mt-2 w-full rounded-md border border-line bg-background px-3 py-3 text-foreground outline-none transition duration-300 focus:border-accent focus:ring-2 focus:ring-accent/20"
      >
        <option value="">Select</option>
        {options.map(([id, name]) => (
          <option key={id} value={id}>
            {name}
          </option>
        ))}
      </select>
    </label>
  );
}

function InventoryBlock({ title, count, icon, children }: { title: string; count: number; icon: React.ReactNode; children: React.ReactNode }) {
  return (
    <div className="rounded-md border border-line bg-background p-4">
      <div className="mb-4 flex items-center justify-between gap-3">
        <div className="flex items-center gap-2 text-accent">
          {icon}
          <p className="font-semibold text-foreground">{title}</p>
        </div>
        <span className="font-mono text-sm text-muted">{count}</span>
      </div>
      <div className="space-y-3">{children}</div>
    </div>
  );
}

function EmptyState({ label, helper }: { label: string; helper: string }) {
  return (
    <div className="rounded-md border border-dashed border-line bg-background p-5 text-center">
      <p className="font-semibold">{label}</p>
      <p className="mt-1 text-sm text-muted">{helper}</p>
    </div>
  );
}

function formatTime(value: string) {
  return new Date(value).toLocaleTimeString("en-US", { hour: "numeric", minute: "2-digit" });
}
