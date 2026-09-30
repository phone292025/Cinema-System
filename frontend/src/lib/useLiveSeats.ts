"use client";

import { useEffect, useEffectEvent, useState } from "react";

import { API_BASE, apiFetch } from "./api";
import type { SeatAvailability, SeatAvailabilityResponse, SeatEvent } from "./types";

export const SEAT_EVENT_TYPES = ["SEAT_LOCKED", "SEAT_RELEASED", "SEAT_BOOKED", "SEAT_BLOCKED", "SEAT_EXPIRED"] as const;
const LIVENESS_EVENT_TYPES = ["CONNECTED", "HEARTBEAT"] as const;

const BASE_RECONNECT_MS = 1_000;
const MAX_RECONNECT_MS = 30_000;

export type LiveStatus = "connecting" | "live" | "reconnecting";

export function reconnectDelay(failures: number) {
  return Math.min(MAX_RECONNECT_MS, BASE_RECONNECT_MS * 2 ** Math.max(0, failures - 1));
}

export function applySeatEvent(seats: SeatAvailability[], event: SeatEvent): SeatAvailability[] {
  return seats.map((seat) =>
    seat.seatId === event.seatId
      ? { ...seat, status: event.status, price: event.price ?? seat.price, lockedUntil: event.expiresAt ?? undefined }
      : seat,
  );
}

function parseSeatEvent(data: unknown): SeatEvent | null {
  if (typeof data !== "string") return null;
  try {
    const event = JSON.parse(data) as Partial<SeatEvent> | null;
    return event && typeof event.seatId === "string" && typeof event.status === "string" ? (event as SeatEvent) : null;
  } catch {
    return null;
  }
}

/**
 * Keeps a seat map in step with the server's seat-event stream. The stream is re-opened with
 * capped exponential backoff after any error (network blips, the server's SSE timeout), and the
 * full map is re-fetched on every reconnect because events sent while disconnected are lost.
 * Remount (e.g. via `key`) to switch showtimes.
 */
export function useLiveSeats(
  showtimeId: string,
  initialSeats: SeatAvailability[],
  onSeatsTaken?: (seatIds: string[]) => void,
) {
  const [seats, setSeats] = useState(initialSeats);
  const [status, setStatus] = useState<LiveStatus>("connecting");
  const reportTaken = useEffectEvent((seatIds: string[]) => {
    if (seatIds.length > 0) onSeatsTaken?.(seatIds);
  });

  useEffect(() => {
    let source: EventSource | null = null;
    let retryTimer: number | undefined;
    let failures = 0;
    let missedEvents = false;
    let disposed = false;
    let resyncRun = 0;

    const resync = () => {
      const run = ++resyncRun;
      apiFetch<SeatAvailabilityResponse>(`/showtimes/${showtimeId}/seats`, { skipAuthRedirect: true })
        .then((response) => {
          if (disposed || run !== resyncRun) return;
          setSeats(response.seats);
          reportTaken(response.seats.filter((seat) => seat.status !== "AVAILABLE").map((seat) => seat.seatId));
        })
        .catch(() => {
          // Keep the last known map; the next reconnect will try again.
        });
    };

    const handleSeatEvent = (message: Event) => {
      const event = parseSeatEvent((message as MessageEvent).data);
      if (!event || disposed) return;
      setSeats((current) => applySeatEvent(current, event));
      if (event.status !== "AVAILABLE") reportTaken([event.seatId]);
    };

    const markLive = () => {
      if (disposed) return;
      failures = 0;
      setStatus("live");
      if (missedEvents) {
        missedEvents = false;
        resync();
      }
    };

    const connect = () => {
      const stream = new EventSource(`${API_BASE}/showtimes/${showtimeId}/seat-events`);
      source = stream;
      stream.onopen = markLive;
      // A heartbeat may arrive as an SSE comment (never surfaced by EventSource) or as a named
      // HEARTBEAT event; either way it carries no seat data, so it only confirms the stream is up.
      LIVENESS_EVENT_TYPES.forEach((name) => stream.addEventListener(name, markLive));
      SEAT_EVENT_TYPES.forEach((name) => stream.addEventListener(name, handleSeatEvent));
      stream.onerror = () => {
        stream.close();
        if (disposed || source !== stream) return;
        source = null;
        failures += 1;
        missedEvents = true;
        setStatus("reconnecting");
        window.clearTimeout(retryTimer);
        retryTimer = window.setTimeout(connect, reconnectDelay(failures));
      };
    };

    connect();
    return () => {
      disposed = true;
      window.clearTimeout(retryTimer);
      source?.close();
      source = null;
    };
  }, [showtimeId]);

  return { seats, status };
}
