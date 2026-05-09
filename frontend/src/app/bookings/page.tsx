"use client";

import { RotateCcw } from "lucide-react";
import Link from "next/link";
import { useEffect, useState } from "react";

import { AppShell } from "@/components/AppShell";
import { StatusBadge } from "@/components/StatusBadge";
import { apiFetch } from "@/lib/api";
import type { Booking } from "@/lib/types";

export default function BookingsPage() {
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [error, setError] = useState("");

  function load() {
    apiFetch<Booking[]>("/users/me/bookings").then(setBookings).catch((err) => setError(err.message));
  }

  useEffect(load, []);

  async function cancel(id: string) {
    setError("");
    try {
      await apiFetch<Booking>(`/bookings/${id}/cancel`, { method: "POST" });
      load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not cancel booking.");
    }
  }

  return (
    <AppShell>
      <section className="mx-auto max-w-6xl px-4 py-10 sm:px-6">
        <h1 className="text-4xl font-semibold">Booking history</h1>
        {error && <p className="mt-5 rounded-md border border-danger/40 bg-danger/10 p-4 text-danger">{error}</p>}
        <div className="mt-6 grid gap-4">
          {bookings.map((booking) => (
            <article key={booking.id} className="rounded-lg border border-line bg-panel p-5">
              <div className="flex flex-col justify-between gap-4 md:flex-row md:items-center">
                <div>
                  <p className="font-mono text-sm text-accent">{booking.bookingCode}</p>
                  <h2 className="mt-2 text-xl font-semibold">{booking.movieTitle}</h2>
                  <p className="mt-1 text-sm text-muted">
                    {booking.cinemaName}, {booking.hallName} · {new Date(booking.startTime).toLocaleString()}
                  </p>
                  <p className="mt-2 text-sm text-muted">
                    {booking.seats.map((seat) => `${seat.rowLabel}${seat.seatNumber}`).join(", ")}
                  </p>
                </div>
                <div className="flex items-center gap-3">
                  <StatusBadge status={booking.status} />
                  {booking.status === "TICKET_ISSUED" && (
                    <Link
                      href={`/confirmation/${booking.id}`}
                      className="rounded-md border border-line px-3 py-2 text-sm text-muted hover:border-accent hover:text-accent"
                    >
                      Ticket
                    </Link>
                  )}
                  {(booking.status === "PAID" || booking.status === "TICKET_ISSUED" || booking.status === "LOCKED" || booking.status === "PAYMENT_PENDING") && (
                    <button
                      type="button"
                      onClick={() => cancel(booking.id)}
                      className="grid size-10 place-items-center rounded-md border border-line text-muted hover:border-danger hover:text-danger"
                      title="Cancel booking"
                      aria-label="Cancel booking"
                    >
                      <RotateCcw size={17} aria-hidden />
                    </button>
                  )}
                </div>
              </div>
            </article>
          ))}
          {bookings.length === 0 && <p className="rounded-lg border border-line bg-panel p-5 text-muted">No bookings yet.</p>}
        </div>
      </section>
    </AppShell>
  );
}
