"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { CreditCard, Loader2 } from "lucide-react";
import { useEffect, useState } from "react";

import { AppShell } from "@/components/AppShell";
import { StatusBadge } from "@/components/StatusBadge";
import { apiFetch } from "@/lib/api";
import type { Booking, Payment } from "@/lib/types";

export default function CheckoutPage() {
  const params = useParams<{ bookingId: string }>();
  const [booking, setBooking] = useState<Booking | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const router = useRouter();

  useEffect(() => {
    apiFetch<Booking>(`/bookings/${params.bookingId}`).then(setBooking).catch((err) => setError(err.message));
  }, [params.bookingId]);

  async function pay() {
    setLoading(true);
    setError("");
    try {
      const payment = await apiFetch<Payment>("/payments/initiate", {
        method: "POST",
        body: JSON.stringify({ bookingId: params.bookingId, method: "MOCK" }),
      });
      await apiFetch<Payment>("/payments/mock-callback", {
        method: "POST",
        body: JSON.stringify({ paymentReference: payment.paymentReference, status: "SUCCEEDED" }),
      });
      router.push(`/confirmation/${params.bookingId}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Payment failed.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <AppShell>
      <section className="mx-auto max-w-4xl px-4 py-10 sm:px-6">
        <h1 className="text-4xl font-semibold">Checkout</h1>
        {error && <p className="mt-5 rounded-md border border-danger/40 bg-danger/10 p-4 text-danger">{error}</p>}
        {booking && (
          <div className="mt-6 rounded-lg border border-line bg-panel p-5">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <p className="font-mono text-sm text-accent">{booking.bookingCode}</p>
                <h2 className="mt-2 text-2xl font-semibold">{booking.movieTitle}</h2>
                <p className="mt-1 text-muted">
                  {booking.cinemaName}, {booking.hallName} · {new Date(booking.startTime).toLocaleString()}
                </p>
              </div>
              <StatusBadge status={booking.status} />
            </div>
            <div className="mt-6 grid gap-3 sm:grid-cols-2">
              {booking.seats.map((seat) => (
                <div key={seat.seatId} className="rounded-md border border-line bg-background p-3 text-sm">
                  {seat.rowLabel}
                  {seat.seatNumber} · {seat.seatType} · ${Number(seat.price).toFixed(2)}
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
                disabled={loading || !["LOCKED", "PAYMENT_PENDING"].includes(booking.status)}
                className="flex items-center gap-2 rounded-md bg-accent px-5 py-3 font-semibold text-background disabled:opacity-50"
              >
                {loading ? <Loader2 className="animate-spin" size={18} aria-hidden /> : <CreditCard size={18} aria-hidden />}
                Pay with mock gateway
              </button>
              <Link href={`/showtimes/${booking.showtimeId}/seats`} className="rounded-md border border-line px-5 py-3 text-muted hover:border-accent hover:text-accent">
                Change seats
              </Link>
            </div>
          </div>
        )}
      </section>
    </AppShell>
  );
}
