import { Building2, CalendarPlus, Film, LayoutDashboard, Rows3, ShieldCheck, TicketCheck } from "lucide-react";
import type { ReactNode } from "react";

import type { SeatType } from "@/lib/types";

export const MAX_HALL_ROWS = 26;
export const MAX_HALL_COLUMNS = 50;

export type AdminSection = "dashboard" | "tickets" | "movies" | "cinemas" | "halls" | "showtimes" | "audit";

export const sections: Array<{
  key: AdminSection;
  label: string;
  eyebrow: string;
  helper: string;
  icon: ReactNode;
}> = [
  {
    key: "dashboard",
    label: "Dashboard",
    eyebrow: "Control room",
    helper: "Operational totals, health, and setup progress.",
    icon: <LayoutDashboard size={18} aria-hidden />,
  },
  {
    key: "tickets",
    label: "Ticket sales",
    eyebrow: "Revenue desk",
    helper: "Paid bookings, revenue, and session demand.",
    icon: <TicketCheck size={18} aria-hidden />,
  },
  {
    key: "movies",
    label: "Movie management",
    eyebrow: "Catalog",
    helper: "Poster, title, description, duration, genre, language, rating, date, and status.",
    icon: <Film size={18} aria-hidden />,
  },
  {
    key: "cinemas",
    label: "Cinema management",
    eyebrow: "Branches",
    helper: "Branch name, location, address, and city.",
    icon: <Building2 size={18} aria-hidden />,
  },
  {
    key: "halls",
    label: "Halls and seats",
    eyebrow: "Seat layout",
    helper: "Hall type, rows, columns, and automatic seat generation.",
    icon: <Rows3 size={18} aria-hidden />,
  },
  {
    key: "showtimes",
    label: "Showtime management",
    eyebrow: "Scheduling",
    helper: "Schedule movies into halls with start time and base price.",
    icon: <CalendarPlus size={18} aria-hidden />,
  },
  {
    key: "audit",
    label: "Audit logs",
    eyebrow: "Safety trail",
    helper: "Review payment, booking, ticket, and admin actions.",
    icon: <ShieldCheck size={18} aria-hidden />,
  },
];

export const emptyMovieForm = {
  title: "",
  description: "",
  durationMinutes: "120",
  genre: "Drama",
  language: "English",
  rating: "PG-13",
  imdbRating: "",
  posterUrl: "",
  releaseDate: "",
  status: "NOW_SHOWING",
};

export type MovieForm = typeof emptyMovieForm;

export type CinemaForm = { name: string; location: string; address: string; city: string };

export type HallForm = {
  cinemaId: string;
  name: string;
  type: string;
  totalRows: string;
  totalColumns: string;
  defaultSeatType: SeatType;
};

export type ShowtimeForm = { movieId: string; hallId: string; startTime: string; basePrice: string };

export const seatTypeOptions: Array<{ value: SeatType; label: string }> = [
  { value: "REGULAR", label: "Regular" },
  { value: "PREMIUM", label: "Premium" },
  { value: "VIP", label: "VIP" },
  { value: "COUPLE", label: "Couple" },
];

/** Tomorrow at 18:00 local time, formatted for a datetime-local input. */
export function defaultShowtimeStart(now = new Date()) {
  const start = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1, 18, 0);
  const pad = (value: number) => String(value).padStart(2, "0");
  return `${start.getFullYear()}-${pad(start.getMonth() + 1)}-${pad(start.getDate())}T18:00`;
}
