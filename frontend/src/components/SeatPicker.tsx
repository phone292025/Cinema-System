"use client";

import { Armchair, Loader2, LockKeyhole, TicketCheck } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
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

  const total = liveSeats
    .filter((seat) => selected.includes(seat.seatId))
    .reduce((sum, seat) => sum + Number(seat.price), 0);

  function toggle(seat: SeatAvailability) {
    if (seat.status !== "AVAILABLE") return;
    setSelected((current) =>
      current.includes(seat.seatId) ? current.filter((id) => id !== seat.seatId) : [...current, seat.seatId],
    );
  }

  async function lockSeats() {
    setError("");
    if (demoMode) {
      setError("This chair map is a local preview. Start the Spring Boot API to lock seats and continue to payment.");
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
            {Object.entries(grouped).map(([row, rowSeats]) => (
              <div
                key={row}
                className="grid items-center gap-1.5 sm:gap-2"
                style={{ gridTemplateColumns: `1.25rem repeat(${rowSeats.length}, minmax(0, 1fr))` }}
              >
                <span className="text-xs font-semibold text-muted sm:text-sm">{row}</span>
                {rowSeats.map((seat) => {
                  const active = selected.includes(seat.seatId);
                  const disabled = seat.status !== "AVAILABLE";
                  return (
                    <button
                      key={seat.seatId}
                      type="button"
                      onClick={() => toggle(seat)}
                      disabled={disabled}
                      title={`${seat.rowLabel}${seat.seatNumber} ${seat.status}${seat.lockedUntil ? ` until ${new Date(seat.lockedUntil).toLocaleTimeString()}` : ""}`}
                      className={`grid aspect-square w-full min-w-0 place-items-center rounded-md border text-xs font-semibold ${
                        active
                          ? "border-accent bg-accent text-background"
                          : disabled
                            ? "border-line bg-background text-muted opacity-45"
                            : "border-line bg-background text-foreground hover:border-accent"
                      }`}
                    >
                      <Armchair className="size-[clamp(0.875rem,4vw,1.1rem)]" aria-hidden />
                    </button>
                  );
                })}
              </div>
            ))}
          </div>
        </div>
        <div className="mt-6 flex flex-wrap gap-x-4 gap-y-2 text-xs text-muted">
          <span className="flex items-center gap-2">
            <span className="size-3 rounded-sm border border-line bg-background" /> Available
          </span>
          <span className="flex items-center gap-2">
            <span className="size-3 rounded-sm bg-accent" /> Selected
          </span>
          <span className="flex items-center gap-2">
            <span className="size-3 rounded-sm bg-muted/35" /> Locked / booked
          </span>
        </div>
      </div>

      <aside className="min-w-0 rounded-lg border border-line bg-panel p-5">
        <div className="mb-4 flex items-center gap-2 text-sm font-semibold text-accent">
          <LockKeyhole size={17} aria-hidden />
          Temporary lock
        </div>
        <p className="text-sm leading-6 text-muted">
          Selected seats are locked for 5 minutes after you continue. The backend rechecks Redis and PostgreSQL before payment.
        </p>
        <div className="my-5 border-t border-line" />
        <div className="space-y-2 text-sm">
          <div className="flex justify-between">
            <span className="text-muted">Seats</span>
            <span>{selected.length}</span>
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
          className="mt-5 flex w-full items-center justify-center gap-2 rounded-md bg-accent px-4 py-3 text-sm font-semibold text-background disabled:cursor-not-allowed disabled:opacity-50"
        >
          {loading ? <Loader2 className="animate-spin" size={18} aria-hidden /> : <TicketCheck size={18} aria-hidden />}
          Lock seats
        </button>
      </aside>
    </section>
  );
}
