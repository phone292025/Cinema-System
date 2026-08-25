"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { CreditCard, Loader2, ShieldCheck } from "lucide-react";
import { useCallback, useEffect, useState } from "react";

import { AppShell } from "@/components/AppShell";
import { HoldCountdown } from "@/components/HoldCountdown";
import { StatusBadge } from "@/components/StatusBadge";
import { apiFetch } from "@/lib/api";
import { formatShowtime } from "@/lib/format";
import type { Booking, Payment } from "@/lib/types";

export default function CheckoutPage() {
  const params = useParams<{ bookingId: string }>();
  const [booking, setBooking] = useState<Booking | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [expired, setExpired] = useState(false);
  const router = useRouter();

  useEffect(() => {
    apiFetch<Booking>(`/bookings/${params.bookingId}`).then(setBooking).catch((err) => setError(err.message));
  }, [params.bookingId]);

  const handleExpire = useCallback(() => setExpired(true), []);

  async function pay() {
    setLoading(true);
    setError("");
    try {
      const payment = await apiFetch<Payment>("/payments/initiate", {
        method: "POST",
        body: JSON.stringify({ bookingId: params.bookingId, method: "MOCK" }),
        idempotencyScope: `payment-initiate:${params.bookingId}`,
      });
      await apiFetch<Payment>("/payments/mock-callback", {
        method: "POST",
        body: JSON.stringify({ paymentReference: payment.paymentReference, status: "SUCCEEDED" }),
        idempotencyScope: `payment-callback:${payment.paymentReference}`,
      });
      router.push(`/confirmation/${params.bookingId}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Payment failed.");
    } finally {
      setLoading(false);
    }
  }

  const payable = booking !== null && !expired && ["LOCKED", "PAYMENT_PENDING"].includes(booking.status);

  return (
    <AppShell>
      <section className="mx-auto max-w-4xl px-4 py-10 sm:px-6">
        <h1 className="text-4xl font-semibold">Checkout</h1>
        {error && <p className="mt-5 rounded-md border border-danger/40 bg-danger/10 p-4 text-danger">{error}</p>}
        {booking && (
          <>
            {["LOCKED", "PAYMENT_PENDING"].includes(booking.status) && (
              <div className="mt-5">
                <HoldCountdown key={booking.expiresAt} expiresAt={booking.expiresAt} onExpire={handleExpire} />
              </div>
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
                <button
                  type="button"
                  onClick={pay}
                  disabled={loading || !payable}
                  className="flex items-center gap-2 rounded-md bg-accent px-5 py-3 font-semibold text-background disabled:opacity-50"
                >
                  {loading ? <Loader2 className="animate-spin" size={18} aria-hidden /> : <CreditCard size={18} aria-hidden />}
                  Pay ${Number(booking.totalAmount).toFixed(2)}
                </button>
                <Link
                  href={`/showtimes/${booking.showtimeId}/seats`}
                  className="rounded-md border border-line px-5 py-3 text-muted hover:border-accent hover:text-accent"
                >
                  {expired ? "Choose seats again" : "Change seats"}
                </Link>
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
