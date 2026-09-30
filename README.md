# Cinema System MVP+

Cinema booking system using a modular monolith backend and a Next.js frontend. It includes live seat locking, mock payments, QR tickets, staff validation, notifications, audit logs, and admin reporting.

## Stack

- Frontend: Next.js App Router, TypeScript, Tailwind CSS
- Backend: Spring Boot 3.5, Java 21, Spring Security, Spring Data JPA
- Data: PostgreSQL with Flyway migrations
- Locks: Redis temporary seat locks with 5-minute TTL
- Payments: mock gateway with idempotent callback handling

## Local Run

Create a private local environment file before starting Docker Compose:

```bash
cp example.env .env
```

Fill `POSTGRES_PASSWORD`, `REDIS_PASSWORD`, `JWT_SECRET`, and `TICKET_SECRET` in `.env` (for example with `openssl rand -base64 48`). The backend refuses to start if either secret is shorter than 32 characters, looks like a placeholder, or has too little variety. Keep `.env` private; it is ignored by git. `example.env` documents every other setting.

Cross-platform Docker commands for Linux, macOS, CI runners, and Windows shells with `make` installed:

```bash
make up
```

This reuses existing Docker images and avoids rebuilding everything every time.

Only rebuild after dependency, Dockerfile, or major source changes:

```bash
make rebuild
```

Stop everything:

```bash
make down
```

Other useful commands:

```bash
make ps
make logs
make health   # readiness probe of the running backend
make config   # validate docker-compose.yml against your .env
make verify
```

`make verify` runs the backend test suite, frontend lint, frontend unit tests, and the frontend production build. Backend tests include Testcontainers integration tests for the booking, payment, ticket issuing, and staff validation flow, and for concurrent seat locking.

Windows PowerShell helpers are also available:

```powershell
.\start-light.ps1
```

```powershell
.\rebuild-once.ps1
```

```powershell
.\stop.ps1
```

The project ignores generated folders such as `frontend/node_modules`, `frontend/.next`, and `backend/target` during Docker builds, so recreating those folders locally will not make Docker send them into the build context.

If Docker is not installed, run PostgreSQL and Redis locally first, set `DATABASE_PASSWORD`, `REDIS_PASSWORD`, `JWT_SECRET`, and `TICKET_SECRET` (plus `SEED_CATALOG_ENABLED=true` if you want the sample catalogue; see the end of `example.env`), then:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

```powershell
cd frontend
npm.cmd run dev
```

Open `http://localhost:3000`.

## CI

GitHub Actions runs on every push to `main` and every pull request:

- Backend: Java 21, Maven tests, PostgreSQL and Redis through Testcontainers. Surefire reports are uploaded when a run fails.
- Frontend: Node.js 24, `npm ci`, lint, Vitest + React Testing Library tests, and production build.
- Docker: validates `docker-compose.yml` and builds both images (with layer caching).

A newer push to the same branch cancels the run already in progress.

## Optional Demo Accounts

Demo user accounts are disabled by default. To create local demo accounts, set these private `.env` values before starting the backend:

```bash
DEMO_USERS_ENABLED=true
DEMO_ADMIN_EMAIL=admin@cinema.test
DEMO_ADMIN_PASSWORD=<long-private-admin-password>
DEMO_CUSTOMER_EMAIL=customer@cinema.test
DEMO_CUSTOMER_PASSWORD=<long-private-customer-password>
```

There is no separate default staff account. A demo admin account can also open the staff ticket validation screen when demo users are explicitly enabled.

## Main Flow

1. Log in as your local demo customer, or register a new customer.
2. Open Movies, pick a showtime, and select available seats.
3. Lock seats; Redis stores keys shaped like `lock:showtime:{showtimeId}:seat:{seatId}`.
4. Pay with the mock gateway.
5. Wait for the outbox worker to issue a QR ticket.
6. View the confirmed booking and ticket in history.
7. Log in as admin/staff and validate the ticket from the staff screen by scanning the QR token.

The backend revalidates seat state before locking and before confirming payment.

## Seeded Staff/Admin Areas

- Staff ticket validation: `http://localhost:3000/staff`
- Admin dashboard: `http://localhost:3000/admin`

## Seat Locking and Concurrency

The authoritative seat lock is the database: `lockSeats` takes a `SELECT ... FOR UPDATE` on the `showtime_seats` rows and checks their status inside that transaction. Redis holds a short-lived key per seat as a fast cross-instance hint, so two people browsing the same showtime stop seeing a seat as free almost immediately. A Redis outage therefore costs responsiveness, not correctness: seat locking carries on with the database row locks alone and logs a warning at most once a minute until Redis is back.

Every seat row records `locked_by_booking_id`, and both the database and the Redis release paths compare against it:

- A booking only ever frees a seat it still holds. An expiring booking cannot release a seat that a newer booking has taken over after the earlier hold timed out.
- Redis releases run as a compare-and-delete Lua script, so a booking cannot delete another booking's key.

`SeatLockConcurrencyIntegrationTest` covers both: eight concurrent requests for one seat produce exactly one booking, and an expiring booking leaves a newer booking's seat alone.

## Rate Limits

Login, registration, refresh, and the payment endpoints are rate limited per client IP with counters in Redis, so the limit holds across replicas. If Redis is unreachable the limiter fails open rather than blocking sign-ins. Defaults, all overridable through the environment:

```bash
RATE_LIMIT_ENABLED=true
RATE_LIMIT_AUTH_PER_MINUTE=10
RATE_LIMIT_PAYMENT_PER_MINUTE=30
# Enable only behind a proxy that overwrites X-Forwarded-For; otherwise clients can spoof it.
RATE_LIMIT_TRUST_FORWARDED_FOR=false
```

## Running More Than One Backend

Scheduled jobs (booking expiry, seat lock sweeps, outbox dispatch, ticket repair, refresh token and idempotency cleanup) are wrapped in a Postgres transaction-level advisory lock, so with several backend replicas each run happens on one instance only. No extra table or dependency is involved, and the lock is released automatically when the transaction ends.

Live seat updates work across replicas too. After a seat change commits, the event is published to the Redis channel `cinema:seat-events`, and every instance forwards it to the browsers connected to it over Server-Sent Events. If Redis is unavailable, an instance still delivers its own events locally. Each stream sends a heartbeat comment every 20 seconds so proxies keep idle connections open. Each showtime accepts up to 500 live viewers per instance (`SEAT_EVENTS_MAX_SUBSCRIBERS_PER_SHOWTIME`).

## Idempotency

Mutating requests carry an `Idempotency-Key`. For the operations where a duplicate costs money — locking seats, initiating a payment, the payment callback, cancelling a booking — the frontend keeps one key per logical operation in `sessionStorage` and reuses it across retries, then drops it once the operation succeeds. A retry after a dropped connection is therefore recognised by the backend as the same request instead of being processed twice. A replay returns the original response with its original status code. Two simultaneous first uses of one key get a `409` rather than both running. Completed keys are kept for 24 hours.

## Payments

The demo checkout uses a mock gateway: the browser starts a payment and then reports its result to `POST /payments/mock-callback`. That means a signed-in customer can confirm their own payment, so the mock is controlled by `PAYMENT_MOCK_GATEWAY_ENABLED`. It is on by default, logs a warning at startup, and returns `404` when switched off. Turn it off anywhere real money is involved.

A real gateway reports results to `POST /payments/webhook` with the same body (`{"paymentReference": "...", "status": "SUCCEEDED" | "FAILED"}`). The request is authenticated by an `X-Payment-Signature: sha256=<hex HMAC-SHA256 of the raw body>` header, using `PAYMENT_WEBHOOK_SECRET`. A missing or wrong signature gets `401`; with no secret configured the endpoint returns `404`. The webhook is exempt from the per-IP payment rate limit, and repeated deliveries are idempotent. Both paths apply the result through the same code.

A payment that arrives after the five-minute hold has lapsed is recorded as failed and the booking as expired before the error is returned. The same applies when a staff member scans a ticket for a session that has already ended.

## Ticket Issuing

Tickets are issued from a transactional outbox: the payment and its `BOOKING_PAID` event commit together. The worker then runs every 5 seconds and handles each event in its own transaction, so one bad event cannot hold up the others.

- **Retries.** A failing event is retried with exponential backoff (30 seconds, doubling) and marked `FAILED` after 5 attempts. Failures are logged; each attempt is logged at WARN and the final failure at ERROR. Both are tunable through `OUTBOX_MAX_ATTEMPTS` and `OUTBOX_RETRY_BACKOFF_SECONDS`.
- **Cancelled before issue.** A booking cancelled between payment and ticket issue is simply skipped.
- **Repair job.** A repair job re-queues paid bookings that have neither a ticket nor an open event. It does not re-queue events that already failed permanently, since those need a person to look at the logs.

## Refunds and Reporting

Cancelling a paid booking (at least `BOOKING_CANCEL_CUTOFF_HOURS` before the show) marks the booking `REFUNDED` and its payment `REFUNDED`, so refunds no longer count as revenue. The customer also gets a notification saying how much was refunded.

Booking responses include `cancellableUntil`:

- For unpaid holds it is the start of the show.
- For paid bookings it is the start of the show minus the cutoff.
- It is null once the booking can no longer be cancelled.

The bookings page only offers Cancel inside that window and otherwise says when cancellation closed.

The admin dashboard is computed with SQL aggregates rather than by loading tables into memory. It shows revenue, paid bookings, failed payments, occupancy, daily revenue and the top films. Per-session revenue is the sum of the seat prices actually sold, so premium surcharges are included.

Business dates use `APP_TIME_ZONE` (default `UTC`). That covers the daily revenue buckets and the staff console's "today's sessions". Set it to the cinema's own zone, for example `APP_TIME_ZONE=Asia/Singapore` for the sample Central Cineplex. Otherwise sessions after midnight local time still count as "today".

## Seat Pricing

Premium seats cost the session's base price plus `PRICING_PREMIUM_SURCHARGE` (default `4.00`), both for sessions created in the admin console and for the seeded sample schedule.

## Sample Data

With `SEED_CATALOG_ENABLED=true` (the Compose default; the application itself defaults to `false`), startup inserts the sample catalogue, cinema, hall and seats if they are missing. It never overwrites existing movies, so edits and archiving done in the admin console survive restarts. Films without upcoming sessions get three each, scheduled back to back in the hall with a 20-minute turnaround, so the sample schedule never double-books the hall.

## Health, Logs and Errors

- **Health.** `GET /api/actuator/health/liveness` and `GET /api/actuator/health/readiness` are public; readiness checks the database. Compose uses readiness as the backend healthcheck and starts the frontend only once the backend is healthy. No other actuator endpoint is exposed.
- **Request IDs.** Every response carries an `X-Request-Id`. A sane inbound value is reused, otherwise one is generated. The ID is written into every log line for that request and included in JSON error bodies.
- **Errors.** Errors use one JSON shape (`timestamp`, `status`, `error`, `message`, `requestId`):
  - `401` for a missing or expired token, which is what triggers the frontend's token refresh.
  - `403` for a forbidden action.
  - `400` for malformed JSON, invalid fields or bad IDs.
  - `404` for unknown routes.
  - `405` / `415` for unsupported methods or content types.
  - `409` for conflicts such as constraint violations. For example, deleting a movie that has showtimes explains that it should be archived instead.
  - Unexpected failures are logged with their stack trace and return a generic `500`.

## Troubleshooting

**Backend exits with `password authentication failed for user "cinema"`.**
`POSTGRES_PASSWORD` is only applied the first time the `postgres-data` volume is initialised. If the password in `.env` is changed afterwards, the database keeps the old one and the backend can no longer connect. Note that `psql` from inside the container still works, because the image trusts local and `127.0.0.1` connections; the backend connects from another host and has to authenticate.

Sync the role with the current `.env` value, keeping existing data:

```bash
docker compose exec postgres psql -U cinema -d cinema -c "ALTER USER cinema WITH PASSWORD 'the-value-from-.env'"
```

Then restart the backend with `docker compose up -d backend`. Alternatively `docker compose down -v` recreates the volume from scratch, which discards all local data.

**The frontend container stays in `Created` state.**
It waits for the backend healthcheck. Run `make health` or `docker compose logs backend` to see why the backend is not ready yet; the first start runs all migrations and can take a minute.

**The browser shows `ERR_CONNECTION_REFUSED` on `localhost:3000`.**
Ports are published on `127.0.0.1` only. If `localhost` resolves to the IPv6 address `::1`, use `http://127.0.0.1:3000`.

## Known Limitations

Access and refresh tokens are kept in `localStorage`, which is readable by any script running on the page. Moving the refresh token into an `HttpOnly` cookie would remove that exposure, at the cost of adding CSRF protection and reworking how the frontend authenticates. It is a deliberate follow-up rather than an oversight.

Refresh tokens rotate on every use. Presenting one that was already rotated is treated as theft: every active refresh token for that user is revoked, so they sign in again on all devices.

The mock payment gateway is enabled by default so the demo works out of the box; see [Payments](#payments).

## Booking Experience

- **The hold is visible.** Checkout shows a live `mm:ss` countdown of the five minute seat hold, turns red under a minute, and blocks payment once it lapses.
- **The ticket arrives on its own.** Tickets are issued by a background worker a moment after payment, so the confirmation page shows a "preparing your ticket" state and swaps in the QR code as soon as it exists. No refresh.
- **The seat map reads like an auditorium.** Seats show their number, rows are split by a centre aisle, and the chosen seats are named ("B1, B2") before you pay. On a phone the running total and the continue button stay pinned above the tab bar.
- **Bookings are split into upcoming and past.** Cancelling is a labelled button with a confirmation step, and it is not offered for screenings that have already happened.
- **Notifications link to the ticket** they are about, and unread ones are visually distinct.
- **The live seat map heals itself.** If the update stream drops, the page says "reconnecting", retries with backoff, and re-reads the seat map when it is back. Seats someone else took in the meantime are removed from your selection, with a note saying which.
- **Outages are visible.** If the API cannot be reached, pages say so and offer Retry instead of quietly showing sample data.
- **Sessions last.** When the 30-minute access token expires, the app refreshes it silently. If the session really is over, you are taken to sign-in and returned to the same page afterwards.

## Staff Console

Tickets are scanned with the camera through the browser's built-in `BarcodeDetector`, so there is no scanning library in the bundle. A scan validates immediately. Where the API is missing the console says so and falls back to the text field, which also accepts a USB barcode scanner (it types the code and presses Enter). The field clears and keeps focus after every scan, so the next ticket can be scanned straight away. A rejected ticket (already used, cancelled, or for a show that has ended) is explained inside the scan panel. Today's sessions sit beside the scanner rather than below it.

## Admin Workspace

The dashboard opens on data rather than a hero, and each figure appears once. Sessions carry real sales: seats sold against capacity, percentage full, and revenue per session (including premium surcharges), ordered by demand. A new hall is created together with its seat layout in one request, so a hall can never exist without seats. The catalog can be edited and archived, not just added to, and long lists (the schedule runs to hundreds of rows) are searchable and load in pages. Admin changes are written to the audit log with the admin who made them. This covers movies, cinemas, halls and showtimes, next to the booking, payment and ticket events.

Scheduling rejects a session that overlaps another in the same hall, so one auditorium can no longer show two films at once. Creation is serialised per hall with a row lock, so two admins booking the same slot at the same moment cannot both succeed. Sessions must start in the future and run at most 12 hours.
