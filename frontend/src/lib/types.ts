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
  status: "LOCKED" | "PAYMENT_PENDING" | "PAID" | "TICKET_ISSUED" | "CANCELLED" | "EXPIRED" | "REFUND_PENDING" | "REFUNDED";
  expiresAt?: string;
  seats: BookingSeat[];
};

export type Payment = {
  id: string;
  bookingId: string;
  paymentReference: string;
  amount: number;
  status: "PENDING" | "PROCESSING" | "SUCCEEDED" | "FAILED" | "REFUND_PENDING" | "REFUNDED";
  method: string;
  paidAt?: string;
  booking: Booking;
};

export type Dashboard = {
  movies: number;
  showtimes: number;
  bookings: number;
  paidBookings: number;
  failedPayments?: number;
  revenue: number;
  occupancyRate?: number;
};

export type SeatEvent = {
  type: "SEAT_LOCKED" | "SEAT_RELEASED" | "SEAT_BOOKED" | "SEAT_BLOCKED" | "SEAT_EXPIRED";
  showtimeId: string;
  seatId: string;
  seatCode: string;
  status: SeatAvailability["status"];
  price: number;
  expiresAt?: string | null;
};

export type Ticket = {
  id: string;
  bookingId: string;
  bookingCode: string;
  ticketCode: string;
  status: "ISSUED" | "USED" | "CANCELLED" | "EXPIRED";
  issuedAt: string;
  usedAt?: string;
  movieTitle: string;
  cinemaName: string;
  hallName: string;
  startTime: string;
  seats: BookingSeat[];
  qrUrl: string;
};

export type Notification = {
  id: string;
  type: string;
  title: string;
  message: string;
  read: boolean;
  readAt?: string | null;
  createdAt: string;
};

export type NotificationList = {
  unreadCount: number;
  notifications: Notification[];
};

export type AuditLog = {
  id: string;
  actorRole?: string;
  action: string;
  entityType: string;
  entityId?: string;
  oldValue?: string;
  newValue?: string;
  ipAddress?: string;
  createdAt: string;
};
