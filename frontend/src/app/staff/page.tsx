"use client";

import { Loader2, Search, ShieldCheck, TicketCheck } from "lucide-react";
import { FormEvent, useCallback, useRef, useState, useSyncExternalStore } from "react";

import { AppShell } from "@/components/AppShell";
import { ErrorState, InlineError } from "@/components/Feedback";
import { QrScanner } from "@/components/QrScanner";
import { StatusBadge } from "@/components/StatusBadge";
import { apiFetch, errorMessage, getStoredUser, subscribeToAuthChanges } from "@/lib/api";
import { formatShowtime, formatTimeOnly } from "@/lib/format";
import type { BookingSeat, Showtime } from "@/lib/types";
import { useApiQuery } from "@/lib/useApiQuery";

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

const noShowtimes: Showtime[] = [];

export default function StaffPage() {
  const user = useSyncExternalStore(subscribeToAuthChanges, getStoredUser, () => null);
  const canUseStaff = user?.role === "ADMIN" || user?.role === "STAFF";
  const {
    data: todaysShowtimes,
    error: showtimesError,
    loading: showtimesLoading,
    reload: reloadShowtimes,
  } = useApiQuery<Showtime[]>(canUseStaff ? "/staff/showtimes/today" : null);
  const showtimes = todaysShowtimes ?? noShowtimes;
  const [ticketCode, setTicketCode] = useState("");
  const [bookingCode, setBookingCode] = useState("");
  const [validation, setValidation] = useState<StaffValidation | null>(null);
  const [booking, setBooking] = useState<StaffBooking | null>(null);
  const [error, setError] = useState("");
  const [scanError, setScanError] = useState("");
  const [validating, setValidating] = useState(false);
  const [searching, setSearching] = useState(false);
  // Validation marks a ticket as used, so a double scan must not send a second request.
  const validatingRef = useRef(false);
  const ticketInputRef = useRef<HTMLInputElement>(null);

  const runValidation = useCallback(async (code: string) => {
    const trimmed = code.trim();
    if (!trimmed || validatingRef.current) return;
    validatingRef.current = true;
    setValidating(true);
    setScanError("");
    setValidation(null);
    try {
      const response = await apiFetch<StaffValidation>(`/staff/tickets/${encodeURIComponent(trimmed)}/validate`, {
        method: "POST",
        body: JSON.stringify({ qrToken: trimmed }),
      });
      setValidation(response);
    } catch (err) {
      setScanError(errorMessage(err, "Ticket validation failed."));
    } finally {
      validatingRef.current = false;
      setValidating(false);
    }
  }, []);

  async function validate(event: FormEvent) {
    event.preventDefault();
    const code = ticketCode;
    // A USB scanner types the next code straight into this field, so it must be empty and focused again.
    setTicketCode("");
    ticketInputRef.current?.focus();
    await runValidation(code);
  }

  const handleScan = useCallback(
    (value: string) => {
      setTicketCode(value);
      void runValidation(value);
    },
    [runValidation],
  );

  async function searchBooking(event: FormEvent) {
    event.preventDefault();
    const code = bookingCode.trim();
    if (!code || searching) return;
    setSearching(true);
    setError("");
    setBooking(null);
    try {
      const response = await apiFetch<StaffBooking>(`/staff/bookings/search?code=${encodeURIComponent(code)}`);
      setBooking(response);
    } catch (err) {
      setError(errorMessage(err, "Booking search failed."));
    } finally {
      setSearching(false);
    }
  }

  return (
    <AppShell>
      <section className="mx-auto max-w-7xl px-4 py-10 sm:px-6">
        <p className="font-mono text-xs uppercase text-accent">Staff console</p>
        <h1 className="mt-2 text-4xl font-semibold">Ticket validation</h1>
        <p className="mt-3 max-w-2xl text-muted">Scan a ticket at the door, look up a booking by its code, and keep an eye on today&apos;s sessions.</p>

        {!canUseStaff && (
          <div className="mt-6 rounded-lg border border-danger/40 bg-danger/10 p-5 text-danger">
            You need a staff or admin account to use this screen.
          </div>
        )}

        {canUseStaff && (
          <>
            <InlineError className="mt-5" message={error} />
            <div className="mt-6 grid items-start gap-6 xl:grid-cols-[minmax(0,440px)_minmax(0,1fr)]">
              <div className="space-y-6">
              <div className="rounded-lg border border-line bg-panel p-5">
                <div className="flex items-center gap-2 text-accent">
                  <ShieldCheck size={18} aria-hidden />
                  <p className="font-mono text-xs uppercase">Validate ticket</p>
                </div>
                <div className="mt-5">
                  <QrScanner onScan={handleScan} />
                </div>
                <form onSubmit={validate} className="mt-5 border-t border-line pt-5">
                  <label htmlFor="ticket-code" className="text-sm text-muted">
                    Or enter the ticket code
                  </label>
                  <input
                    id="ticket-code"
                    ref={ticketInputRef}
                    value={ticketCode}
                    onChange={(event) => setTicketCode(event.target.value)}
                    placeholder="Scan or type the code, then press Enter"
                    autoComplete="off"
                    className="mt-2 w-full rounded-md border border-line bg-background px-3 py-3 outline-none focus:border-accent"
                  />
                  <button
                    type="submit"
                    disabled={!ticketCode.trim() || validating}
                    className="mt-4 flex w-full items-center justify-center gap-2 rounded-md border border-line px-4 py-3 font-semibold text-muted hover:border-accent hover:text-accent disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    {validating ? <Loader2 size={18} className="animate-spin" aria-hidden /> : <TicketCheck size={18} aria-hidden />}
                    {validating ? "Validating" : "Validate"}
                  </button>
                </form>
                <InlineError className="mt-4" message={scanError} />
              </div>

              {validation && (
                <article role="status" className="rounded-lg border border-success/40 bg-success/10 p-5">
                  <div className="flex items-center justify-between gap-3">
                    <p className="font-mono text-sm text-accent">{validation.ticketCode}</p>
                    <StatusBadge status={validation.status} />
                  </div>
                  <h2 className="mt-3 text-2xl font-semibold">{validation.movieTitle}</h2>
                  <p className="mt-2 text-muted">
                    {validation.cinemaName}, {validation.hallName} · {formatShowtime(validation.startTime)}
                  </p>
                  <p className="mt-3 text-sm text-muted">Seats: {validation.seats.map((seat) => `${seat.rowLabel}${seat.seatNumber}`).join(", ")}</p>
                </article>
              )}

              <form onSubmit={searchBooking} className="rounded-lg border border-line bg-panel p-5">
                <div className="flex items-center gap-2 text-accent">
                  <Search size={18} aria-hidden />
                  <p className="font-mono text-xs uppercase">Booking lookup</p>
                </div>
                <label htmlFor="booking-code" className="mt-5 block text-sm text-muted">
                  Booking code
                </label>
                <input
                  id="booking-code"
                  value={bookingCode}
                  onChange={(event) => setBookingCode(event.target.value)}
                  placeholder="CBX-20261001-AB12CD34"
                  autoComplete="off"
                  className="mt-2 w-full rounded-md border border-line bg-background px-3 py-3 outline-none focus:border-accent"
                />
                <button
                  type="submit"
                  disabled={!bookingCode.trim() || searching}
                  className="mt-4 flex items-center gap-2 rounded-md border border-line px-4 py-3 font-semibold text-muted hover:border-accent hover:text-accent disabled:cursor-not-allowed disabled:opacity-50"
                >
                  {searching && <Loader2 size={18} className="animate-spin" aria-hidden />}
                  Search booking
                </button>
              </form>

              {booking && (
                <article role="status" className="rounded-lg border border-line bg-panel p-5">
                  <p className="font-mono text-sm text-accent">{booking.bookingCode}</p>
                  <h2 className="mt-3 text-xl font-semibold">{booking.movieTitle}</h2>
                  <p className="mt-2 text-muted">
                    {booking.cinemaName}, {booking.hallName} · {formatShowtime(booking.startTime)}
                  </p>
                  <div className="mt-3">
                    <StatusBadge status={booking.status} />
                  </div>
                </article>
              )}
              </div>

              <section className="rounded-lg border border-line bg-panel p-5">
                <div className="flex flex-wrap items-end justify-between gap-3">
                  <div>
                    <p className="font-mono text-xs uppercase text-accent">Today&apos;s sessions</p>
                    <h2 className="mt-2 text-2xl font-semibold">Session table</h2>
                  </div>
                  <p className="text-sm text-muted">{todaysShowtimes ? `${showtimes.length} sessions` : ""}</p>
                </div>

                {showtimesError && !todaysShowtimes ? (
                  <ErrorState
                    className="mt-5"
                    title="We couldn't load today's sessions"
                    message={showtimesError.message}
                    onRetry={reloadShowtimes}
                    retrying={showtimesLoading}
                  />
                ) : (
                  <div className="mt-5 max-h-[34rem] overflow-auto cinema-scrollbar-none">
                    <table className="w-full min-w-[560px] border-separate border-spacing-0 text-left">
                      <thead className="sticky top-0 z-10 bg-panel">
                        <tr className="text-xs uppercase text-muted">
                          <th className="border-b border-line px-4 py-3 font-mono">Movie</th>
                          <th className="border-b border-line px-4 py-3 font-mono">Cinema</th>
                          <th className="border-b border-line px-4 py-3 font-mono">Hall</th>
                          <th className="border-b border-line px-4 py-3 font-mono">Time</th>
                        </tr>
                      </thead>
                      <tbody>
                        {showtimes.map((showtime) => (
                          <tr key={showtime.id}>
                            <td className="whitespace-nowrap border-b border-line/70 px-4 py-3 font-semibold">{showtime.movieTitle}</td>
                            <td className="whitespace-nowrap border-b border-line/70 px-4 py-3 text-muted">{showtime.cinemaName}</td>
                            <td className="whitespace-nowrap border-b border-line/70 px-4 py-3 text-muted">{showtime.hallName}</td>
                            <td className="whitespace-nowrap border-b border-line/70 px-4 py-3 font-mono text-accent">
                              {formatTimeOnly(showtime.startTime)}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                    {!todaysShowtimes && (
                      <p role="status" className="flex items-center gap-2 p-5 text-sm text-muted">
                        <Loader2 size={16} className="animate-spin text-accent" aria-hidden />
                        Loading today&apos;s sessions…
                      </p>
                    )}
                    {todaysShowtimes && showtimes.length === 0 && (
                      <p className="rounded-md border border-dashed border-line bg-background p-5 text-sm text-muted">No showtimes today.</p>
                    )}
                  </div>
                )}
              </section>
            </div>
          </>
        )}
      </section>
    </AppShell>
  );
}
