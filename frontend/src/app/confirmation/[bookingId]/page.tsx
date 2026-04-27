"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { CheckCircle2, History } from "lucide-react";
import { useEffect, useState } from "react";

import { AppShell } from "@/components/AppShell";
import { StatusBadge } from "@/components/StatusBadge";
import { apiFetch } from "@/lib/api";
import type { Booking } from "@/lib/types";

export default function ConfirmationPage() {
  const params = useParams<{ bookingId: string }>();
  const [booking, setBooking] = useState<Booking | null>(null);

  useEffect(() => {
    apiFetch<Booking>(`/bookings/${params.bookingId}`).then(setBooking);
  }, [params.bookingId]);

  return (
    <AppShell>
      <section className="mx-auto max-w-3xl px-4 py-12 text-center sm:px-6">
        <CheckCircle2 className="mx-auto text-success" size={54} aria-hidden />
        <h1 className="mt-4 text-4xl font-semibold">Booking confirmed</h1>
        {booking && (
          <div className="mt-6 rounded-lg border border-line bg-panel p-5 text-left">
            <div className="flex items-center justify-between gap-3">
              <p className="font-mono text-accent">{booking.bookingCode}</p>
              <StatusBadge status={booking.status} />
            </div>
            <h2 className="mt-4 text-2xl font-semibold">{booking.movieTitle}</h2>
            <p className="mt-2 text-muted">
              {booking.cinemaName}, {booking.hallName} · {new Date(booking.startTime).toLocaleString()}
            </p>
            <p className="mt-4 text-sm text-muted">
              Seats: {booking.seats.map((seat) => `${seat.rowLabel}${seat.seatNumber}`).join(", ")}
            </p>
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
