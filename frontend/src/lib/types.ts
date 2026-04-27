export type User = {
  id: string;
  name: string;
  email: string;
  role: "CUSTOMER" | "ADMIN" | "STAFF";
};

export type AuthResponse = {
  accessToken: string;
  refreshToken: string;
  user: User;
};

export type Movie = {
  id: string;
  title: string;
  description: string;
  durationMinutes: number;
  genre: string;
  language: string;
  rating: string;
  posterUrl?: string;
  releaseDate: string;
  imdbRating?: number | null;
  status: "NOW_SHOWING" | "COMING_SOON" | "ARCHIVED";
};

export type Cinema = {
  id: string;
  name: string;
  location: string;
  address: string;
  city: string;
};

export type Showtime = {
  id: string;
  movieId: string;
  movieTitle: string;
  cinemaId: string;
  cinemaName: string;
  hallId: string;
  hallName: string;
  startTime: string;
  endTime: string;
  basePrice: number;
  status: "SCHEDULED" | "CANCELLED" | "SOLD_OUT";
};

export type SeatAvailability = {
  seatId: string;
  rowLabel: string;
  seatNumber: number;
  seatType: "REGULAR" | "PREMIUM" | "COUPLE" | "VIP";
  price: number;
  status: "AVAILABLE" | "LOCKED" | "BOOKED" | "BLOCKED";
  lockedUntil?: string;
};

export type SeatAvailabilityResponse = {
  showtimeId: string;
  seats: SeatAvailability[];
};

export type BookingSeat = {
  seatId: string;
  rowLabel: string;
  seatNumber: number;
  seatType: SeatAvailability["seatType"];
  price: number;
};

export type Booking = {
  id: string;
  bookingCode: string;
  showtimeId: string;
  movieTitle: string;
  cinemaName: string;
  hallName: string;
  startTime: string;
  totalAmount: number;
  status: "PENDING" | "LOCKED" | "PAID" | "CANCELLED" | "EXPIRED" | "REFUNDED";
  expiresAt?: string;
  seats: BookingSeat[];
};

export type Payment = {
  id: string;
  bookingId: string;
  paymentReference: string;
  amount: number;
  status: "PENDING" | "SUCCEEDED" | "FAILED" | "REFUNDED";
  method: string;
  paidAt?: string;
  booking: Booking;
};

export type Dashboard = {
  movies: number;
  showtimes: number;
  bookings: number;
  paidBookings: number;
  revenue: number;
};
