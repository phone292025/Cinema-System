import { act, cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import type { SeatAvailability } from "@/lib/types";
import { SeatPicker } from "./SeatPicker";

vi.mock("next/navigation", () => ({ useRouter: () => ({ push: vi.fn() }) }));

class FakeEventSource {
  static instances: FakeEventSource[] = [];
  onopen: (() => void) | null = null;
  onerror: (() => void) | null = null;
  closed = false;
  private listeners = new Map<string, Array<(event: MessageEvent) => void>>();

  constructor(readonly url: string) {
    FakeEventSource.instances.push(this);
  }

  addEventListener(name: string, listener: (event: MessageEvent) => void) {
    this.listeners.set(name, [...(this.listeners.get(name) ?? []), listener]);
  }

  emit(name: string, data: unknown) {
    this.listeners.get(name)?.forEach((listener) => listener(new MessageEvent(name, { data: JSON.stringify(data) })));
  }

  close() {
    this.closed = true;
  }
}

function seat(rowLabel: string, seatNumber: number, status: SeatAvailability["status"] = "AVAILABLE", price = 15): SeatAvailability {
  return { seatId: `${rowLabel}${seatNumber}`, rowLabel, seatNumber, seatType: "REGULAR", price, status };
}

const seats = [seat("A", 1), seat("A", 2, "AVAILABLE", 19), seat("A", 3, "BOOKED")];

describe("SeatPicker", () => {
  beforeEach(() => {
    FakeEventSource.instances = [];
    vi.stubGlobal("EventSource", FakeEventSource);
    vi.stubGlobal("fetch", vi.fn(() => Promise.resolve(new Response(JSON.stringify({ showtimeId: "s1", seats }), { status: 200 }))));
  });

  afterEach(() => {
    cleanup();
    vi.unstubAllGlobals();
    vi.useRealTimers();
  });

  it("totals the chosen seats and names them", () => {
    render(<SeatPicker showtimeId="s1" seats={seats} />);

    fireEvent.click(screen.getByRole("button", { name: "Seat A1, $15.00" }));
    fireEvent.click(screen.getByRole("button", { name: "Seat A2, $19.00" }));

    expect(screen.getAllByText("$34.00").length).toBeGreaterThan(0);
    expect(screen.getAllByText("A1, A2").length).toBeGreaterThan(0);
  });

  it("does not let a taken seat be chosen", () => {
    render(<SeatPicker showtimeId="s1" seats={seats} />);

    const taken = screen.getByRole("button", { name: "Seat A3, unavailable" }) as HTMLButtonElement;

    expect(taken.disabled).toBe(true);
  });

  it("drops a selected seat when someone else locks it", () => {
    render(<SeatPicker showtimeId="s1" seats={seats} />);
    fireEvent.click(screen.getByRole("button", { name: "Seat A1, $15.00" }));

    act(() => {
      FakeEventSource.instances[0].emit("SEAT_LOCKED", { seatId: "A1", status: "LOCKED", price: 15 });
    });

    expect((screen.getByRole("button", { name: "Seat A1, unavailable" }) as HTMLButtonElement).disabled).toBe(true);
    expect(screen.getByText(/Seat A1 was just taken/)).toBeTruthy();
  });

  it("reconnects after the live stream drops", () => {
    vi.useFakeTimers();
    render(<SeatPicker showtimeId="s1" seats={seats} />);

    act(() => {
      FakeEventSource.instances[0].onerror?.();
    });
    expect(screen.getByText(/reconnecting/)).toBeTruthy();

    act(() => {
      vi.advanceTimersByTime(1_000);
    });

    expect(FakeEventSource.instances).toHaveLength(2);
    expect(FakeEventSource.instances[0].closed).toBe(true);
  });
});
