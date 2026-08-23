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

Fill `POSTGRES_PASSWORD`, `REDIS_PASSWORD`, `JWT_SECRET`, and `TICKET_SECRET` in `.env`. Use long random values for the JWT and ticket secrets. Keep `.env` private; it is ignored by git.

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

If Docker is not installed, run PostgreSQL and Redis locally first, set `DATABASE_PASSWORD`, `REDIS_PASSWORD`, `JWT_SECRET`, and `TICKET_SECRET`, then:

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

- Backend: Java 21, Maven tests, PostgreSQL and Redis through Testcontainers.
- Frontend: Node.js 20, `npm ci`, lint, Vitest unit tests, and production build.

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

The authoritative seat lock is the database: `lockSeats` takes a `SELECT ... FOR UPDATE` on the `showtime_seats` rows and checks their status inside that transaction. Redis holds a short-lived key per seat as a fast cross-instance hint, so two people browsing the same showtime stop seeing a seat as free almost immediately. A Redis outage therefore costs responsiveness, not correctness.

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

## Idempotency

Mutating requests carry an `Idempotency-Key`. For the operations where a duplicate costs money — locking seats, initiating a payment, the payment callback, cancelling a booking — the frontend keeps one key per logical operation in `sessionStorage` and reuses it across retries, then drops it once the operation succeeds. A retry after a dropped connection is therefore recognised by the backend as the same request instead of being processed twice.

## Troubleshooting

**Backend exits with `password authentication failed for user "cinema"`.**
`POSTGRES_PASSWORD` is only applied the first time the `postgres-data` volume is initialised. If the password in `.env` is changed afterwards, the database keeps the old one and the backend can no longer connect. Note that `psql` from inside the container still works, because the image trusts local and `127.0.0.1` connections; the backend connects from another host and has to authenticate.

Sync the role with the current `.env` value, keeping existing data:

```bash
docker compose exec postgres psql -U cinema -d cinema -c "ALTER USER cinema WITH PASSWORD 'the-value-from-.env'"
```

Then restart the backend with `docker compose up -d backend`. Alternatively `docker compose down -v` recreates the volume from scratch, which discards all local data.

**The browser shows `ERR_CONNECTION_REFUSED` on `localhost:3000`.**
Ports are published on `127.0.0.1` only. If `localhost` resolves to the IPv6 address `::1`, use `http://127.0.0.1:3000`.

## Known Limitations

Access and refresh tokens are kept in `localStorage`, which is readable by any script running on the page. Moving the refresh token into an `HttpOnly` cookie would remove that exposure, at the cost of adding CSRF protection and reworking how the frontend authenticates. It is a deliberate follow-up rather than an oversight.
