"use client";

import { Search, ShieldCheck, TicketCheck } from "lucide-react";
import { FormEvent, useEffect, useState } from "react";

import { AppShell } from "@/components/AppShell";
import { StatusBadge } from "@/components/StatusBadge";
import { apiFetch, getStoredUser } from "@/lib/api";
import type { BookingSeat, Showtime, User } from "@/lib/types";

type StaffValidation = {
  ticketCode: string;
  bookingCode: string;
  status: string;
  movieTitle: string;
  cinemaName: string;
  hallName: string;
  startTime: string;
  seats: BookingSeat[];
  validatedAt?: string;
};

type StaffBooking = {
  id: string;
  bookingCode: string;
  movieTitle: string;
  cinemaName: string;
  hallName: string;
  startTime: string;
  status: string;
  seats: BookingSeat[];
};

export default function StaffPage() {
  const [user, setUser] = useState<User | null>(null);
  const [showtimes, setShowtimes] = useState<Showtime[]>([]);
  const [ticketCode, setTicketCode] = useState("");
  const [bookingCode, setBookingCode] = useState("");
  const [validation, setValidation] = useState<StaffValidation | null>(null);
  const [booking, setBooking] = useState<StaffBooking | null>(null);
  const [error, setError] = useState("");

  const canUseStaff = user?.role === "ADMIN" || user?.role === "STAFF";

  useEffect(() => {
    setUser(getStoredUser());
  }, []);

  useEffect(() => {
    if (!canUseStaff) return;
    apiFetch<Showtime[]>("/staff/showtimes/today").then(setShowtimes).catch(() => undefined);
  }, [canUseStaff]);

  async function validate(event: FormEvent) {
    event.preventDefault();
    setError("");
    setValidation(null);
    try {
      const response = await apiFetch<StaffValidation>(`/staff/tickets/${encodeURIComponent(ticketCode)}/validate`, {
        method: "POST",
        body: JSON.stringify({ qrToken: ticketCode }),
      });
      setValidation(response);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Ticket validation failed.");
    }
  }

  async function searchBooking(event: FormEvent) {
    event.preventDefault();
    setError("");
    setBooking(null);
    try {
      const response = await apiFetch<StaffBooking>(`/staff/bookings/search?code=${encodeURIComponent(bookingCode)}`);
      setBooking(response);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Booking search failed.");
    }
  }

  return (
    <AppShell>
      <section className="mx-auto max-w-7xl px-4 py-10 sm:px-6">
        <p className="font-mono text-xs uppercase text-accent">Staff console</p>
        <h1 className="mt-2 text-4xl font-semibold">Ticket validation</h1>
        <p className="mt-3 max-w-2xl text-muted">Scan or paste a QR token, search a booking code, and keep today's sessions visible.</p>

        {!canUseStaff && (
          <div className="mt-6 rounded-lg border border-danger/40 bg-danger/10 p-5 text-danger">
            Login as a staff or admin user to use this screen.
          </div>
        )}

        {canUseStaff && (
          <>
            {error && <p className="mt-5 rounded-md border border-danger/40 bg-danger/10 p-4 text-danger">{error}</p>}
            <div className="mt-6 space-y-6">
              <form onSubmit={validate} className="rounded-lg border border-line bg-panel p-5">
                <div className="flex items-center gap-2 text-accent">
                  <ShieldCheck size={18} aria-hidden />
                  <p className="font-mono text-xs uppercase">Validate ticket</p>
                </div>
                <input
                  value={ticketCode}
                  onChange={(event) => setTicketCode(event.target.value)}
                  placeholder="Paste QR token or ticket code"
                  className="mt-5 w-full rounded-md border border-line bg-background px-3 py-3 outline-none focus:border-accent"
                />
                <button type="submit" className="mt-4 flex w-full items-center justify-center gap-2 rounded-md bg-accent px-4 py-3 font-semibold text-background">
                  <TicketCheck size={18} aria-hidden />
                  Validate
                </button>
              </form>

              {validation && (
                <article className="rounded-lg border border-success/40 bg-success/10 p-5">
                  <div className="flex items-center justify-between gap-3">
                    <p className="font-mono text-sm text-accent">{validation.ticketCode}</p>
                    <StatusBadge status={validation.status} />
                  </div>
                  <h2 className="mt-3 text-2xl font-semibold">{validation.movieTitle}</h2>
                  <p className="mt-2 text-muted">
                    {validation.cinemaName}, {validation.hallName} · {new Date(validation.startTime).toLocaleString()}
                  </p>
                  <p className="mt-3 text-sm text-muted">Seats: {validation.seats.map((seat) => `${seat.rowLabel}${seat.seatNumber}`).join(", ")}</p>
                </article>
              )}

              <form onSubmit={searchBooking} className="rounded-lg border border-line bg-panel p-5">
                <div className="flex items-center gap-2 text-accent">
                  <Search size={18} aria-hidden />
                  <p className="font-mono text-xs uppercase">Booking lookup</p>
                </div>
                <input
                  value={bookingCode}
                  onChange={(event) => setBookingCode(event.target.value)}
                  placeholder="CBX-20260426-AB12"
                  className="mt-5 w-full rounded-md border border-line bg-background px-3 py-3 outline-none focus:border-accent"
                />
                <button type="submit" className="mt-4 rounded-md border border-line px-4 py-3 font-semibold text-muted hover:border-accent hover:text-accent">
                  Search booking
                </button>
              </form>

              {booking && (
                <article className="rounded-lg border border-line bg-panel p-5">
                  <p className="font-mono text-sm text-accent">{booking.bookingCode}</p>
                  <h2 className="mt-3 text-xl font-semibold">{booking.movieTitle}</h2>
                  <p className="mt-2 text-muted">
                    {booking.cinemaName}, {booking.hallName} · {new Date(booking.startTime).toLocaleString()}
                  </p>
                  <div className="mt-3">
                    <StatusBadge status={booking.status} />
                  </div>
                </article>
              )}

              <section className="rounded-lg border border-line bg-panel p-5">
                <div className="flex flex-wrap items-end justify-between gap-3">
                  <div>
                    <p className="font-mono text-xs uppercase text-accent">Today's sessions</p>
                    <h2 className="mt-2 text-2xl font-semibold">Session table</h2>
                  </div>
                  <p className="text-sm text-muted">{showtimes.length} sessions</p>
                </div>

                <div className="mt-5 overflow-x-auto cinema-scrollbar-none">
                  <table className="w-full min-w-[720px] border-separate border-spacing-0 text-left">
                    <thead>
                      <tr className="text-xs uppercase text-muted">
                        <th className="border-b border-line px-4 py-3 font-mono">Movie</th>
                        <th className="border-b border-line px-4 py-3 font-mono">Cinema</th>
                        <th className="border-b border-line px-4 py-3 font-mono">Hall</th>
                        <th className="border-b border-line px-4 py-3 font-mono">Time</th>
                      </tr>
                    </thead>
                    <tbody>
                      {showtimes.map((showtime) => (
                        <tr key={showtime.id} className="align-top">
                          <td className="border-b border-line/70 px-4 py-4 font-semibold">{showtime.movieTitle}</td>
                          <td className="border-b border-line/70 px-4 py-4 text-muted">{showtime.cinemaName}</td>
                          <td className="border-b border-line/70 px-4 py-4 text-muted">{showtime.hallName}</td>
                          <td className="border-b border-line/70 px-4 py-4 font-mono text-accent">
                            {new Date(showtime.startTime).toLocaleTimeString()}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                  {showtimes.length === 0 && <p className="rounded-md border border-dashed border-line bg-background p-5 text-sm text-muted">No showtimes today.</p>}
                </div>
              </section>
            </div>
          </>
        )}
      </section>
    </AppShell>
  );
}
