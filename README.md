# Cinema System MVP+

Cinema booking system using a modular monolith backend and a Next.js frontend. It includes live seat locking, mock payments, QR tickets, staff validation, notifications, audit logs, and admin reporting.

## Stack

- Frontend: Next.js App Router, TypeScript, Tailwind CSS
- Backend: Spring Boot 3.5, Java 21, Spring Security, Spring Data JPA
- Data: PostgreSQL with Flyway migrations
- Locks: Redis temporary seat locks with 5-minute TTL
- Payments: mock gateway with idempotent callback handling

## Local Run

For normal daily startup, use the lightweight command:

```powershell
.\start-light.ps1
```

This reuses existing Docker images and avoids rebuilding everything every time.

Only rebuild after dependency, Dockerfile, or major source changes:

```powershell
.\rebuild-once.ps1
```

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
5. Wait for the outbox worker to issue a QR ticket.
6. View the confirmed booking and ticket in history.
7. Log in as admin/staff and validate the ticket from the staff screen.

The backend revalidates seat state before locking and before confirming payment.

## Seeded Staff/Admin Areas

- Staff ticket validation: `http://localhost:3000/staff`
- Admin dashboard: `http://localhost:3000/admin`
