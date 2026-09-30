"use client";

import { CalendarClock, CalendarPlus } from "lucide-react";
import type { FormEvent } from "react";

import { formatDateTime, formatMoney } from "@/lib/format";
import type { Cinema, Movie, Showtime, Hall } from "@/lib/types";
import { HallForm } from "./model";
import { AdminForm, FilterableInventoryPanel, Input, Panel, PanelHeader, Select, WorkspaceGrid } from "./ui";

export function ShowtimeWorkspace({
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
  hallForm: HallForm;
  setHallForm: React.Dispatch<React.SetStateAction<HallForm>>;
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

      <FilterableInventoryPanel
        title="Scheduled sessions"
        eyebrow="Calendar"
        icon={<CalendarClock size={18} aria-hidden />}
        items={showtimes}
        searchPlaceholder="Search by movie or hall"
        matches={(showtime, query) =>
          `${showtime.movieTitle} ${showtime.hallName} ${showtime.cinemaName}`.toLowerCase().includes(query)
        }
        renderItem={(showtime) => (
          <article key={showtime.id} className="rounded-md border border-line bg-background p-4">
            <div className="flex items-start justify-between gap-4">
              <div className="min-w-0">
                <p className="truncate font-semibold">{showtime.movieTitle}</p>
                <p className="mt-1 truncate text-sm text-muted">
                  {showtime.hallName} · {formatDateTime(showtime.startTime)}
                </p>
              </div>
              <span className="shrink-0 rounded-md bg-panel px-2.5 py-1 font-mono text-xs text-accent">
                {formatMoney(Number(showtime.basePrice))}
              </span>
            </div>
          </article>
        )}
      />
    </WorkspaceGrid>
  );
}
