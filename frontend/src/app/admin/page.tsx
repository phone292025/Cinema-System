"use client";

import { Activity, Loader2, ShieldCheck } from "lucide-react";
import { useState, useSyncExternalStore } from "react";

import { AppShell } from "@/components/AppShell";
import { AuditWorkspace } from "@/components/admin/AuditWorkspace";
import { CinemaWorkspace } from "@/components/admin/CinemaWorkspace";
import { DashboardWorkspace } from "@/components/admin/DashboardWorkspace";
import { HallWorkspace } from "@/components/admin/HallWorkspace";
import { MovieWorkspace } from "@/components/admin/MovieWorkspace";
import { sections, type AdminSection } from "@/components/admin/model";
import { ShowtimeWorkspace } from "@/components/admin/ShowtimeWorkspace";
import { TicketSalesWorkspace } from "@/components/admin/TicketSalesWorkspace";
import { AccessRequired, StatusStrip, clamp } from "@/components/admin/ui";
import { useAdminConsole } from "@/components/admin/useAdminConsole";
import { getStoredUser, subscribeToAuthChanges } from "@/lib/api";

export default function AdminPage() {
  const user = useSyncExternalStore(subscribeToAuthChanges, getStoredUser, () => null);
  const [activeSection, setActiveSection] = useState<AdminSection>("dashboard");
  const canManage = user?.role === "ADMIN";
  const {
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
  } = useAdminConsole(canManage, canManage);

  const activeMeta = sections.find((section) => section.key === activeSection) ?? sections[0];
  const revenue = Number(dashboard?.revenue ?? 0);
  const paidBookings = Number(dashboard?.paidBookings ?? 0);
  const averageTicket = paidBookings > 0 ? revenue / paidBookings : 0;
  const seatPreviewRows = clamp(Number(hallForm.totalRows) || 0, 1, 9);
  const seatPreviewColumns = clamp(Number(hallForm.totalColumns) || 0, 1, 12);

  return (
    <AppShell>
      <section className="relative min-h-[calc(100dvh-65px)] overflow-hidden bg-background px-4 py-5 sm:px-6 lg:py-8">
        <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_12%_4%,rgba(244,184,96,0.14),transparent_28%),radial-gradient(circle_at_86%_18%,rgba(68,194,141,0.08),transparent_28%)]" />
        <div className="pointer-events-none absolute inset-x-0 top-0 h-px bg-accent/30" />

        <div className="relative mx-auto max-w-7xl">
          {/* An operations screen should open on data, not on a marketing headline —
              on a phone the old hero filled the whole first screen. */}
          <header className="flex flex-wrap items-center justify-between gap-4">
            <div className="flex min-w-0 items-center gap-3">
              <span className="grid size-11 shrink-0 place-items-center rounded-lg border border-line bg-panel text-accent">
                {activeMeta.icon}
              </span>
              <div className="min-w-0">
                <p className="flex items-center gap-2 font-mono text-xs uppercase text-accent">
                  <ShieldCheck size={13} aria-hidden />
                  {canManage ? `${user?.role} access` : "Admin access required"}
                </p>
                <h1 className="mt-1 truncate text-2xl font-semibold sm:text-3xl">{activeMeta.label}</h1>
              </div>
            </div>
            <button
              type="button"
              onClick={() => canManage && load()}
              disabled={!canManage || isLoading}
              className="flex items-center justify-center gap-2 rounded-md border border-line px-4 py-2.5 text-sm font-semibold text-muted transition hover:border-accent hover:text-accent active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-50"
            >
              {isLoading ? <Loader2 size={16} className="animate-spin" aria-hidden /> : <Activity size={16} aria-hidden />}
              {isLoading ? "Refreshing" : "Refresh"}
            </button>
          </header>

          {!canManage ? (
            <AccessRequired />
          ) : (
            <>
              {/* Wrapping pills instead of fixed-width cards: the old strip ran off
                  the right edge of a 1280px screen with no scroll affordance. */}
              <nav className="mt-5 flex flex-wrap gap-2" aria-label="Admin workspaces">
                {sections.map((section) => (
                  <button
                    key={section.key}
                    type="button"
                    aria-current={activeSection === section.key ? "page" : undefined}
                    onClick={() => setActiveSection(section.key)}
                    className={`flex items-center gap-2 rounded-md border px-3.5 py-2.5 text-sm font-medium transition active:scale-[0.98] ${
                      activeSection === section.key
                        ? "border-accent bg-accent/12 text-foreground"
                        : "border-line bg-panel/60 text-muted hover:border-accent/60 hover:text-foreground"
                    }`}
                  >
                    <span className="text-accent">{section.icon}</span>
                    {section.label}
                  </button>
                ))}
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
                    onSubmit={saveMovie}
                    isSaving={isSaving}
                    editingMovieId={editingMovieId}
                    onEdit={startEditMovie}
                    onCancelEdit={cancelEditMovie}
                    onSetStatus={setMovieStatus}
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
