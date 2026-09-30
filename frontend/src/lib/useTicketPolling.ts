"use client";

import { useCallback, useEffect, useState } from "react";

import { ApiError, apiBlob, apiFetch } from "./api";
import type { Ticket } from "./types";

export const TICKET_POLL_BUDGET_MS = 30_000;
const FIRST_DELAY_MS = 1_000;
const MAX_DELAY_MS = 6_000;

export function ticketPollDelay(attempt: number) {
  return Math.min(MAX_DELAY_MS, Math.round(FIRST_DELAY_MS * 1.5 ** Math.max(0, attempt - 1)));
}

type Outcome =
  | { kind: "ready"; ticket: Ticket; qrUrl: string }
  | { kind: "slow" }
  | { kind: "failed"; message: string };

export type TicketPollState = { kind: "idle" } | { kind: "pending" } | Outcome;

/**
 * Tickets are issued asynchronously after payment, so poll with backoff for about 30 seconds,
 * then settle on "slow" and let the user retry by hand.
 */
export function useTicketPolling(bookingId: string, enabled: boolean) {
  const [run, setRun] = useState(0);
  const [result, setResult] = useState<{ key: string; outcome: Outcome } | null>(null);
  const key = `${bookingId}:${run}`;

  useEffect(() => {
    if (!enabled) return undefined;
    let cancelled = false;
    let timer: number | undefined;
    let objectUrl = "";
    const startedAt = Date.now();
    let attempt = 0;

    const settle = (outcome: Outcome) => {
      if (!cancelled) setResult({ key, outcome });
    };

    const poll = async () => {
      attempt += 1;
      let ticket: Ticket;
      try {
        ticket = await apiFetch<Ticket>(`/bookings/${bookingId}/ticket`);
      } catch (err) {
        if (cancelled) return;
        if (err instanceof ApiError && (err.status === 401 || err.status === 403)) {
          settle({ kind: "failed", message: err.message });
          return;
        }
        const delay = ticketPollDelay(attempt);
        if (Date.now() - startedAt + delay > TICKET_POLL_BUDGET_MS) {
          settle({ kind: "slow" });
          return;
        }
        timer = window.setTimeout(() => void poll(), delay);
        return;
      }

      if (cancelled) return;
      try {
        const blob = await apiBlob(ticket.qrUrl);
        if (cancelled) return;
        objectUrl = URL.createObjectURL(blob);
        settle({ kind: "ready", ticket, qrUrl: objectUrl });
      } catch {
        settle({ kind: "ready", ticket, qrUrl: "" });
      }
    };

    void poll();
    return () => {
      cancelled = true;
      window.clearTimeout(timer);
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [bookingId, enabled, key]);

  const retry = useCallback(() => setRun((current) => current + 1), []);

  let state: TicketPollState;
  if (!enabled) state = { kind: "idle" };
  else if (result?.key === key) state = result.outcome;
  else state = { kind: "pending" };

  return { state, retry };
}
