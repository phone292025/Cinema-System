# Cinema MVP

Greenfield cinema booking MVP using a modular monolith backend and a Next.js frontend.

## Stack

- Frontend: Next.js App Router, TypeScript, Tailwind CSS
- Backend: Spring Boot 3.5, Java 21, Spring Security, Spring Data JPA
- Data: PostgreSQL with Flyway migrations
- Locks: Redis temporary seat locks with 5-minute TTL
- Payments: mock gateway with idempotent callback handling

## Local Run

For normal daily startup, use the Codex-safe lighter command:

```powershell
.\start-codex-safe.ps1
```

This reuses existing Docker images, avoids rebuilding everything every time, and sends noisy Docker output to `logs/start-codex-safe.log` instead of flooding the Codex app.

Only rebuild after dependency, Dockerfile, or major source changes:

```powershell
.\rebuild-once.ps1
```

Avoid running `docker compose up --build` directly inside Codex unless you really need to watch the raw logs. Large Docker build output can make the Codex desktop app look like it is reconnecting or not responding.

Stop everything:

```powershell
.\stop.ps1
```

The project ignores generated folders such as `frontend/node_modules`, `frontend/.next`, and `backend/target` during Docker builds, so recreating those folders locally will not make Docker send them into the build context.

If Docker is not installed, run PostgreSQL and Redis locally first, then:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

```powershell
cd frontend
npm.cmd run dev
```

Open `http://localhost:3000`.

## Seeded Accounts

- Customer: `customer@cinema.test` / `customer12345`
- Admin: `admin@cinema.test` / `admin12345`

## Main Flow

1. Log in as the seeded customer.
2. Open Movies, pick a showtime, and select available seats.
3. Lock seats; Redis stores keys shaped like `lock:showtime:{showtimeId}:seat:{seatId}`.
4. Pay with the mock gateway.
5. View the confirmed booking in history.

The backend revalidates seat state before locking and before confirming payment.
