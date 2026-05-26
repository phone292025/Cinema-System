"use client";

import {
  Activity,
  Armchair,
  BarChart3,
  Building2,
  CalendarClock,
  CalendarPlus,
  CheckCircle2,
  ChevronRight,
  CircleDollarSign,
  Clapperboard,
  Film,
  Gauge,
  LayoutDashboard,
  Loader2,
  MapPinned,
  Plus,
  Rows3,
  Search,
  Settings2,
  ShieldCheck,
  Sparkles,
  Theater,
  Ticket,
  TicketCheck,
  WalletCards,
} from "lucide-react";
import { FormEvent, ReactNode, useCallback, useEffect, useMemo, useState, useSyncExternalStore } from "react";

import { AppShell } from "@/components/AppShell";
import { StatusBadge } from "@/components/StatusBadge";
import { apiFetch, getStoredUser, subscribeToAuthChanges } from "@/lib/api";
import type { AuditLog, Cinema, Dashboard, Movie, Showtime } from "@/lib/types";

type Hall = {
  id: string;
  cinemaId: string;
  name: string;
  type: string;
  totalRows: number;
  totalColumns: number;
};

type AdminSection = "dashboard" | "tickets" | "movies" | "cinemas" | "halls" | "showtimes" | "audit";

const sections: Array<{
  key: AdminSection;
  label: string;
  eyebrow: string;
  helper: string;
  icon: ReactNode;
}> = [
  {
    key: "dashboard",
    label: "Dashboard",
    eyebrow: "Control room",
    helper: "Operational totals, health, and setup progress.",
    icon: <LayoutDashboard size={18} aria-hidden />,
  },
  {
    key: "tickets",
    label: "Ticket sales",
    eyebrow: "Revenue desk",
    helper: "Paid bookings, revenue, and session demand.",
    icon: <TicketCheck size={18} aria-hidden />,
  },
  {
    key: "movies",
    label: "Movie management",
    eyebrow: "Catalog",
    helper: "Poster, title, description, duration, genre, language, rating, date, and status.",
    icon: <Film size={18} aria-hidden />,
  },
  {
    key: "cinemas",
    label: "Cinema management",
    eyebrow: "Branches",
    helper: "Branch name, location, address, and city.",
    icon: <Building2 size={18} aria-hidden />,
  },
  {
    key: "halls",
    label: "Halls and seats",
    eyebrow: "Seat layout",
    helper: "Hall type, rows, columns, and automatic seat generation.",
    icon: <Rows3 size={18} aria-hidden />,
  },
  {
    key: "showtimes",
    label: "Showtime management",
    eyebrow: "Scheduling",
    helper: "Schedule movies into halls with start time and base price.",
    icon: <CalendarPlus size={18} aria-hidden />,
  },
  {
    key: "audit",
    label: "Audit logs",
    eyebrow: "Safety trail",
    helper: "Review payment, booking, ticket, and admin actions.",
    icon: <ShieldCheck size={18} aria-hidden />,
  },
];

export default function AdminPage() {
  const user = useSyncExternalStore(subscribeToAuthChanges, getStoredUser, () => null);
  const [activeSection, setActiveSection] = useState<AdminSection>("dashboard");
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

  const [movieForm, setMovieForm] = useState({
    title: "",
    description: "",
    durationMinutes: "120",
    genre: "Drama",
    language: "English",
    rating: "PG-13",
    imdbRating: "8.1",
    posterUrl: "/posters/shawshank-redemption.jpg",
    releaseDate: "2026-04-24",
    status: "NOW_SHOWING",
  });
  const [cinemaForm, setCinemaForm] = useState({ name: "", location: "", address: "", city: "" });
  const [hallForm, setHallForm] = useState({
    cinemaId: "",
    name: "Hall 1",
    type: "Standard",
    totalRows: "6",
    totalColumns: "8",
  });
  const [showtimeForm, setShowtimeForm] = useState({
    movieId: "",
    hallId: "",
    startTime: "2026-04-25T18:00",
    basePrice: "15.00",
  });

  const canManage = user?.role === "ADMIN";

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
      user?.role === "ADMIN" ? apiFetch<AuditLog[]>("/admin/audit-logs") : Promise.resolve([]),
    ])
      .then(([dashboardResponse, movieResponse, cinemaResponse, showtimeResponse, auditResponse]) => {
        setDashboard(dashboardResponse);
        setMovies(movieResponse);
        setCinemas(cinemaResponse);
        setShowtimes(showtimeResponse);
        setAuditLogs(auditResponse);

        const selectedCinemaId = hallForm.cinemaId || cinemaResponse[0]?.id || "";
        if (selectedCinemaId) {
          setHallForm((current) => ({ ...current, cinemaId: current.cinemaId || selectedCinemaId }));
          loadHalls(selectedCinemaId);
        }

        if (movieResponse[0]) {
          setShowtimeForm((current) => ({ ...current, movieId: current.movieId || movieResponse[0].id }));
        }
      })
      .catch((err) => setError(err instanceof Error ? err.message : "Dashboard request failed."))
      .finally(() => setIsLoading(false));
  }, [hallForm.cinemaId, loadHalls, user?.role]);

  useEffect(() => {
    if (!canManage) return undefined;
    const timer = window.setTimeout(() => {
      void load();
    }, 0);
    return () => window.clearTimeout(timer);
  }, [canManage, load]);

  const sortedShowtimes = useMemo(
    () => showtimes.slice().sort((a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime()),
    [showtimes],
  );

  const nextShowtime = sortedShowtimes[0];
  const activeMeta = sections.find((section) => section.key === activeSection) ?? sections[0];
  const revenue = Number(dashboard?.revenue ?? 0);
  const paidBookings = Number(dashboard?.paidBookings ?? 0);
  const averageTicket = paidBookings > 0 ? revenue / paidBookings : 0;
  const seatPreviewRows = clamp(Number(hallForm.totalRows) || 0, 1, 9);
  const seatPreviewColumns = clamp(Number(hallForm.totalColumns) || 0, 1, 12);
  const generatedSeatCount = Math.max(0, (Number(hallForm.totalRows) || 0) * (Number(hallForm.totalColumns) || 0));

  async function createMovie(event: FormEvent) {
    event.preventDefault();
    await run("Movie added to the catalog.", () =>
      apiFetch<Movie>("/admin/movies", {
        method: "POST",
        body: JSON.stringify({
          ...movieForm,
          durationMinutes: Number(movieForm.durationMinutes),
          imdbRating: movieForm.imdbRating ? Number(movieForm.imdbRating) : null,
        }),
      }),
    );
  }

  async function createCinema(event: FormEvent) {
    event.preventDefault();
    await run("Cinema branch created.", () =>
      apiFetch<Cinema>("/admin/cinemas", { method: "POST", body: JSON.stringify(cinemaForm) }),
    );
  }

  async function createHall(event: FormEvent) {
    event.preventDefault();
    await run(`${generatedSeatCount} seats generated for ${hallForm.name}.`, () =>
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

  async function run<T>(message: string, action: () => Promise<T>) {
    setError("");
    setSuccess("");
    setIsSaving(true);
    try {
      await action();
      setSuccess(message);
      load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Admin action failed.");
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <AppShell>
      <section className="relative min-h-[calc(100dvh-65px)] overflow-hidden bg-background px-4 py-5 sm:px-6 lg:py-8">
        <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_12%_4%,rgba(244,184,96,0.14),transparent_28%),radial-gradient(circle_at_86%_18%,rgba(68,194,141,0.08),transparent_28%)]" />
        <div className="pointer-events-none absolute inset-x-0 top-0 h-px bg-accent/30" />

        <div className="relative mx-auto max-w-7xl">
          <header className="grid gap-4 xl:grid-cols-[1fr_420px]">
            <div className="rounded-lg border border-line bg-panel/65 p-5 shadow-[0_24px_80px_rgba(0,0,0,0.24)] sm:p-7">
              <div className="flex flex-wrap items-center gap-3">
                <span className="flex items-center gap-2 rounded-md border border-accent/30 bg-accent/10 px-3 py-2 font-mono text-xs uppercase text-accent">
                  <ShieldCheck size={15} aria-hidden />
                  Admin suite
                </span>
                <span className="rounded-md border border-line bg-background px-3 py-2 text-sm text-muted">
                  {canManage ? `${user?.role} access` : "Login required"}
                </span>
              </div>
              <h1 className="mt-5 max-w-4xl text-4xl font-semibold leading-none text-foreground sm:text-5xl lg:text-6xl">
                Cinema operations, split into the jobs you actually do.
              </h1>
              <p className="mt-5 max-w-3xl text-base leading-7 text-muted sm:text-lg">
                Control ticket sales, movie catalog, cinema branches, halls, automatic seat layouts, and showtime scheduling from one focused admin workspace.
              </p>
            </div>

            <aside className="rounded-lg border border-line bg-background/82 p-5 shadow-[0_24px_80px_rgba(0,0,0,0.24)]">
              <div className="flex items-start justify-between gap-4">
                <div>
                  <p className="font-mono text-xs uppercase text-accent">Current workspace</p>
                  <h2 className="mt-3 text-3xl font-semibold">{activeMeta.label}</h2>
                  <p className="mt-3 text-sm leading-6 text-muted">{activeMeta.helper}</p>
                </div>
                <span className="grid size-12 place-items-center rounded-lg border border-line bg-panel text-accent">
                  {activeMeta.icon}
                </span>
              </div>
              <button
                type="button"
                onClick={() => canManage && load()}
                disabled={!canManage || isLoading}
                className="mt-5 flex w-full items-center justify-center gap-2 rounded-md bg-accent px-4 py-3 font-semibold text-background transition active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-50"
              >
                {isLoading ? <Loader2 size={17} className="animate-spin" aria-hidden /> : <Activity size={17} aria-hidden />}
                {isLoading ? "Refreshing" : "Refresh data"}
              </button>
            </aside>
          </header>

          {!canManage ? (
            <AccessRequired />
          ) : (
            <>
              <nav className="mt-5 overflow-x-auto cinema-scrollbar-none" aria-label="Admin workspaces">
                <div className="flex min-w-max gap-3 pb-1">
                  {sections.map((section) => (
                    <button
                      key={section.key}
                      type="button"
                      onClick={() => setActiveSection(section.key)}
                      className={`group w-[220px] rounded-lg border p-4 text-left transition duration-300 active:scale-[0.98] ${
                        activeSection === section.key
                          ? "border-accent bg-accent/12 text-foreground"
                          : "border-line bg-panel/60 text-muted hover:border-accent/60 hover:text-foreground"
                      }`}
                    >
                      <span className="flex items-center justify-between gap-3">
                        <span className="grid size-10 place-items-center rounded-md bg-background text-accent">{section.icon}</span>
                        <ChevronRight size={18} className="transition duration-300 group-hover:translate-x-1" aria-hidden />
                      </span>
                      <span className="mt-4 block font-mono text-xs uppercase text-accent">{section.eyebrow}</span>
                      <span className="mt-1 block text-lg font-semibold">{section.label}</span>
                    </button>
                  ))}
                </div>
              </nav>

              <StatusStrip error={error} success={success} />

              <section className="mt-6">
                {activeSection === "dashboard" ? (
                  <DashboardWorkspace
                    dashboard={dashboard}
                    movies={movies}
                    cinemas={cinemas}
                    halls={halls}
                    showtimes={sortedShowtimes}
                    nextShowtime={nextShowtime}
                    averageTicket={averageTicket}
                  />
                ) : null}

                {activeSection === "tickets" ? (
                  <TicketSalesWorkspace
                    dashboard={dashboard}
                    showtimes={sortedShowtimes}
                    revenue={revenue}
                    paidBookings={paidBookings}
                    averageTicket={averageTicket}
                  />
                ) : null}

                {activeSection === "movies" ? (
                  <MovieWorkspace
                    movies={movies}
                    movieForm={movieForm}
                    setMovieForm={setMovieForm}
                    onSubmit={createMovie}
                    isSaving={isSaving}
                  />
                ) : null}

                {activeSection === "cinemas" ? (
                  <CinemaWorkspace
                    cinemas={cinemas}
                    cinemaForm={cinemaForm}
                    setCinemaForm={setCinemaForm}
                    onSubmit={createCinema}
                    isSaving={isSaving}
                  />
                ) : null}

                {activeSection === "halls" ? (
                  <HallWorkspace
                    cinemas={cinemas}
                    halls={halls}
                    hallForm={hallForm}
                    setHallForm={setHallForm}
                    loadHalls={loadHalls}
                    onSubmit={createHall}
                    isSaving={isSaving}
                    seatPreviewRows={seatPreviewRows}
                    seatPreviewColumns={seatPreviewColumns}
                    generatedSeatCount={generatedSeatCount}
                  />
                ) : null}

                {activeSection === "showtimes" ? (
                  <ShowtimeWorkspace
                    movies={movies}
                    cinemas={cinemas}
                    halls={halls}
                    showtimes={sortedShowtimes}
                    hallForm={hallForm}
                    setHallForm={setHallForm}
                    loadHalls={loadHalls}
                    showtimeForm={showtimeForm}
                    setShowtimeForm={setShowtimeForm}
                    onSubmit={createShowtime}
                    isSaving={isSaving}
                  />
                ) : null}

                {activeSection === "audit" ? <AuditWorkspace logs={auditLogs} /> : null}
              </section>
            </>
          )}
        </div>
      </section>
    </AppShell>
  );
}

function DashboardWorkspace({
  dashboard,
  movies,
  cinemas,
  halls,
  showtimes,
  nextShowtime,
  averageTicket,
}: {
  dashboard: Dashboard | null;
  movies: Movie[];
  cinemas: Cinema[];
  halls: Hall[];
  showtimes: Showtime[];
  nextShowtime?: Showtime;
  averageTicket: number;
}) {
  const setupItems = [
    { label: "Movies", complete: movies.length > 0, helper: `${movies.length} catalog records` },
    { label: "Cinemas", complete: cinemas.length > 0, helper: `${cinemas.length} branches` },
    { label: "Halls", complete: halls.length > 0, helper: `${halls.length} halls in selected branch` },
    { label: "Showtimes", complete: showtimes.length > 0, helper: `${showtimes.length} scheduled sessions` },
  ];

  return (
    <div className="grid gap-6 xl:grid-cols-[1fr_380px]">
      <div className="space-y-6">
        <MetricsGrid dashboard={dashboard} averageTicket={averageTicket} />
        <Panel>
          <PanelHeader eyebrow="Today" title="Operations dashboard" helper="A fast read of what is ready and what still needs setup." icon={<Gauge size={19} aria-hidden />} />
          <div className="mt-5 grid gap-3 md:grid-cols-2">
            <SignalBlock icon={<CalendarClock size={18} aria-hidden />} label="Next showtime" value={nextShowtime ? formatTime(nextShowtime.startTime) : "No queue"} detail={nextShowtime?.movieTitle ?? "Create a schedule"} />
            <SignalBlock icon={<Ticket size={18} aria-hidden />} label="Bookings" value={String(dashboard?.bookings ?? 0)} detail={`${dashboard?.paidBookings ?? 0} paid confirmations`} />
            <SignalBlock icon={<Clapperboard size={18} aria-hidden />} label="Movie catalog" value={String(dashboard?.movies ?? movies.length)} detail="Now showing and coming soon" />
            <SignalBlock icon={<Theater size={18} aria-hidden />} label="Showtimes" value={String(dashboard?.showtimes ?? showtimes.length)} detail="Sessions available to customers" />
          </div>
        </Panel>
      </div>

      <Panel>
        <PanelHeader eyebrow="Setup" title="Admin checklist" helper="Keep every booking dependency ready before sales open." icon={<Settings2 size={19} aria-hidden />} />
        <div className="mt-5 space-y-3">
          {setupItems.map((item) => (
            <div key={item.label} className="flex items-center justify-between gap-4 rounded-md border border-line bg-background p-4">
              <div>
                <p className="font-semibold">{item.label}</p>
                <p className="mt-1 text-sm text-muted">{item.helper}</p>
              </div>
              <span className={`grid size-9 place-items-center rounded-md ${item.complete ? "bg-success/15 text-success" : "bg-accent/12 text-accent"}`}>
                {item.complete ? <CheckCircle2 size={18} aria-hidden /> : <Plus size={18} aria-hidden />}
              </span>
            </div>
          ))}
        </div>
      </Panel>
    </div>
  );
}

function TicketSalesWorkspace({
  dashboard,
  showtimes,
  revenue,
  paidBookings,
  averageTicket,
}: {
  dashboard: Dashboard | null;
  showtimes: Showtime[];
  revenue: number;
  paidBookings: number;
  averageTicket: number;
}) {
  return (
    <div className="grid gap-6 xl:grid-cols-[380px_1fr]">
      <Panel>
        <PanelHeader eyebrow="Ticket sales" title="Revenue desk" helper="Sales data is separated from setup work so the front counter stays focused." icon={<WalletCards size={19} aria-hidden />} />
        <div className="mt-6 space-y-3">
          <RevenueLine label="Gross revenue" value={formatMoney(revenue)} strong />
          <RevenueLine label="Paid bookings" value={paidBookings.toString()} />
          <RevenueLine label="Average ticket" value={formatMoney(averageTicket)} />
          <RevenueLine label="All bookings" value={String(dashboard?.bookings ?? 0)} />
        </div>
      </Panel>

      <Panel>
        <PanelHeader eyebrow="Demand" title="Session sales board" helper="Use this to scan which sessions are on sale and ready for customer booking." icon={<BarChart3 size={19} aria-hidden />} />
        <div className="mt-5 grid gap-3">
          {showtimes.slice(0, 10).map((showtime) => (
            <div key={showtime.id} className="grid gap-4 rounded-md border border-line bg-background p-4 md:grid-cols-[1fr_auto] md:items-center">
              <div>
                <p className="text-lg font-semibold">{showtime.movieTitle}</p>
                <p className="mt-1 text-sm text-muted">
                  {showtime.cinemaName}, {showtime.hallName} · {formatDateTime(showtime.startTime)}
                </p>
              </div>
              <div className="flex items-center gap-3 md:justify-end">
                <span className="font-mono text-sm text-accent">{formatMoney(Number(showtime.basePrice))} base</span>
                <StatusBadge status={showtime.status} />
              </div>
            </div>
          ))}
          {showtimes.length === 0 ? <EmptyState label="No sessions on sale" helper="Create showtimes before tracking ticket sales." /> : null}
        </div>
      </Panel>
    </div>
  );
}

function AuditWorkspace({ logs }: { logs: AuditLog[] }) {
  return (
    <Panel>
      <PanelHeader eyebrow="Audit trail" title="System activity" helper="Append-only records from booking, payment, ticket, and admin workflows." icon={<ShieldCheck size={19} aria-hidden />} />
      <div className="mt-5 overflow-x-auto cinema-scrollbar-none">
        <table className="min-w-full text-left text-sm">
          <thead className="font-mono text-xs uppercase text-muted">
            <tr className="border-b border-line">
              <th className="py-3 pr-4">Time</th>
              <th className="py-3 pr-4">Action</th>
              <th className="py-3 pr-4">Actor</th>
              <th className="py-3 pr-4">Entity</th>
              <th className="py-3">Value</th>
            </tr>
          </thead>
          <tbody>
            {logs.map((log) => (
              <tr key={log.id} className="border-b border-line/70">
                <td className="py-3 pr-4 text-muted">{formatDateTime(log.createdAt)}</td>
                <td className="py-3 pr-4 font-semibold">{log.action.replaceAll("_", " ")}</td>
                <td className="py-3 pr-4 text-muted">{log.actorRole ?? "SYSTEM"}</td>
                <td className="py-3 pr-4 text-muted">
                  {log.entityType}
                  {log.entityId ? ` ${log.entityId.slice(0, 8)}` : ""}
                </td>
                <td className="max-w-[280px] truncate py-3 text-muted">{log.newValue ?? log.oldValue ?? "-"}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {logs.length === 0 ? <EmptyState label="No audit rows yet" helper="Important actions will appear here once the system is used." /> : null}
      </div>
    </Panel>
  );
}

function MovieWorkspace({
  movies,
  movieForm,
  setMovieForm,
  onSubmit,
  isSaving,
}: {
  movies: Movie[];
  movieForm: {
    title: string;
    description: string;
    durationMinutes: string;
    genre: string;
    language: string;
    rating: string;
    imdbRating: string;
    posterUrl: string;
    releaseDate: string;
    status: string;
  };
  setMovieForm: React.Dispatch<React.SetStateAction<{
    title: string;
    description: string;
    durationMinutes: string;
    genre: string;
    language: string;
    rating: string;
    imdbRating: string;
    posterUrl: string;
    releaseDate: string;
    status: string;
  }>>;
  onSubmit: (event: FormEvent) => void;
  isSaving: boolean;
}) {
  return (
    <WorkspaceGrid>
      <Panel>
        <PanelHeader eyebrow="Add movie" title="Movie management" helper="Create the customer-facing movie card and detail page data." icon={<Film size={19} aria-hidden />} />
        <AdminForm onSubmit={onSubmit} buttonLabel="Add movie" isSaving={isSaving} icon={<Plus size={17} aria-hidden />}>
          <div className="grid gap-3 lg:grid-cols-2">
            <Input label="Poster URL" value={movieForm.posterUrl} onChange={(value) => setMovieForm((current) => ({ ...current, posterUrl: value }))} />
            <Input label="Title" value={movieForm.title} onChange={(value) => setMovieForm((current) => ({ ...current, title: value }))} />
          </div>
          <Textarea label="Description" value={movieForm.description} onChange={(value) => setMovieForm((current) => ({ ...current, description: value }))} />
          <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
            <Input label="Duration minutes" type="number" value={movieForm.durationMinutes} onChange={(value) => setMovieForm((current) => ({ ...current, durationMinutes: value }))} />
            <Input label="Genre" value={movieForm.genre} onChange={(value) => setMovieForm((current) => ({ ...current, genre: value }))} />
            <Input label="Language" value={movieForm.language} onChange={(value) => setMovieForm((current) => ({ ...current, language: value }))} />
            <Input label="Age rating" value={movieForm.rating} onChange={(value) => setMovieForm((current) => ({ ...current, rating: value }))} />
          </div>
          <div className="grid gap-3 sm:grid-cols-3">
            <Input label="IMDb rating" type="number" value={movieForm.imdbRating} onChange={(value) => setMovieForm((current) => ({ ...current, imdbRating: value }))} />
            <Input label="Release date" type="date" value={movieForm.releaseDate} onChange={(value) => setMovieForm((current) => ({ ...current, releaseDate: value }))} />
            <Select
              label="Status"
              value={movieForm.status}
              onChange={(value) => setMovieForm((current) => ({ ...current, status: value }))}
              options={[
                { value: "NOW_SHOWING", label: "Now showing" },
                { value: "COMING_SOON", label: "Coming soon" },
                { value: "ARCHIVED", label: "Archived" },
              ]}
            />
          </div>
        </AdminForm>
      </Panel>

      <InventoryPanel title="Current movies" eyebrow="Catalog" count={movies.length} icon={<Search size={18} aria-hidden />}>
        {movies.map((movie) => (
          <InventoryRow key={movie.id} title={movie.title} meta={`${movie.genre} · ${movie.rating} · ${movie.durationMinutes} min`} detail={movie.status} />
        ))}
      </InventoryPanel>
    </WorkspaceGrid>
  );
}

function CinemaWorkspace({
  cinemas,
  cinemaForm,
  setCinemaForm,
  onSubmit,
  isSaving,
}: {
  cinemas: Cinema[];
  cinemaForm: { name: string; location: string; address: string; city: string };
  setCinemaForm: React.Dispatch<React.SetStateAction<{ name: string; location: string; address: string; city: string }>>;
  onSubmit: (event: FormEvent) => void;
  isSaving: boolean;
}) {
  return (
    <WorkspaceGrid>
      <Panel>
        <PanelHeader eyebrow="Add branch" title="Cinema management" helper="Create cinema branches used by halls and showtimes." icon={<Building2 size={19} aria-hidden />} />
        <AdminForm onSubmit={onSubmit} buttonLabel="Add cinema branch" isSaving={isSaving} icon={<Plus size={17} aria-hidden />}>
          <div className="grid gap-3 lg:grid-cols-2">
            <Input label="Branch name" value={cinemaForm.name} onChange={(value) => setCinemaForm((current) => ({ ...current, name: value }))} />
            <Input label="City" value={cinemaForm.city} onChange={(value) => setCinemaForm((current) => ({ ...current, city: value }))} />
          </div>
          <Input label="Location" value={cinemaForm.location} onChange={(value) => setCinemaForm((current) => ({ ...current, location: value }))} />
          <Textarea label="Address" value={cinemaForm.address} onChange={(value) => setCinemaForm((current) => ({ ...current, address: value }))} />
        </AdminForm>
      </Panel>

      <InventoryPanel title="Cinema branches" eyebrow="Venues" count={cinemas.length} icon={<MapPinned size={18} aria-hidden />}>
        {cinemas.map((cinema) => (
          <InventoryRow key={cinema.id} title={cinema.name} meta={`${cinema.location} · ${cinema.city}`} detail={cinema.address} />
        ))}
      </InventoryPanel>
    </WorkspaceGrid>
  );
}

function HallWorkspace({
  cinemas,
  halls,
  hallForm,
  setHallForm,
  loadHalls,
  onSubmit,
  isSaving,
  seatPreviewRows,
  seatPreviewColumns,
  generatedSeatCount,
}: {
  cinemas: Cinema[];
  halls: Hall[];
  hallForm: { cinemaId: string; name: string; type: string; totalRows: string; totalColumns: string };
  setHallForm: React.Dispatch<React.SetStateAction<{ cinemaId: string; name: string; type: string; totalRows: string; totalColumns: string }>>;
  loadHalls: (cinemaId: string) => void;
  onSubmit: (event: FormEvent) => void;
  isSaving: boolean;
  seatPreviewRows: number;
  seatPreviewColumns: number;
  generatedSeatCount: number;
}) {
  return (
    <WorkspaceGrid>
      <Panel>
        <PanelHeader eyebrow="Create hall" title="Hall and seat management" helper="Create the hall, then generate its default seat layout automatically." icon={<Rows3 size={19} aria-hidden />} />
        <AdminForm onSubmit={onSubmit} buttonLabel="Create hall and seats" isSaving={isSaving} icon={<Theater size={17} aria-hidden />}>
          <Select
            label="Cinema branch"
            value={hallForm.cinemaId}
            onChange={(value) => {
              setHallForm((current) => ({ ...current, cinemaId: value }));
              loadHalls(value);
            }}
            options={cinemas.map((cinema) => ({ value: cinema.id, label: cinema.name }))}
          />
          <div className="grid gap-3 sm:grid-cols-2">
            <Input label="Hall name" value={hallForm.name} onChange={(value) => setHallForm((current) => ({ ...current, name: value }))} />
            <Input label="Hall type" value={hallForm.type} onChange={(value) => setHallForm((current) => ({ ...current, type: value }))} />
          </div>
          <div className="grid gap-3 sm:grid-cols-2">
            <Input label="Rows" type="number" value={hallForm.totalRows} onChange={(value) => setHallForm((current) => ({ ...current, totalRows: value }))} />
            <Input label="Columns" type="number" value={hallForm.totalColumns} onChange={(value) => setHallForm((current) => ({ ...current, totalColumns: value }))} />
          </div>
        </AdminForm>

        <div className="mt-5 rounded-lg border border-line bg-background p-4">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <p className="font-semibold">Automatic seat preview</p>
              <p className="mt-1 text-sm text-muted">{generatedSeatCount} regular seats will be created.</p>
            </div>
            <span className="rounded-md bg-accent/12 px-3 py-2 font-mono text-sm text-accent">
              {hallForm.totalRows} × {hallForm.totalColumns}
            </span>
          </div>
          <SeatPreview rows={seatPreviewRows} columns={seatPreviewColumns} />
        </div>
      </Panel>

      <InventoryPanel title="Halls in selected branch" eyebrow="Layouts" count={halls.length} icon={<Armchair size={18} aria-hidden />}>
        {halls.map((hall) => (
          <InventoryRow key={hall.id} title={`${hall.name} · ${hall.type}`} meta={`${hall.totalRows} rows × ${hall.totalColumns} columns`} detail={`${hall.totalRows * hall.totalColumns} seats`} />
        ))}
      </InventoryPanel>
    </WorkspaceGrid>
  );
}

function ShowtimeWorkspace({
  movies,
  cinemas,
  halls,
  showtimes,
  hallForm,
  setHallForm,
  loadHalls,
  showtimeForm,
  setShowtimeForm,
  onSubmit,
  isSaving,
}: {
  movies: Movie[];
  cinemas: Cinema[];
  halls: Hall[];
  showtimes: Showtime[];
  hallForm: { cinemaId: string; name: string; type: string; totalRows: string; totalColumns: string };
  setHallForm: React.Dispatch<React.SetStateAction<{ cinemaId: string; name: string; type: string; totalRows: string; totalColumns: string }>>;
  loadHalls: (cinemaId: string) => void;
  showtimeForm: { movieId: string; hallId: string; startTime: string; basePrice: string };
  setShowtimeForm: React.Dispatch<React.SetStateAction<{ movieId: string; hallId: string; startTime: string; basePrice: string }>>;
  onSubmit: (event: FormEvent) => void;
  isSaving: boolean;
}) {
  return (
    <WorkspaceGrid>
      <Panel>
        <PanelHeader eyebrow="Create session" title="Showtime management" helper="Pick a movie, select a cinema hall, choose start time, and set the base ticket price." icon={<CalendarPlus size={19} aria-hidden />} />
        <AdminForm onSubmit={onSubmit} buttonLabel="Schedule showtime" isSaving={isSaving} icon={<CalendarPlus size={17} aria-hidden />}>
          <Select label="Movie" value={showtimeForm.movieId} onChange={(value) => setShowtimeForm((current) => ({ ...current, movieId: value }))} options={movies.map((movie) => ({ value: movie.id, label: movie.title }))} />
          <Select
            label="Cinema branch"
            value={hallForm.cinemaId}
            onChange={(value) => {
              setHallForm((current) => ({ ...current, cinemaId: value }));
              loadHalls(value);
            }}
            options={cinemas.map((cinema) => ({ value: cinema.id, label: cinema.name }))}
          />
          <Select label="Hall" value={showtimeForm.hallId} onChange={(value) => setShowtimeForm((current) => ({ ...current, hallId: value }))} options={halls.map((hall) => ({ value: hall.id, label: `${hall.name} · ${hall.type}` }))} />
          <div className="grid gap-3 sm:grid-cols-2">
            <Input label="Start time" type="datetime-local" value={showtimeForm.startTime} onChange={(value) => setShowtimeForm((current) => ({ ...current, startTime: value }))} />
            <Input label="Base price" type="number" value={showtimeForm.basePrice} onChange={(value) => setShowtimeForm((current) => ({ ...current, basePrice: value }))} />
          </div>
        </AdminForm>
      </Panel>

      <InventoryPanel title="Scheduled sessions" eyebrow="Calendar" count={showtimes.length} icon={<CalendarClock size={18} aria-hidden />}>
        {showtimes.map((showtime) => (
          <InventoryRow key={showtime.id} title={showtime.movieTitle} meta={`${showtime.cinemaName}, ${showtime.hallName}`} detail={`${formatDateTime(showtime.startTime)} · ${formatMoney(Number(showtime.basePrice))}`} />
        ))}
      </InventoryPanel>
    </WorkspaceGrid>
  );
}

function MetricsGrid({ dashboard, averageTicket }: { dashboard: Dashboard | null; averageTicket: number }) {
  return (
    <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-5">
      <MetricCard icon={<Film size={18} aria-hidden />} label="Movies" value={dashboard?.movies ?? 0} detail="Catalog records" />
      <MetricCard icon={<CalendarClock size={18} aria-hidden />} label="Showtimes" value={dashboard?.showtimes ?? 0} detail="Scheduled sessions" />
      <MetricCard icon={<Ticket size={18} aria-hidden />} label="Bookings" value={dashboard?.bookings ?? 0} detail="All reservations" />
      <MetricCard icon={<CheckCircle2 size={18} aria-hidden />} label="Paid" value={dashboard?.paidBookings ?? 0} detail="Confirmed orders" />
      <MetricCard icon={<CircleDollarSign size={18} aria-hidden />} label="Avg ticket" value={formatMoney(averageTicket)} detail="Paid revenue average" />
    </div>
  );
}

function AccessRequired() {
  return (
    <div className="mt-6 rounded-lg border border-danger/40 bg-danger/10 p-6">
      <p className="font-mono text-xs uppercase text-danger">Admin access required</p>
      <h2 className="mt-3 text-3xl font-semibold">Login as an admin or staff user</h2>
      <p className="mt-3 max-w-2xl text-muted">This area manages ticket sales, movies, branches, halls, seat layouts, and showtimes.</p>
    </div>
  );
}

function WorkspaceGrid({ children }: { children: ReactNode }) {
  return <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_430px]">{children}</div>;
}

function Panel({ children }: { children: ReactNode }) {
  return <section className="rounded-lg border border-line bg-panel/72 p-5 shadow-[0_24px_80px_rgba(0,0,0,0.22)] sm:p-6">{children}</section>;
}

function PanelHeader({ eyebrow, title, helper, icon }: { eyebrow: string; title: string; helper: string; icon: ReactNode }) {
  return (
    <div className="flex items-start justify-between gap-4">
      <div>
        <p className="font-mono text-xs uppercase text-accent">{eyebrow}</p>
        <h2 className="mt-2 text-2xl font-semibold sm:text-3xl">{title}</h2>
        <p className="mt-2 max-w-2xl text-sm leading-6 text-muted">{helper}</p>
      </div>
      <span className="grid size-11 shrink-0 place-items-center rounded-md border border-line bg-background text-accent">{icon}</span>
    </div>
  );
}

function StatusStrip({ error, success }: { error: string; success: string }) {
  if (!error && !success) return null;
  return (
    <div className={`mt-5 rounded-md border p-4 ${error ? "border-danger/40 bg-danger/10 text-danger" : "border-success/40 bg-success/10 text-success"}`}>
      {error || success}
    </div>
  );
}

function MetricCard({ icon, label, value, detail }: { icon: ReactNode; label: string; value: ReactNode; detail: string }) {
  return (
    <article className="rounded-lg border border-line bg-panel/80 p-4 transition duration-300 hover:-translate-y-1 hover:border-accent/60">
      <div className="flex items-center justify-between gap-3">
        <p className="text-sm text-muted">{label}</p>
        <span className="grid size-9 place-items-center rounded-md bg-background text-accent">{icon}</span>
      </div>
      <p className="mt-4 font-mono text-2xl font-semibold text-accent">{value}</p>
      <p className="mt-1 text-sm text-muted">{detail}</p>
    </article>
  );
}

function SignalBlock({ icon, label, value, detail }: { icon: ReactNode; label: string; value: string; detail: string }) {
  return (
    <div className="rounded-md border border-line bg-background p-4">
      <div className="flex items-center gap-2 text-accent">
        {icon}
        <span className="text-sm text-muted">{label}</span>
      </div>
      <p className="mt-3 text-2xl font-semibold">{value}</p>
      <p className="mt-1 truncate text-sm text-muted">{detail}</p>
    </div>
  );
}

function RevenueLine({ label, value, strong = false }: { label: string; value: string; strong?: boolean }) {
  return (
    <div className="flex items-center justify-between gap-4 rounded-md border border-line bg-background p-4">
      <p className="text-sm text-muted">{label}</p>
      <p className={`font-mono font-semibold ${strong ? "text-3xl text-accent" : "text-lg text-foreground"}`}>{value}</p>
    </div>
  );
}

function AdminForm({
  onSubmit,
  buttonLabel,
  icon,
  isSaving,
  children,
}: {
  onSubmit: (event: FormEvent) => void;
  buttonLabel: string;
  icon: ReactNode;
  isSaving: boolean;
  children: ReactNode;
}) {
  return (
    <form onSubmit={onSubmit} className="mt-5">
      <div className="grid gap-4">{children}</div>
      <button
        type="submit"
        disabled={isSaving}
        className="group mt-5 flex w-full items-center justify-between rounded-md bg-accent px-4 py-3 font-semibold text-background transition active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-60"
      >
        <span className="flex items-center gap-2">
          {isSaving ? <Loader2 size={17} className="animate-spin" aria-hidden /> : icon}
          {isSaving ? "Saving" : buttonLabel}
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
        className="mt-2 w-full rounded-md border border-line bg-background px-3 py-3 text-foreground outline-none transition duration-300 placeholder:text-muted/60 focus:border-accent focus:ring-2 focus:ring-accent/20"
      />
    </label>
  );
}

function Textarea({ label, value, onChange }: { label: string; value: string; onChange: (value: string) => void }) {
  return (
    <label className="block text-sm font-medium text-muted">
      {label}
      <textarea
        value={value}
        onChange={(event) => onChange(event.target.value)}
        rows={4}
        className="mt-2 w-full resize-none rounded-md border border-line bg-background px-3 py-3 text-foreground outline-none transition duration-300 placeholder:text-muted/60 focus:border-accent focus:ring-2 focus:ring-accent/20"
      />
    </label>
  );
}

function Select({
  label,
  value,
  onChange,
  options,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  options: Array<{ value: string; label: string }>;
}) {
  return (
    <label className="block text-sm font-medium text-muted">
      {label}
      <select
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="mt-2 w-full rounded-md border border-line bg-background px-3 py-3 text-foreground outline-none transition duration-300 focus:border-accent focus:ring-2 focus:ring-accent/20"
      >
        <option value="">Select</option>
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
    </label>
  );
}

function InventoryPanel({ title, eyebrow, count, icon, children }: { title: string; eyebrow: string; count: number; icon: ReactNode; children: ReactNode }) {
  return (
    <Panel>
      <PanelHeader eyebrow={eyebrow} title={title} helper={`${count} records loaded from the admin API.`} icon={icon} />
      <div className="mt-5 max-h-[640px] space-y-3 overflow-y-auto pr-1 cinema-scrollbar-none">
        {count > 0 ? children : <EmptyState label="No records yet" helper="Create the first item from the form." />}
      </div>
    </Panel>
  );
}

function InventoryRow({ title, meta, detail }: { title: string; meta: string; detail: string }) {
  return (
    <article className="rounded-md border border-line bg-background p-4">
      <div className="flex items-start justify-between gap-4">
        <div className="min-w-0">
          <p className="truncate font-semibold">{title}</p>
          <p className="mt-1 truncate text-sm text-muted">{meta}</p>
        </div>
        <span className="shrink-0 rounded-md bg-panel px-2.5 py-1 font-mono text-xs text-accent">{detail}</span>
      </div>
    </article>
  );
}

function SeatPreview({ rows, columns }: { rows: number; columns: number }) {
  return (
    <div className="mt-4 overflow-x-auto cinema-scrollbar-none">
      <div className="min-w-max rounded-md border border-line bg-panel p-3">
        <div className="mb-3 rounded-md border border-line bg-background py-2 text-center font-mono text-xs uppercase text-muted">Screen</div>
        <div className="grid gap-2" style={{ gridTemplateColumns: `repeat(${columns}, minmax(24px, 1fr))` }}>
          {Array.from({ length: rows * columns }).map((_, index) => (
            <span key={index} className="grid size-6 place-items-center rounded border border-line bg-background text-accent">
              <Armchair size={13} aria-hidden />
            </span>
          ))}
        </div>
      </div>
    </div>
  );
}

function EmptyState({ label, helper }: { label: string; helper: string }) {
  return (
    <div className="rounded-md border border-dashed border-line bg-background p-5 text-center">
      <Sparkles size={22} className="mx-auto text-accent" aria-hidden />
      <p className="mt-3 font-semibold">{label}</p>
      <p className="mt-1 text-sm text-muted">{helper}</p>
    </div>
  );
}

function formatMoney(value: number) {
  return `$${Number(value || 0).toFixed(2)}`;
}

function formatTime(value: string) {
  return new Date(value).toLocaleTimeString("en-US", { hour: "numeric", minute: "2-digit" });
}

function formatDateTime(value: string) {
  return new Date(value).toLocaleString("en-US", {
    month: "short",
    day: "numeric",
    hour: "numeric",
    minute: "2-digit",
  });
}

function clamp(value: number, min: number, max: number) {
  return Math.min(max, Math.max(min, value));
}
