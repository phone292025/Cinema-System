"use client";

import { useParams } from "next/navigation";
import { useEffect, useState } from "react";

import { AppShell } from "@/components/AppShell";
import { SeatPicker } from "@/components/SeatPicker";
import { apiFetch } from "@/lib/api";
import { demoShowtimes, findDemoSeatAvailability } from "@/lib/demo-data";
import type { SeatAvailabilityResponse, Showtime } from "@/lib/types";

export default function SeatSelectionPage() {
  const params = useParams<{ id: string }>();
  const [showtime, setShowtime] = useState<Showtime | null>(null);
  const [availability, setAvailability] = useState<SeatAvailabilityResponse | null>(null);
  const [error, setError] = useState("");
  const [demoMode, setDemoMode] = useState(false);

  useEffect(() => {
    Promise.all([apiFetch<Showtime>(`/showtimes/${params.id}`), apiFetch<SeatAvailabilityResponse>(`/showtimes/${params.id}/seats`)])
      .then(([showtimeResponse, seatsResponse]) => {
        setShowtime(showtimeResponse);
        setAvailability(seatsResponse);
        setDemoMode(false);
      })
      .catch((err) => {
        const fallbackShowtime = demoShowtimes.find((item) => item.id === params.id);
        const fallbackAvailability = findDemoSeatAvailability(params.id);
        if (fallbackShowtime && fallbackAvailability) {
          setShowtime(fallbackShowtime);
          setAvailability(fallbackAvailability);
          setDemoMode(true);
          setError("Showing a local demo seat map until the backend API is running.");
          return;
        }
        setError(err.message);
      });
  }, [params.id]);

  return (
    <AppShell>
      <section className="mx-auto max-w-7xl overflow-hidden px-4 py-7 sm:px-6 md:py-10">
        <div className="mb-8">
          <p className="font-mono text-xs uppercase text-accent">Seat map</p>
          <h1 className="mt-2 text-3xl font-semibold leading-tight sm:text-4xl">{showtime?.movieTitle ?? "Select seats"}</h1>
          {showtime && (
            <p className="mt-2 text-muted">
              {showtime.cinemaName}, {showtime.hallName} · {new Date(showtime.startTime).toLocaleString()}
            </p>
          )}
        </div>
        {error && <p className="rounded-md border border-danger/40 bg-danger/10 p-4 text-danger">{error}</p>}
        {availability && <SeatPicker showtimeId={params.id} seats={availability.seats} demoMode={demoMode} />}
      </section>
    </AppShell>
  );
}
