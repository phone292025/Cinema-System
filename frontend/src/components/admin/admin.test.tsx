import { cleanup, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it } from "vitest";

import type { Showtime } from "@/lib/types";
import { defaultShowtimeStart } from "./model";
import { TicketSalesWorkspace } from "./TicketSalesWorkspace";

function showtime(overrides: Partial<Showtime>): Showtime {
  return {
    id: "s1",
    movieId: "m1",
    movieTitle: "The Godfather",
    cinemaId: "c1",
    cinemaName: "Central Cineplex",
    hallId: "h1",
    hallName: "Hall 1",
    startTime: "2026-10-01T18:00:00Z",
    endTime: "2026-10-01T21:00:00Z",
    basePrice: 15,
    status: "SCHEDULED",
    soldSeats: 2,
    totalSeats: 48,
    revenue: 34,
    ...overrides,
  };
}

describe("TicketSalesWorkspace", () => {
  afterEach(cleanup);

  it("shows what a session actually took, including premium surcharges", () => {
    render(<TicketSalesWorkspace dashboard={null} showtimes={[showtime({})]} revenue={34} paidBookings={1} averageTicket={34} />);

    expect(screen.getByText(/\$34\.00 sold/)).toBeTruthy();
  });

  it("treats a session without paid bookings as zero revenue", () => {
    render(
      <TicketSalesWorkspace
        dashboard={null}
        showtimes={[showtime({ soldSeats: 0, revenue: null })]}
        revenue={0}
        paidBookings={0}
        averageTicket={0}
      />,
    );

    expect(screen.getByText(/\$0\.00 sold/)).toBeTruthy();
  });
});

describe("defaultShowtimeStart", () => {
  it("suggests tomorrow evening so a new session is never in the past", () => {
    expect(defaultShowtimeStart(new Date(2026, 8, 30, 23, 30))).toBe("2026-10-01T18:00");
  });
});
