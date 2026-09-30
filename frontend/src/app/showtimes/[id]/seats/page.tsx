"use client";

import Link from "next/link";
import { useParams } from "next/navigation";

import { AppShell } from "@/components/AppShell";
import { ErrorState } from "@/components/Feedback";
import { SeatPicker } from "@/components/SeatPicker";
import { apiFetch } from "@/lib/api";
import { formatShowtime } from "@/lib/format";
import type { SeatAvailabilityResponse, Showtime } from "@/lib/types";
import { useApiQuery } from "@/lib/useApiQuery";

async function loadSeatMap(showtimeId: string) {
  const [showtime, availability] = await Promise.all([
    apiFetch<Showtime>(`/showtimes/${showtimeId}`),
    apiFetch<SeatAvailabilityResponse>(`/showtimes/${showtimeId}/seats`),
  ]);
  return { showtime, availability };
}

export default function SeatSelectionPage() {
  const params = useParams<{ id: string }>();
  const { data, error, loading, reload } = useApiQuery(params.id, loadSeatMap);
  const showtime = data?.showtime;

  return (
    <AppShell>
      <section className="mx-auto max-w-7xl overflow-hidden px-4 py-7 sm:px-6 md:py-10">
        <div className="mb-8">
          <p className="font-mono text-xs uppercase text-accent">Seat map</p>
          <h1 className="mt-2 text-3xl font-semibold leading-tight sm:text-4xl">{showtime?.movieTitle ?? "Select seats"}</h1>
          {showtime && (
            <p className="mt-2 text-muted">
              {showtime.cinemaName}, {showtime.hallName} · {formatShowtime(showtime.startTime)}
            </p>
          )}
        </div>
        {error && !data ? (
          <ErrorState title="We couldn't load this seat map" message={error.message} onRetry={reload} retrying={loading}>
            <Link href="/movies" className="text-sm font-semibold text-muted hover:text-accent">
              Back to movies
            </Link>
          </ErrorState>
        ) : null}
        {!data && !error ? <SeatMapSkeleton /> : null}
        {data ? <SeatPicker key={params.id} showtimeId={params.id} seats={data.availability.seats} /> : null}
      </section>
    </AppShell>
  );
}

function SeatMapSkeleton() {
  return (
    <div role="status" aria-label="Loading seat map" className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_320px]">
      <div className="rounded-lg border border-line bg-panel p-5">
        <div className="mb-5 h-10 animate-pulse rounded-md bg-background" />
        <div className="mx-auto max-w-[620px] space-y-3">
          {Array.from({ length: 6 }).map((_, index) => (
            <div key={index} className="h-8 animate-pulse rounded-md bg-background" />
          ))}
        </div>
      </div>
      <div className="h-64 animate-pulse rounded-lg border border-line bg-panel" />
    </div>
  );
}
