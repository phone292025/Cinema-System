"use client";

import { BarChart3, WalletCards } from "lucide-react";

import { formatDateTime, formatMoney } from "@/lib/format";
import type { Dashboard, Showtime } from "@/lib/types";
import { EmptyState, Occupancy, Panel, PanelHeader, RevenueLine } from "./ui";

export function TicketSalesWorkspace({
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
  const busiestSessions = showtimes
    .slice()
    .sort((a, b) => Number(b.soldSeats ?? 0) - Number(a.soldSeats ?? 0))
    .slice(0, 10);

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
        <PanelHeader eyebrow="Demand" title="Session sales board" helper="How full each session is, busiest first." icon={<BarChart3 size={19} aria-hidden />} />
        <div className="mt-5 grid gap-3">
          {busiestSessions.map((showtime) => (
            <div key={showtime.id} className="grid gap-4 rounded-md border border-line bg-background p-4 md:grid-cols-[1fr_auto] md:items-center">
              <div className="min-w-0">
                <p className="truncate text-lg font-semibold">{showtime.movieTitle}</p>
                <p className="mt-1 text-sm text-muted">
                  {showtime.cinemaName}, {showtime.hallName} · {formatDateTime(showtime.startTime)}
                </p>
                <p className="mt-1 font-mono text-sm text-accent">
                  {formatMoney(showtime.revenue ?? 0)} sold ·{" "}
                  {formatMoney(Number(showtime.basePrice))} a seat
                </p>
              </div>
              <div className="md:justify-self-end">
                <Occupancy sold={showtime.soldSeats} total={showtime.totalSeats} />
              </div>
            </div>
          ))}
          {showtimes.length === 0 ? <EmptyState label="No sessions on sale" helper="Create showtimes before tracking ticket sales." /> : null}
        </div>
      </Panel>
    </div>
  );
}
