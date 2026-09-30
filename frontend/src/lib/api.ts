import type { AuthResponse, ErrorResponse, User } from "./types";

export const API_BASE = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api";
const ACCESS_KEY = "cinema.accessToken";
const REFRESH_KEY = "cinema.refreshToken";
const USER_KEY = "cinema.user";
const IDEMPOTENCY_PREFIX = "cinema.idempotency.";
let cachedUserRaw: string | null | undefined;
let cachedUser: User | null = null;

export class ApiError extends Error {
  readonly status: number;
  readonly requestId?: string;

  constructor(message: string, status: number, requestId?: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.requestId = requestId;
  }
}

export function errorMessage(error: unknown, fallback: string): string {
  return error instanceof Error && error.message ? error.message : fallback;
}

export function getStoredUser(): User | null {
  if (typeof window === "undefined") return null;
  const raw = window.localStorage.getItem(USER_KEY);
  if (raw === cachedUserRaw) return cachedUser;

  cachedUserRaw = raw;
  try {
    cachedUser = raw ? (JSON.parse(raw) as User) : null;
  } catch {
    cachedUser = null;
  }
  return cachedUser;
}

export function subscribeToAuthChanges(listener: () => void) {
  if (typeof window === "undefined") return () => undefined;
  window.addEventListener("cinema-auth", listener);
  window.addEventListener("storage", listener);
  return () => {
    window.removeEventListener("cinema-auth", listener);
    window.removeEventListener("storage", listener);
  };
}

export function getAccessToken(): string | null {
  if (typeof window === "undefined") return null;
  return window.localStorage.getItem(ACCESS_KEY);
}

export function getRefreshToken(): string | null {
  if (typeof window === "undefined") return null;
  return window.localStorage.getItem(REFRESH_KEY);
}

export function storeAuth(auth: AuthResponse) {
  window.localStorage.setItem(ACCESS_KEY, auth.accessToken);
  window.localStorage.setItem(REFRESH_KEY, auth.refreshToken);
  window.localStorage.setItem(USER_KEY, JSON.stringify(auth.user));
  window.dispatchEvent(new Event("cinema-auth"));
}

export function clearAuth() {
  window.localStorage.removeItem(ACCESS_KEY);
  window.localStorage.removeItem(REFRESH_KEY);
  window.localStorage.removeItem(USER_KEY);
  window.dispatchEvent(new Event("cinema-auth"));
}

export const NOTIFICATIONS_CHANGED_EVENT = "cinema-notifications";

export function notifyNotificationsChanged() {
  if (typeof window !== "undefined") window.dispatchEvent(new Event(NOTIFICATIONS_CHANGED_EVENT));
}

/** Returns `next` only if it is a same-origin path, so a crafted login link can't redirect off-site. */
export function safeNextPath(next: string | null | undefined): string | null {
  if (!next || !next.startsWith("/") || next.startsWith("//") || next.startsWith("/\\")) return null;
  try {
    const origin = typeof window === "undefined" ? "http://localhost" : window.location.origin;
    const url = new URL(next, origin);
    return url.origin === origin ? `${url.pathname}${url.search}${url.hash}` : null;
  } catch {
    return null;
  }
}

export function loginPath(next?: string) {
  const target = next ?? (typeof window === "undefined" ? "/" : `${window.location.pathname}${window.location.search}`);
  return `/login?next=${encodeURIComponent(target)}`;
}

type SessionExpiredHandler = (loginUrl: string) => void;

const defaultSessionExpiredHandler: SessionExpiredHandler = (loginUrl) => {
  if (typeof window === "undefined") return;
  if (["/login", "/register"].includes(window.location.pathname)) return;
  window.location.assign(loginUrl);
};

let sessionExpiredHandler: SessionExpiredHandler = defaultSessionExpiredHandler;

/** Swap how the client leaves the page when a session cannot be recovered. Returns a restore function. */
export function setSessionExpiredHandler(handler: SessionExpiredHandler) {
  sessionExpiredHandler = handler;
  return () => {
    sessionExpiredHandler = defaultSessionExpiredHandler;
  };
}

export type ApiOptions = RequestInit & {
  idempotencyScope?: string;
  /** Background requests set this so an expired session quietly signs the user out instead of navigating away. */
  skipAuthRedirect?: boolean;
};

type InternalOptions = ApiOptions & { skipJsonContentType?: boolean };

const AUTH_PATHS = ["/auth/login", "/auth/register", "/auth/refresh", "/auth/logout"];
const NETWORK_ERROR_MESSAGE = "Could not reach the server. Check your connection and try again.";

type RefreshOutcome = "refreshed" | "rejected" | "unavailable";
let inFlightRefresh: Promise<RefreshOutcome> | null = null;

export async function apiFetch<T>(path: string, options: ApiOptions = {}): Promise<T> {
  const response = await sendWithAuth(path, options);

  if (!response.ok) {
    throw await toApiError(response);
  }
  clearIdempotencyKey(options.idempotencyScope);

  if (response.status === 204) {
    return undefined as T;
  }

  const text = await response.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

export async function apiBlob(path: string, options: ApiOptions = {}): Promise<Blob> {
  const response = await sendWithAuth(path, { ...options, skipJsonContentType: true });

  if (!response.ok) {
    throw await toApiError(response);
  }
  clearIdempotencyKey(options.idempotencyScope);

  return response.blob();
}

async function sendWithAuth(path: string, options: InternalOptions): Promise<Response> {
  // Resolve the key once so a replay after a token refresh is recognised as the same operation.
  const idempotencyKey = resolveIdempotencyKey(options);
  const sentWithToken = getAccessToken();
  const response = await send(path, options, idempotencyKey);
  if (response.status !== 401 || AUTH_PATHS.includes(path)) {
    return response;
  }

  if (!sentWithToken && !getRefreshToken()) {
    return sessionRequired(options, "Please sign in to continue.");
  }

  const outcome = await refreshAccessToken(sentWithToken);
  if (outcome === "refreshed") {
    return send(path, options, idempotencyKey);
  }
  if (outcome === "unavailable") {
    return response;
  }

  clearAuth();
  // A public endpoint can reject a stale token outright, so try once as a guest before
  // concluding this page needs a sign-in.
  if (sentWithToken) {
    const anonymous = await send(path, options, idempotencyKey);
    if (anonymous.status !== 401) return anonymous;
  }
  return sessionRequired(options, "Your session has expired. Please sign in again.");
}

function sessionRequired(options: InternalOptions, message: string): never {
  if (!options.skipAuthRedirect) sessionExpiredHandler(loginPath());
  throw new ApiError(message, 401);
}

async function send(path: string, options: InternalOptions, idempotencyKey: string | null): Promise<Response> {
  const headers = new Headers(options.headers);
  if (!options.skipJsonContentType) {
    headers.set("Content-Type", "application/json");
  }
  const token = getAccessToken();
  if (token) headers.set("Authorization", `Bearer ${token}`);
  if (idempotencyKey) headers.set("Idempotency-Key", idempotencyKey);

  try {
    return await fetch(`${API_BASE}${path}`, { ...options, headers });
  } catch {
    throw new ApiError(NETWORK_ERROR_MESSAGE, 0);
  }
}

function refreshAccessToken(staleAccessToken: string | null): Promise<RefreshOutcome> {
  if (!inFlightRefresh) {
    inFlightRefresh = withCrossTabLock(() => performRefresh(staleAccessToken)).finally(() => {
      inFlightRefresh = null;
    });
  }
  return inFlightRefresh;
}

function withCrossTabLock<T>(task: () => Promise<T>): Promise<T> {
  const locks = typeof navigator === "undefined" ? undefined : navigator.locks;
  if (!locks?.request) return task();
  return new Promise<T>((resolve, reject) => {
    locks.request("cinema.auth.refresh", () => task().then(resolve, reject)).catch(reject);
  });
}

// Refresh tokens rotate and the server revokes the whole family when an old one is replayed,
// so another tab that refreshed first must win: always prefer a newer stored access token.
async function performRefresh(staleAccessToken: string | null): Promise<RefreshOutcome> {
  if (tokenChangedSince(staleAccessToken)) return "refreshed";

  const refreshToken = getRefreshToken();
  if (!refreshToken) return "rejected";

  let response: Response;
  try {
    response = await fetch(`${API_BASE}/auth/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
    });
  } catch {
    return tokenChangedSince(staleAccessToken) ? "refreshed" : "unavailable";
  }

  if (response.ok) {
    storeAuth((await response.json()) as AuthResponse);
    return "refreshed";
  }
  if (tokenChangedSince(staleAccessToken)) return "refreshed";
  return response.status >= 500 ? "unavailable" : "rejected";
}

function tokenChangedSince(staleAccessToken: string | null) {
  const current = getAccessToken();
  return current !== null && current !== staleAccessToken;
}

async function toApiError(response: Response): Promise<ApiError> {
  let body: Partial<ErrorResponse> | null = null;
  try {
    body = (await response.json()) as Partial<ErrorResponse>;
  } catch {
    body = null;
  }
  const requestId = body?.requestId ?? response.headers.get("X-Request-Id") ?? undefined;
  const base = body?.message || defaultErrorMessage(response.status);
  // A reference only helps support track down server faults; client errors explain themselves.
  const message = requestId && response.status >= 500 ? `${base} (Reference: ${requestId})` : base;
  return new ApiError(message, response.status, requestId);
}

function defaultErrorMessage(status: number) {
  if (status === 503) return "The service is temporarily unavailable. Please try again shortly.";
  if (status >= 500) return "Something went wrong on our side. Please try again.";
  return `Request failed with ${status}`;
}

function resolveIdempotencyKey({ method = "GET", headers, idempotencyScope }: InternalOptions): string | null {
  if (!["POST", "PUT", "PATCH", "DELETE"].includes(method.toUpperCase())) return null;
  const explicit = new Headers(headers).get("Idempotency-Key");
  if (explicit) return explicit;
  return idempotencyScope ? idempotencyKeyFor(idempotencyScope) : createRequestId();
}

export function idempotencyKeyFor(scope: string): string {
  const storageKey = `${IDEMPOTENCY_PREFIX}${scope}`;
  if (typeof window === "undefined") return createRequestId();

  const existing = window.sessionStorage.getItem(storageKey);
  if (existing) return existing;

  const key = createRequestId();
  window.sessionStorage.setItem(storageKey, key);
  return key;
}

export function clearIdempotencyKey(scope?: string) {
  if (!scope || typeof window === "undefined") return;
  window.sessionStorage.removeItem(`${IDEMPOTENCY_PREFIX}${scope}`);
}

function createRequestId() {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
    return crypto.randomUUID();
  }
  return `req-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

export async function logout() {
  const refreshToken = getRefreshToken();
  if (refreshToken) {
    await apiFetch("/auth/logout", {
      method: "POST",
      body: JSON.stringify({ refreshToken }),
    }).catch(() => undefined);
  }
  clearAuth();
}
