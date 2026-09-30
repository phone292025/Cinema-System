"use client";

import Image from "next/image";
import Link from "next/link";
import { useParams } from "next/navigation";
import { AlertTriangle, CheckCircle2, Clock3, History, Loader2, QrCode, RotateCcw, XCircle } from "lucide-react";

import { AppShell } from "@/components/AppShell";
import { ErrorState } from "@/components/Feedback";
import { StatusBadge } from "@/components/StatusBadge";
import { formatShowtime } from "@/lib/format";
import type { Booking } from "@/lib/types";
import { useApiQuery } from "@/lib/useApiQuery";
import { useTicketPolling } from "@/lib/useTicketPolling";

const CONFIRMED: Booking["status"][] = ["PAID", "TICKET_ISSUED"];
const AWAITING_PAYMENT: Booking["status"][] = ["LOCKED", "PAYMENT_PENDING"];

const inactiveCopy: Partial<Record<Booking["status"], { title: string; body: string }>> = {
  CANCELLED: { title: "Booking cancelled", body: "These seats were released and the ticket no longer works." },
  EXPIRED: { title: "Seat hold expired", body: "Payment wasn't completed in time, so the seats went back on sale." },
  REFUND_PENDING: { title: "Refund in progress", body: "This booking was cancelled and the refund is being processed." },
  REFUNDED: { title: "Booking refunded", body: "This booking was cancelled and the payment has been refunded." },
};

export default function ConfirmationPage() {
  const params = useParams<{ bookingId: string }>();
  const { data: booking, error, loading, reload } = useApiQuery<Booking>(`/bookings/${params.bookingId}`);
  const confirmed = booking !== undefined && CONFIRMED.includes(booking.status);
  const { state: ticketState, retry: retryTicket } = useTicketPolling(params.bookingId, confirmed);

  if (!booking) {
    return (
      <AppShell>
        <section className="mx-auto max-w-3xl px-4 py-12 sm:px-6">
          {error ? (
            <ErrorState title="We couldn't load this booking" message={error.message} onRetry={reload} retrying={loading}>
              <Link href="/bookings" className="text-sm font-semibold text-muted hover:text-accent">
                Your bookings
              </Link>
            </ErrorState>
          ) : (
            <div role="status" className="flex flex-col items-center gap-4 py-10 text-center text-muted">
              <Loader2 className="animate-spin text-accent" size={40} aria-hidden />
              Checking your booking…
            </div>
          )}
        </section>
      </AppShell>
    );
  }

  const awaitingPayment = AWAITING_PAYMENT.includes(booking.status);
  const inactive = inactiveCopy[booking.status];

  return (
    <AppShell>
      <section className="mx-auto max-w-3xl px-4 py-12 text-center sm:px-6">
        {confirmed ? (
          <>
            <CheckCircle2 className="mx-auto text-success" size={54} aria-hidden />
            <h1 className="mt-4 text-4xl font-semibold">Booking confirmed</h1>
          </>
        ) : awaitingPayment ? (
          <>
            <Clock3 className="mx-auto text-accent" size={54} aria-hidden />
            <h1 className="mt-4 text-4xl font-semibold">Payment not completed</h1>
            <p className="mt-3 text-muted">Your seats are held, but we haven&apos;t received payment for this booking yet.</p>
          </>
        ) : (
          <>
            <XCircle className="mx-auto text-danger" size={54} aria-hidden />
            <h1 className="mt-4 text-4xl font-semibold">{inactive?.title ?? "Booking not active"}</h1>
            <p className="mt-3 text-muted">{inactive?.body ?? "This booking can't be used for entry."}</p>
          </>
        )}

        <div className="mt-6 rounded-lg border border-line bg-panel p-5 text-left">
          <div className="flex items-center justify-between gap-3">
            <p className="font-mono text-accent">{booking.bookingCode}</p>
            <StatusBadge status={ticketState.kind === "ready" && booking.status === "PAID" ? "TICKET_ISSUED" : booking.status} />
          </div>
          <h2 className="mt-4 text-2xl font-semibold">{booking.movieTitle}</h2>
          <p className="mt-2 text-muted">
            {booking.cinemaName}, {booking.hallName} · {formatShowtime(booking.startTime)}
          </p>
          <p className="mt-4 text-sm text-muted">
            Seats: {booking.seats.map((seat) => `${seat.rowLabel}${seat.seatNumber}`).join(", ")}
          </p>
        </div>

        {awaitingPayment && (
          <div className="mt-5 flex flex-wrap justify-center gap-3">
            <Link href={`/checkout/${booking.id}`} className="rounded-md bg-accent px-5 py-3 font-semibold text-background">
              Finish payment
            </Link>
            <button
              type="button"
              onClick={reload}
              disabled={loading}
              className="flex items-center gap-2 rounded-md border border-line px-5 py-3 text-muted hover:border-accent hover:text-accent disabled:opacity-50"
            >
              {loading ? <Loader2 className="animate-spin" size={18} aria-hidden /> : <RotateCcw size={18} aria-hidden />}
              Check again
            </button>
          </div>
        )}

        {confirmed && ticketState.kind !== "ready" && (
          <div className="mt-5 rounded-lg border border-line bg-panel p-5 text-left">
            <div className="flex flex-col gap-5 sm:flex-row sm:items-center">
              <div className="grid size-44 shrink-0 place-items-center rounded-md border border-dashed border-line bg-background">
                {ticketState.kind === "pending" ? (
                  <Loader2 className="animate-spin text-accent" size={40} aria-hidden />
                ) : ticketState.kind === "failed" ? (
                  <AlertTriangle className="text-danger" size={60} aria-hidden />
                ) : (
                  <QrCode className="text-muted" size={68} aria-hidden />
                )}
              </div>
              <div role={ticketState.kind === "failed" ? "alert" : "status"}>
                <h2 className="text-2xl font-semibold">
                  {ticketState.kind === "pending"
                    ? "Preparing your ticket"
                    : ticketState.kind === "failed"
                      ? "We couldn't load your ticket"
                      : "This is taking longer than usual"}
                </h2>
                <p className="mt-2 text-muted">
                  {ticketState.kind === "pending"
                    ? "Your payment went through. We are generating the QR code you will show at the entrance."
                    : ticketState.kind === "failed"
                      ? ticketState.message
                      : "Your booking is paid and safe. Try again in a moment, or find the ticket in your booking history shortly."}
                </p>
                {ticketState.kind !== "pending" && (
                  <button
                    type="button"
                    onClick={retryTicket}
                    className="mt-4 flex items-center gap-2 rounded-md border border-line px-4 py-2 text-sm font-semibold text-muted hover:border-accent hover:text-accent"
                  >
                    <RotateCcw size={16} aria-hidden />
                    Retry
                  </button>
                )}
              </div>
            </div>
          </div>
        )}
        {ticketState.kind === "ready" && (
          <div className="mt-5 rounded-lg border border-line bg-panel p-5 text-left">
            <div className="flex flex-col gap-5 sm:flex-row sm:items-center">
              <div className="grid size-44 place-items-center rounded-md border border-line bg-white p-3">
                {ticketState.qrUrl ? (
                  <Image
                    src={ticketState.qrUrl}
                    alt={`QR ticket ${ticketState.ticket.ticketCode}`}
                    width={152}
                    height={152}
                    unoptimized
                    className="h-full w-full object-contain"
                  />
                ) : (
                  <QrCode className="text-background" size={68} aria-hidden />
                )}
              </div>
              <div>
                <p className="font-mono text-sm text-accent">{ticketState.ticket.ticketCode}</p>
                <h2 className="mt-2 text-2xl font-semibold">QR ticket ready</h2>
                <p className="mt-2 text-muted">
                  {ticketState.qrUrl
                    ? "Staff can scan this code at the cinema entrance. Keep it private."
                    : "The QR image didn't load. Staff can also type the ticket code above at the entrance."}
                </p>
                <div className="mt-4 flex flex-wrap items-center gap-3">
                  <StatusBadge status={ticketState.ticket.status} />
                  {!ticketState.qrUrl && (
                    <button
                      type="button"
                      onClick={retryTicket}
                      className="flex items-center gap-2 rounded-md border border-line px-3 py-1.5 text-xs font-semibold text-muted hover:border-accent hover:text-accent"
                    >
                      <RotateCcw size={14} aria-hidden />
                      Reload QR code
                    </button>
                  )}
                </div>
              </div>
            </div>
          </div>
        )}
        <Link href="/bookings" className="mt-6 inline-flex items-center gap-2 rounded-md bg-accent px-5 py-3 font-semibold text-background">
          <History size={18} aria-hidden />
          View booking history
        </Link>
      </section>
    </AppShell>
  );
}
