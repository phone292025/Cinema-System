# Cinema frontend

Next.js App Router client for the cinema booking API: browsing, live seat selection, checkout, tickets, the staff scanner, and the admin console.

## Scripts

```bash
npm ci            # install exactly what package-lock.json pins
npm run dev       # dev server on http://localhost:3000
npm run lint      # ESLint (Next core-web-vitals + TypeScript rules)
npm test          # Vitest + React Testing Library, jsdom
npm run build     # production build (standalone output, used by the Dockerfile)
```

Node 22.22.2 or newer is required (`engines` in `package.json`); CI and the Docker image use Node 24.

## Configuration

| Variable | Default | Notes |
| --- | --- | --- |
| `NEXT_PUBLIC_API_BASE_URL` | `http://localhost:8080/api` | Inlined into the client bundle **at build time**. Changing it on a running container does nothing; rebuild instead (Compose passes it as a build arg). |

## Structure

```
src/app/                 routes (App Router); each page is a client component
src/components/          shared UI: AppShell, SeatPicker, HoldCountdown, QrScanner, Feedback states
src/components/admin/    admin console: one module per workspace, shared form/panel primitives,
                         and useAdminConsole (data loading + actions)
src/lib/api.ts           fetch wrapper: auth headers, token refresh, idempotency keys, errors
src/lib/useApiQuery.ts   load-on-mount data hook with error + retry
src/lib/useLiveSeats.ts  seat-event stream with reconnect/backoff and resync
src/lib/useTicketPolling.ts  polls for the asynchronously issued ticket
src/lib/showcase.ts      static hero artwork; which movies exist always comes from the API
```

## How the client talks to the API

- **Sessions.** Access and refresh tokens live in `localStorage`. A `401` triggers one refresh. It is single-flight within a tab and serialised across tabs with the Web Locks API, because refresh tokens rotate and a replayed old token revokes the whole family. The original request is then replayed. If the refresh is rejected, the user is signed out and sent to `/login?next=…`; `next` is only honoured for same-site paths.
- **Idempotency.** Every mutating request carries an `Idempotency-Key`. Operations where a duplicate costs money reuse one key per logical operation (stored in `sessionStorage`) until they succeed. Those operations are locking seats, starting a payment, the payment callback and cancelling.
- **Errors.** API errors surface the server's message. Server-side failures also show the request ID ("Reference: …"), which matches the backend logs.
- **No fake data.** If the API is unreachable, pages show an error with a Retry button rather than falling back to demo content.
- **Live seats.** The seat map subscribes to `/showtimes/{id}/seat-events` (Server-Sent Events). If the stream drops, it reconnects with capped exponential backoff and re-fetches the seat map, because events sent while disconnected are lost.

## Tests

Tests sit next to the code as `*.test.ts(x)`: the API client, `SeatPicker` (selection, live events, reconnect), `HoldCountdown`, and the admin sales board. They run in jsdom; `EventSource` and `fetch` are stubbed per test.
