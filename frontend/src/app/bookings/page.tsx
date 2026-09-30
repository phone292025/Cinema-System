"use client";

import { CalendarX2, Ticket as TicketIcon, XCircle } from "lucide-react";
import Link from "next/link";
import { useMemo, useState } from "react";

import { AppShell } from "@/components/AppShell";
import { ErrorState, InlineError } from "@/components/Feedback";
import { StatusBadge } from "@/components/StatusBadge";
import { apiFetch, errorMessage } from "@/lib/api";
import { formatMoney, formatShowtime, formatTimeOnly } from "@/lib/format";
import type { Booking } from "@/lib/types";
import { useApiQuery } from "@/lib/useApiQuery";

const PAID: Booking["status"][] = ["PAID", "TICKET_ISSUED"];
const noBookings: Booking[] = [];

function cancelledMessage(booking: Booking) {
  if (booking.status === "REFUNDED") return `Booking ${booking.bookingCode} was cancelled and ${formatMoney(booking.totalAmount)} has been refunded.`;
  if (booking.status === "REFUND_PENDING") return `Booking ${booking.bookingCode} was cancelled. Your refund is being processed.`;
  return `Booking ${booking.bookingCode} was cancelled and the seats are back on sale.`;
}

export default function BookingsPage() {
  const { data, error: loadError, loading, reload } = useApiQuery<Booking[]>("/users/me/bookings");
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [pendingCancel, setPendingCancel] = useState<string | null>(null);
  const [cancelling, setCancelling] = useState<string | null>(null);
  const bookings = data ?? noBookings;

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
    if (cancelling) return;
    setError("");
    setSuccess("");
    setCancelling(id);
    try {
      const cancelled = await apiFetch<Booking>(`/bookings/${id}/cancel`, { method: "POST", idempotencyScope: `booking-cancel:${id}` });
      setPendingCancel(null);
      if (cancelled) setSuccess(cancelledMessage(cancelled));
      reload();
    } catch (err) {
      setError(errorMessage(err, "Could not cancel booking."));
    } finally {
      setCancelling(null);
    }
  }

  function renderBooking(booking: Booking, isPast: boolean) {
    const cancelDeadline = booking.cancellableUntil ? new Date(booking.cancellableUntil).getTime() : null;
    const canCancel = !isPast && cancelDeadline !== null && now < cancelDeadline;
    const cancellationClosed = !isPast && PAID.includes(booking.status) && cancelDeadline !== null && now >= cancelDeadline;
    const confirming = pendingCancel === booking.id;
    const seatLabels = booking.seats.map((seat) => `${seat.rowLabel}${seat.seatNumber}`).join(", ");

    return (
      <article key={booking.id} className={`rounded-lg border border-line bg-panel p-5 ${isPast ? "opacity-75" : ""}`}>
        <div className="flex flex-col justify-between gap-4 md:flex-row md:items-center">
          <div>
            <p className="font-mono text-sm text-accent">{booking.bookingCode}</p>
            <h3 className="mt-2 text-xl font-semibold">{booking.movieTitle}</h3>
            <p className="mt-1 text-sm text-muted">
              {booking.cinemaName}, {booking.hallName} · {formatShowtime(booking.startTime)}
            </p>
            <p className="mt-2 text-sm text-muted">Seats {seatLabels}</p>
            {cancellationClosed && (
              <p className="mt-2 text-xs text-muted">Cancellation closed at {formatTimeOnly(booking.cancellableUntil!)}, too close to the show.</p>
            )}
          </div>
          <div className="flex flex-wrap items-center gap-3">
            <StatusBadge status={booking.status} />
            {booking.status === "TICKET_ISSUED" && (
              <Link
                href={`/confirmation/${booking.id}`}
                aria-label={`View ticket for ${booking.movieTitle}, booking ${booking.bookingCode}`}
                className="flex items-center gap-2 rounded-md border border-line px-3 py-2 text-sm text-muted hover:border-accent hover:text-accent"
              >
                <TicketIcon size={16} aria-hidden />
                View ticket
              </Link>
            )}
            {canCancel && !confirming && (
              <button
                type="button"
                onClick={() => {
                  setSuccess("");
                  setPendingCancel(booking.id);
                }}
                aria-label={`Cancel booking ${booking.bookingCode} for ${booking.movieTitle}`}
                className="flex items-center gap-2 rounded-md border border-line px-3 py-2 text-sm text-muted hover:border-danger hover:text-danger"
              >
                <XCircle size={16} aria-hidden />
                Cancel
              </button>
            )}
          </div>
        </div>

        {confirming && (
          <div role="alertdialog" aria-labelledby={`cancel-${booking.id}`} className="mt-4 rounded-md border border-danger/40 bg-danger/10 p-4">
            <p id={`cancel-${booking.id}`} className="text-sm text-danger">
              {PAID.includes(booking.status)
                ? `Cancel this booking? Your seats go back on sale, the ticket stops working, and ${formatMoney(booking.totalAmount)} is refunded.`
                : "Cancel this booking? Your held seats go back on sale."}
            </p>
            <div className="mt-3 flex flex-wrap gap-3">
              <button
                type="button"
                onClick={() => cancel(booking.id)}
                disabled={cancelling !== null}
                className="rounded-md bg-danger px-4 py-2 text-sm font-semibold text-background disabled:opacity-50"
              >
                {cancelling === booking.id ? "Cancelling…" : "Yes, cancel it"}
              </button>
              <button
                type="button"
                onClick={() => setPendingCancel(null)}
                disabled={cancelling === booking.id}
                className="rounded-md border border-line px-4 py-2 text-sm text-muted hover:text-foreground disabled:opacity-50"
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
        <InlineError className="mt-5" message={error} />
        {success && (
          <p role="status" className="mt-5 rounded-md border border-success/40 bg-success/10 p-4 text-success">
            {success}
          </p>
        )}
        {loadError && !data ? (
          <ErrorState className="mt-5" title="We couldn't load your bookings" message={loadError.message} onRetry={reload} retrying={loading} />
        ) : null}

        <h2 className="mt-8 font-mono text-xs uppercase text-accent">Upcoming</h2>
        <div className="mt-3 grid gap-4">
          {!data && !loadError
            ? Array.from({ length: 2 }).map((_, index) => (
                <div key={index} aria-hidden className="h-36 animate-pulse rounded-lg border border-line bg-panel" />
              ))
            : null}
          {upcoming.map((booking) => renderBooking(booking, false))}
          {data && upcoming.length === 0 && (
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
