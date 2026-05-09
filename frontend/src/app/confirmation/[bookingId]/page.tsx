"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { CheckCircle2, History, QrCode } from "lucide-react";
import { useEffect, useState } from "react";

import { AppShell } from "@/components/AppShell";
import { StatusBadge } from "@/components/StatusBadge";
import { apiBlob, apiFetch } from "@/lib/api";
import type { Booking, Ticket } from "@/lib/types";

export default function ConfirmationPage() {
  const params = useParams<{ bookingId: string }>();
  const [booking, setBooking] = useState<Booking | null>(null);
  const [ticket, setTicket] = useState<Ticket | null>(null);
  const [qrUrl, setQrUrl] = useState("");

  useEffect(() => {
    apiFetch<Booking>(`/bookings/${params.bookingId}`).then(setBooking);
  }, [params.bookingId]);

  useEffect(() => {
    let cancelled = false;
    let attempts = 0;
    let objectUrl = "";

    async function loadTicket() {
      attempts += 1;
      try {
        const response = await apiFetch<Ticket>(`/bookings/${params.bookingId}/ticket`);
        if (cancelled) return;
        setTicket(response);
        const blob = await apiBlob(response.qrUrl);
        if (cancelled) return;
        objectUrl = URL.createObjectURL(blob);
        setQrUrl(objectUrl);
      } catch {
        if (!cancelled && attempts < 8) {
          window.setTimeout(loadTicket, 1200);
        }
      }
    }

    void loadTicket();
    return () => {
      cancelled = true;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
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
        {ticket && (
          <div className="mt-5 rounded-lg border border-line bg-panel p-5 text-left">
            <div className="flex flex-col gap-5 sm:flex-row sm:items-center">
              <div className="grid size-44 place-items-center rounded-md border border-line bg-white p-3">
                {qrUrl ? <img src={qrUrl} alt={`QR ticket ${ticket.ticketCode}`} className="h-full w-full object-contain" /> : <QrCode className="text-background" size={68} aria-hidden />}
              </div>
              <div>
                <p className="font-mono text-sm text-accent">{ticket.ticketCode}</p>
                <h2 className="mt-2 text-2xl font-semibold">QR ticket ready</h2>
                <p className="mt-2 text-muted">Staff can scan this code at the cinema entrance. Keep it private.</p>
                <div className="mt-4">
                  <StatusBadge status={ticket.status} />
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
