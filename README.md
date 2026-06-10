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

`make verify` runs the backend test suite, frontend lint, and frontend production build. Backend tests include a Testcontainers integration test for the booking, payment, ticket issuing, and staff validation flow.

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
- Frontend: Node.js 20, `npm ci`, lint, and production build.

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
