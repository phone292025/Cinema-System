"use client";

import { CalendarX2, Ticket as TicketIcon, XCircle } from "lucide-react";
import Link from "next/link";
import { useEffect, useMemo, useState } from "react";

import { AppShell } from "@/components/AppShell";
import { StatusBadge } from "@/components/StatusBadge";
import { apiFetch } from "@/lib/api";
import { formatShowtime } from "@/lib/format";
import type { Booking } from "@/lib/types";

const CANCELLABLE = ["PAID", "TICKET_ISSUED", "LOCKED", "PAYMENT_PENDING"];

export default function BookingsPage() {
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [error, setError] = useState("");
  const [pendingCancel, setPendingCancel] = useState<string | null>(null);
  const [cancelling, setCancelling] = useState<string | null>(null);

  function load() {
    apiFetch<Booking[]>("/users/me/bookings").then(setBookings).catch((err) => setError(err.message));
  }

  useEffect(load, []);

  const [now] = useState(() => Date.now());

  const { upcoming, past } = useMemo(() => {
    const sorted = bookings.slice().sort((a, b) => new Date(b.startTime).getTime() - new Date(a.startTime).getTime());
    return {
      upcoming: sorted
        .filter((booking) => new Date(booking.startTime).getTime() >= now)
        .sort((a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime()),
      past: sorted.filter((booking) => new Date(booking.startTime).getTime() < now),
    };
  }, [bookings, now]);

  async function cancel(id: string) {
    setError("");
    setCancelling(id);
    try {
      await apiFetch<Booking>(`/bookings/${id}/cancel`, { method: "POST", idempotencyScope: `booking-cancel:${id}` });
      setPendingCancel(null);
      load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not cancel booking.");
    } finally {
      setCancelling(null);
    }
  }

  function renderBooking(booking: Booking, isPast: boolean) {
    const canCancel = !isPast && CANCELLABLE.includes(booking.status);
    const confirming = pendingCancel === booking.id;

    return (
      <article key={booking.id} className={`rounded-lg border border-line bg-panel p-5 ${isPast ? "opacity-75" : ""}`}>
        <div className="flex flex-col justify-between gap-4 md:flex-row md:items-center">
          <div>
            <p className="font-mono text-sm text-accent">{booking.bookingCode}</p>
            <h3 className="mt-2 text-xl font-semibold">{booking.movieTitle}</h3>
            <p className="mt-1 text-sm text-muted">
              {booking.cinemaName}, {booking.hallName} · {formatShowtime(booking.startTime)}
            </p>
            <p className="mt-2 text-sm text-muted">
              Seats {booking.seats.map((seat) => `${seat.rowLabel}${seat.seatNumber}`).join(", ")}
            </p>
          </div>
          <div className="flex flex-wrap items-center gap-3">
            <StatusBadge status={booking.status} />
            {booking.status === "TICKET_ISSUED" && (
              <Link
                href={`/confirmation/${booking.id}`}
                className="flex items-center gap-2 rounded-md border border-line px-3 py-2 text-sm text-muted hover:border-accent hover:text-accent"
              >
                <TicketIcon size={16} aria-hidden />
                View ticket
              </Link>
            )}
            {canCancel && !confirming && (
              <button
                type="button"
                onClick={() => setPendingCancel(booking.id)}
                className="flex items-center gap-2 rounded-md border border-line px-3 py-2 text-sm text-muted hover:border-danger hover:text-danger"
              >
                <XCircle size={16} aria-hidden />
                Cancel
              </button>
            )}
          </div>
        </div>

        {confirming && (
          <div className="mt-4 rounded-md border border-danger/40 bg-danger/10 p-4">
            <p className="text-sm text-danger">Cancel this booking? Your seats go back on sale and the ticket stops working.</p>
            <div className="mt-3 flex flex-wrap gap-3">
              <button
                type="button"
                onClick={() => cancel(booking.id)}
                disabled={cancelling === booking.id}
                className="rounded-md bg-danger px-4 py-2 text-sm font-semibold text-background disabled:opacity-50"
              >
                {cancelling === booking.id ? "Cancelling…" : "Yes, cancel it"}
              </button>
              <button
                type="button"
                onClick={() => setPendingCancel(null)}
                className="rounded-md border border-line px-4 py-2 text-sm text-muted hover:text-foreground"
              >
                Keep booking
              </button>
            </div>
          </div>
        )}
      </article>
    );
  }

  return (
    <AppShell>
      <section className="mx-auto max-w-6xl px-4 py-10 sm:px-6">
        <h1 className="text-4xl font-semibold">Your bookings</h1>
        {error && <p className="mt-5 rounded-md border border-danger/40 bg-danger/10 p-4 text-danger">{error}</p>}

        <h2 className="mt-8 font-mono text-xs uppercase text-accent">Upcoming</h2>
        <div className="mt-3 grid gap-4">
          {upcoming.map((booking) => renderBooking(booking, false))}
          {upcoming.length === 0 && (
            <div className="rounded-lg border border-dashed border-line bg-panel p-8 text-center">
              <CalendarX2 className="mx-auto text-accent" size={32} aria-hidden />
              <p className="mt-3 font-semibold">Nothing booked yet</p>
              <p className="mt-1 text-sm text-muted">Pick a showtime and your seats are held while you pay.</p>
              <Link href="/movies" className="mt-4 inline-block rounded-md bg-accent px-4 py-2 text-sm font-semibold text-background">
                Browse movies
              </Link>
            </div>
          )}
        </div>

        {past.length > 0 && (
          <>
            <h2 className="mt-10 font-mono text-xs uppercase text-muted">Past</h2>
            <div className="mt-3 grid gap-4">{past.map((booking) => renderBooking(booking, true))}</div>
          </>
        )}
      </section>
    </AppShell>
  );
}
