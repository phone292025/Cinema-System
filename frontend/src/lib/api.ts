import type { AuthResponse, User } from "./types";

export const API_BASE = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api";
const ACCESS_KEY = "cinema.accessToken";
const REFRESH_KEY = "cinema.refreshToken";
const USER_KEY = "cinema.user";
const IDEMPOTENCY_PREFIX = "cinema.idempotency.";
let cachedUserRaw: string | null | undefined;
let cachedUser: User | null = null;

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

export type ApiOptions = RequestInit & { idempotencyScope?: string };

type InternalOptions = ApiOptions & { skipJsonContentType?: boolean };

const AUTH_PATHS = ["/auth/login", "/auth/register", "/auth/refresh", "/auth/logout"];
let inFlightRefresh: Promise<boolean> | null = null;

export async function apiFetch<T>(path: string, options: ApiOptions = {}): Promise<T> {
  const response = await sendWithAuth(path, options);

  if (!response.ok) {
    throw new Error(await errorMessage(response));
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
    throw new Error(await errorMessage(response));
  }
  clearIdempotencyKey(options.idempotencyScope);

  return response.blob();
}

async function sendWithAuth(path: string, options: InternalOptions): Promise<Response> {
  const response = await send(path, options);
  if (response.status !== 401 || AUTH_PATHS.includes(path) || !getRefreshToken()) {
    return response;
  }

  const refreshed = await refreshAccessToken();
  if (!refreshed) {
    return response;
  }
  return send(path, options);
}

async function send(path: string, options: InternalOptions): Promise<Response> {
  const headers = new Headers(options.headers);
  if (!options.skipJsonContentType) {
    headers.set("Content-Type", "application/json");
  }
  const token = getAccessToken();
  if (token) headers.set("Authorization", `Bearer ${token}`);
  ensureIdempotencyHeader(headers, options.method, options.idempotencyScope);

  return fetch(`${API_BASE}${path}`, { ...options, headers });
}

async function refreshAccessToken(): Promise<boolean> {
  if (inFlightRefresh) return inFlightRefresh;

  inFlightRefresh = (async () => {
    const refreshToken = getRefreshToken();
    if (!refreshToken) return false;
    try {
      const response = await fetch(`${API_BASE}/auth/refresh`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ refreshToken }),
      });
      if (!response.ok) {
        clearAuth();
        return false;
      }
      storeAuth((await response.json()) as AuthResponse);
      return true;
    } catch {
      return false;
    } finally {
      inFlightRefresh = null;
    }
  })();

  return inFlightRefresh;
}

async function errorMessage(response: Response): Promise<string> {
  try {
    const body = (await response.json()) as { message?: string };
    return body.message ?? `Request failed with ${response.status}`;
  } catch {
    return `Request failed with ${response.status}`;
  }
}

function ensureIdempotencyHeader(headers: Headers, method = "GET", scope?: string) {
  const normalized = method.toUpperCase();
  if (!["POST", "PUT", "PATCH", "DELETE"].includes(normalized) || headers.has("Idempotency-Key")) {
    return;
  }
  headers.set("Idempotency-Key", scope ? idempotencyKeyFor(scope) : createRequestId());
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
