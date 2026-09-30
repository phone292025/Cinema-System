"use client";

import { CalendarClock, CheckCircle2, CircleDollarSign, Film, Gauge, Plus, Settings2, Ticket } from "lucide-react";
import { useState } from "react";

import { formatDateTime, formatMoney } from "@/lib/format";
import type { Cinema, Dashboard, Movie, Showtime, Hall } from "@/lib/types";
import { EmptyState, MetricCard, Occupancy, Panel, PanelHeader } from "./ui";

export function DashboardWorkspace({
  dashboard,
  movies,
  cinemas,
  halls,
  showtimes,
  averageTicket,
}: {
  dashboard: Dashboard | null;
  movies: Movie[];
  cinemas: Cinema[];
  halls: Hall[];
  showtimes: Showtime[];
  averageTicket: number;
}) {
  const setupItems = [
    { label: "Movies", complete: movies.length > 0, helper: "Add at least one film to the catalog" },
    { label: "Cinemas", complete: cinemas.length > 0, helper: "Create a branch to host screenings" },
    { label: "Halls", complete: halls.length > 0, helper: `Add a hall to ${cinemas[0]?.name ?? "the selected branch"}` },
    { label: "Showtimes", complete: showtimes.length > 0, helper: "Schedule sessions so seats go on sale" },
  ];
  const outstanding = setupItems.filter((item) => !item.complete);
  const [now] = useState(() => Date.now());
  const upcoming = showtimes
    .filter((showtime) => new Date(showtime.startTime).getTime() >= now)
    .slice(0, 5);

  return (
    <div className="grid gap-6 xl:grid-cols-[1fr_380px]">
      <div className="space-y-6">
        <MetricsGrid dashboard={dashboard} averageTicket={averageTicket} />
        <Panel>
          <PanelHeader eyebrow="Today" title="Next up" helper="The sessions customers can walk into soonest." icon={<Gauge size={19} aria-hidden />} />
          <div className="mt-5 space-y-2">
            {upcoming.map((showtime) => (
              <div key={showtime.id} className="flex flex-wrap items-center justify-between gap-3 rounded-md border border-line bg-background p-4">
                <div className="min-w-0">
                  <p className="truncate font-semibold">{showtime.movieTitle}</p>
                  <p className="mt-1 text-sm text-muted">
                    {showtime.hallName} · {formatDateTime(showtime.startTime)}
                  </p>
                </div>
                <Occupancy sold={showtime.soldSeats} total={showtime.totalSeats} />
              </div>
            ))}
            {upcoming.length === 0 && (
              <EmptyState label="Nothing scheduled ahead" helper="Create showtimes so customers have something to book." />
            )}
          </div>
        </Panel>
      </div>

      <Panel>
        <PanelHeader
          eyebrow="Setup"
          title={outstanding.length === 0 ? "Ready to sell" : "Finish setup"}
          helper={
            outstanding.length === 0
              ? "Everything a booking depends on is in place."
              : `${outstanding.length} ${outstanding.length === 1 ? "step" : "steps"} left before seats can go on sale.`
          }
          icon={<Settings2 size={19} aria-hidden />}
        />
        <div className="mt-5 space-y-3">
          {setupItems.map((item) => (
            <div key={item.label} className="flex items-center justify-between gap-4 rounded-md border border-line bg-background p-4">
              <div>
                <p className={item.complete ? "font-semibold text-muted" : "font-semibold"}>{item.label}</p>
                {!item.complete && <p className="mt-1 text-sm text-muted">{item.helper}</p>}
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

export function MetricsGrid({ dashboard, averageTicket }: { dashboard: Dashboard | null; averageTicket: number }) {
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
