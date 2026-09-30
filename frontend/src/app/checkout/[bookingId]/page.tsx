"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { CheckCircle2, CreditCard, Loader2, ShieldCheck } from "lucide-react";
import { useCallback, useRef, useState } from "react";

import { AppShell } from "@/components/AppShell";
import { ErrorState, InlineError } from "@/components/Feedback";
import { HoldCountdown } from "@/components/HoldCountdown";
import { StatusBadge } from "@/components/StatusBadge";
import { ApiError, apiFetch, errorMessage } from "@/lib/api";
import { formatShowtime } from "@/lib/format";
import type { Booking, Payment } from "@/lib/types";
import { useApiQuery } from "@/lib/useApiQuery";

const HELD_STATUSES: Booking["status"][] = ["LOCKED", "PAYMENT_PENDING"];
const PAID_STATUSES: Booking["status"][] = ["PAID", "TICKET_ISSUED"];

const MOCK_PAYMENTS_DISABLED_MESSAGE =
  "Test payments are turned off on this server, so this booking can't be paid here. Your seats stay held until the timer runs out.";

export default function CheckoutPage() {
  const params = useParams<{ bookingId: string }>();
  const { data: booking, error: loadError, loading, reload } = useApiQuery<Booking>(`/bookings/${params.bookingId}`);
  const [payError, setPayError] = useState("");
  const [paying, setPaying] = useState(false);
  const [expiredFor, setExpiredFor] = useState<string | null>(null);
  const payingRef = useRef(false);
  const router = useRouter();

  const holdKey = booking?.expiresAt ?? "";
  const expired = expiredFor !== null && expiredFor === holdKey;
  const handleExpire = useCallback(() => setExpiredFor(holdKey), [holdKey]);

  const held = booking !== undefined && HELD_STATUSES.includes(booking.status);
  const paid = booking !== undefined && PAID_STATUSES.includes(booking.status);
  const payable = held && !expired;

  async function pay() {
    // The ref blocks a second submit that lands before React re-renders the disabled button.
    if (payingRef.current || !payable) return;
    payingRef.current = true;
    setPaying(true);
    setPayError("");
    try {
      const payment = await apiFetch<Payment>("/payments/initiate", {
        method: "POST",
        body: JSON.stringify({ bookingId: params.bookingId, method: "MOCK" }),
        idempotencyScope: `payment-initiate:${params.bookingId}`,
      });
      try {
        await apiFetch<Payment>("/payments/mock-callback", {
          method: "POST",
          body: JSON.stringify({ paymentReference: payment.paymentReference, status: "SUCCEEDED" }),
          idempotencyScope: `payment-callback:${payment.paymentReference}`,
        });
      } catch (err) {
        if (err instanceof ApiError && err.status === 404) throw new Error(MOCK_PAYMENTS_DISABLED_MESSAGE);
        throw err;
      }
      router.push(`/confirmation/${params.bookingId}`);
    } catch (err) {
      setPayError(errorMessage(err, "Payment failed."));
      payingRef.current = false;
      setPaying(false);
      // The payment may have landed even though the response didn't; show the real status.
      reload();
    }
  }

  return (
    <AppShell>
      <section className="mx-auto max-w-4xl px-4 py-10 sm:px-6">
        <h1 className="text-4xl font-semibold">Checkout</h1>
        {loadError && !booking ? (
          <ErrorState className="mt-5" title="We couldn't load this booking" message={loadError.message} onRetry={reload} retrying={loading}>
            <Link href="/bookings" className="text-sm font-semibold text-muted hover:text-accent">
              Your bookings
            </Link>
          </ErrorState>
        ) : null}
        {!booking && !loadError ? (
          <div role="status" aria-label="Loading booking" className="mt-5 h-72 animate-pulse rounded-lg border border-line bg-panel" />
        ) : null}
        <InlineError className="mt-5" message={payError} />
        {booking && (
          <>
            {held && (
              <div className="mt-5">
                <HoldCountdown key={booking.expiresAt} expiresAt={booking.expiresAt} onExpire={handleExpire} />
              </div>
            )}

            {paid && (
              <div role="status" className="mt-5 flex flex-wrap items-center justify-between gap-3 rounded-md border border-success/40 bg-success/10 p-4 text-success">
                <p className="flex items-center gap-2 text-sm font-medium">
                  <CheckCircle2 size={18} aria-hidden />
                  This booking is already paid.
                </p>
                <Link href={`/confirmation/${booking.id}`} className="text-sm font-semibold underline-offset-4 hover:underline">
                  View your ticket
                </Link>
              </div>
            )}

            {!held && !paid && (
              <p role="status" className="mt-5 rounded-md border border-line bg-panel p-4 text-sm text-muted">
                This booking can no longer be paid. Choose your seats again to start a new hold.
              </p>
            )}

            <div className="mt-5 rounded-lg border border-line bg-panel p-5">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div>
                  <p className="font-mono text-sm text-accent">{booking.bookingCode}</p>
                  <h2 className="mt-2 text-2xl font-semibold">{booking.movieTitle}</h2>
                  <p className="mt-1 text-muted">
                    {booking.cinemaName}, {booking.hallName} · {formatShowtime(booking.startTime)}
                  </p>
                </div>
                <StatusBadge status={booking.status} />
              </div>

              <p className="mt-6 font-mono text-xs uppercase text-accent">Your seats</p>
              <div className="mt-3 grid gap-3 sm:grid-cols-2">
                {booking.seats.map((seat) => (
                  <div key={seat.seatId} className="flex items-center justify-between rounded-md border border-line bg-background p-3 text-sm">
                    <span className="font-semibold">
                      Row {seat.rowLabel}, seat {seat.seatNumber}
                    </span>
                    <span className="text-muted">${Number(seat.price).toFixed(2)}</span>
                  </div>
                ))}
              </div>

              <div className="mt-6 flex items-center justify-between border-t border-line pt-5">
                <span className="text-muted">Total</span>
                <span className="text-2xl font-semibold text-accent">${Number(booking.totalAmount).toFixed(2)}</span>
              </div>

              <div className="mt-6 flex flex-wrap gap-3">
                {!paid && (
                  <button
                    type="button"
                    onClick={pay}
                    disabled={paying || !payable}
                    aria-busy={paying}
                    className="flex items-center gap-2 rounded-md bg-accent px-5 py-3 font-semibold text-background disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    {paying ? <Loader2 className="animate-spin" size={18} aria-hidden /> : <CreditCard size={18} aria-hidden />}
                    {paying ? "Processing payment" : `Pay $${Number(booking.totalAmount).toFixed(2)}`}
                  </button>
                )}
                {!paid && (
                  <Link
                    href={`/showtimes/${booking.showtimeId}/seats`}
                    className="rounded-md border border-line px-5 py-3 text-muted hover:border-accent hover:text-accent"
                  >
                    {payable ? "Change seats" : "Choose seats again"}
                  </Link>
                )}
              </div>

              <p className="mt-4 flex items-center gap-2 text-xs text-muted">
                <ShieldCheck size={14} aria-hidden />
                Test payment only. No card details are collected and you will never be charged twice.
              </p>
            </div>
          </>
        )}
      </section>
    </AppShell>
  );
}
