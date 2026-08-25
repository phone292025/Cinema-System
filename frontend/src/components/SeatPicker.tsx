"use client";

import { Loader2, LockKeyhole, TicketCheck } from "lucide-react";
import { Fragment, useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";

import { API_BASE, apiFetch, getAccessToken } from "@/lib/api";
import type { Booking, SeatAvailability, SeatEvent } from "@/lib/types";

type Props = {
  showtimeId: string;
  seats: SeatAvailability[];
  demoMode?: boolean;
};

export function SeatPicker({ showtimeId, seats, demoMode = false }: Props) {
  const [seatState, setSeatState] = useState({
    showtimeId,
    sourceSeats: seats,
    liveSeats: seats,
  });
  const [selected, setSelected] = useState<string[]>([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const router = useRouter();

  const liveSeats = seatState.showtimeId === showtimeId && seatState.sourceSeats === seats ? seatState.liveSeats : seats;

  useEffect(() => {
    if (demoMode) return undefined;
    const source = new EventSource(`${API_BASE}/showtimes/${showtimeId}/seat-events`);
    const apply = (message: MessageEvent) => {
      const event = JSON.parse(message.data) as SeatEvent;
      setSeatState((current) => {
        const currentSeats = current.showtimeId === showtimeId && current.sourceSeats === seats ? current.liveSeats : seats;
        return {
          showtimeId,
          sourceSeats: seats,
          liveSeats: currentSeats.map((seat) =>
            seat.seatId === event.seatId
              ? { ...seat, status: event.status, price: event.price, lockedUntil: event.expiresAt ?? undefined }
              : seat,
          ),
        };
      });
      if (event.status !== "AVAILABLE") {
        setSelected((current) => current.filter((seatId) => seatId !== event.seatId));
      }
    };
    ["SEAT_LOCKED", "SEAT_RELEASED", "SEAT_BOOKED", "SEAT_BLOCKED", "SEAT_EXPIRED"].forEach((name) =>
      source.addEventListener(name, apply),
    );
    source.onerror = () => {
      source.close();
    };
    return () => source.close();
  }, [demoMode, seats, showtimeId]);

  const grouped = useMemo(() => {
    return liveSeats.reduce<Record<string, SeatAvailability[]>>((acc, seat) => {
      acc[seat.rowLabel] ??= [];
      acc[seat.rowLabel].push(seat);
      return acc;
    }, {});
  }, [liveSeats]);

  const selectedSeats = liveSeats.filter((seat) => selected.includes(seat.seatId));
  const total = selectedSeats.reduce((sum, seat) => sum + Number(seat.price), 0);
  const selectedLabels = selectedSeats
    .slice()
    .sort((a, b) => a.rowLabel.localeCompare(b.rowLabel) || a.seatNumber - b.seatNumber)
    .map((seat) => `${seat.rowLabel}${seat.seatNumber}`);

  function toggle(seat: SeatAvailability) {
    if (seat.status !== "AVAILABLE") return;
    setSelected((current) =>
      current.includes(seat.seatId) ? current.filter((id) => id !== seat.seatId) : [...current, seat.seatId],
    );
  }

  async function lockSeats() {
    setError("");
    if (demoMode) {
      setError("This is a local preview of the seat map. Connect the booking service to hold seats and continue to payment.");
      return;
    }
    if (!getAccessToken()) {
      router.push(`/login?next=${encodeURIComponent(`/showtimes/${showtimeId}/seats`)}`);
      return;
    }
    setLoading(true);
    try {
      const booking = await apiFetch<Booking>("/bookings/lock-seats", {
        method: "POST",
        body: JSON.stringify({ showtimeId, seatIds: selected }),
        idempotencyScope: `lock-seats:${showtimeId}:${[...selected].sort().join(",")}`,
      });
      router.push(`/checkout/${booking.id}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not lock seats.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <section className="grid min-w-0 gap-6 lg:grid-cols-[minmax(0,1fr)_320px]">
      <div className="min-w-0 overflow-hidden rounded-lg border border-line bg-panel p-3 sm:p-5">
        <div className="mb-5 rounded-md border border-line bg-background px-4 py-3 text-center text-xs font-semibold uppercase text-muted">
          Screen
        </div>
        <div className="pb-2">
          <div className="mx-auto w-full max-w-[620px] space-y-2 sm:space-y-3">
            {Object.entries(grouped).map(([row, rowSeats]) => {
              const aisleAfter = Math.ceil(rowSeats.length / 2);
              return (
                <div
                  key={row}
                  className="grid items-center gap-1.5 sm:gap-2"
                  style={{
                    gridTemplateColumns: `1.25rem repeat(${aisleAfter}, minmax(0, 1fr)) 0.6rem repeat(${
                      rowSeats.length - aisleAfter
                    }, minmax(0, 1fr))`,
                  }}
                >
                  <span className="text-xs font-semibold text-muted sm:text-sm">{row}</span>
                  {rowSeats.map((seat, index) => {
                    const active = selected.includes(seat.seatId);
                    const disabled = seat.status !== "AVAILABLE";
                    return (
                      <Fragment key={seat.seatId}>
                        {index === aisleAfter && <span aria-hidden />}
                        <button
                          type="button"
                          onClick={() => toggle(seat)}
                          disabled={disabled}
                          aria-label={`Seat ${seat.rowLabel}${seat.seatNumber}, ${
                            disabled ? "unavailable" : `$${Number(seat.price).toFixed(2)}`
                          }`}
                          aria-pressed={active}
                          title={`${seat.rowLabel}${seat.seatNumber} · $${Number(seat.price).toFixed(2)}${
                            disabled ? " · taken" : ""
                          }`}
                          className={`grid aspect-square w-full min-w-0 place-items-center rounded-md border text-[0.65rem] font-semibold leading-none sm:text-xs ${
                            active
                              ? "border-accent bg-accent text-background"
                              : disabled
                                ? "border-line bg-background text-muted opacity-45"
                                : "border-line bg-background text-foreground hover:border-accent"
                          }`}
                        >
                          {seat.seatNumber}
                        </button>
                      </Fragment>
                    );
                  })}
                </div>
              );
            })}
          </div>
        </div>
        <div className="mt-6 flex flex-wrap gap-x-5 gap-y-2 text-sm text-muted">
          <span className="flex items-center gap-2">
            <span className="size-4 rounded-sm border border-line bg-background" /> Available
          </span>
          <span className="flex items-center gap-2">
            <span className="size-4 rounded-sm bg-accent" /> Your pick
          </span>
          <span className="flex items-center gap-2">
            <span className="size-4 rounded-sm bg-muted/35" /> Taken
          </span>
        </div>
      </div>

      <aside className="min-w-0 rounded-lg border border-line bg-panel p-5 max-md:pb-0">
        <div className="mb-4 flex items-center gap-2 text-sm font-semibold text-accent">
          <LockKeyhole size={17} aria-hidden />
          Your seats are held for 5 minutes
        </div>
        <p className="text-sm leading-6 text-muted">
          Once you continue, nobody else can take these seats while you pay. If the 5 minutes run out, they go back on sale.
        </p>
        <div className="my-5 border-t border-line" />
        <div className="space-y-2 text-sm">
          <div className="flex justify-between gap-4">
            <span className="text-muted">Seats</span>
            <span className="text-right font-medium">{selectedLabels.length > 0 ? selectedLabels.join(", ") : "None yet"}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-muted">Total</span>
            <span className="font-semibold text-accent">${total.toFixed(2)}</span>
          </div>
        </div>
        {error && <p className="mt-4 rounded-md border border-danger/40 bg-danger/10 p-3 text-sm text-danger">{error}</p>}
        <button
          type="button"
          disabled={selected.length === 0 || loading}
          onClick={lockSeats}
          className="mt-5 hidden w-full items-center justify-center gap-2 rounded-md bg-accent px-4 py-3 text-sm font-semibold text-background disabled:cursor-not-allowed disabled:opacity-50 md:flex"
        >
          {loading ? <Loader2 className="animate-spin" size={18} aria-hidden /> : <TicketCheck size={18} aria-hidden />}
          Continue to payment
        </button>
      </aside>

      {/* On a phone the summary sits below the fold, so repeat the total and the
          action just above the tab bar where a thumb already is. */}
      <div className="fixed inset-x-0 bottom-[4.75rem] z-20 border-t border-line bg-background/95 px-4 py-3 backdrop-blur md:hidden">
        <div className="flex items-center justify-between gap-4">
          <div className="min-w-0">
            <p className="truncate text-sm text-muted">{selectedLabels.length > 0 ? selectedLabels.join(", ") : "Pick your seats"}</p>
            <p className="text-lg font-semibold text-accent">${total.toFixed(2)}</p>
          </div>
          <button
            type="button"
            disabled={selected.length === 0 || loading}
            onClick={lockSeats}
            className="flex shrink-0 items-center justify-center gap-2 rounded-md bg-accent px-5 py-3 text-sm font-semibold text-background disabled:cursor-not-allowed disabled:opacity-50"
          >
            {loading ? <Loader2 className="animate-spin" size={18} aria-hidden /> : <TicketCheck size={18} aria-hidden />}
            Continue
          </button>
        </div>
      </div>
    </section>
  );
}
