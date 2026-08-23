import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { apiFetch, clearIdempotencyKey, getAccessToken, idempotencyKeyFor, storeAuth } from "./api";
import type { AuthResponse } from "./types";

type FetchMock = ReturnType<typeof vi.fn>;

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });
}

function authResponse(accessToken: string): AuthResponse {
  return {
    accessToken,
    refreshToken: "refresh-token-2",
    user: { id: "u1", name: "Ada", email: "ada@cinema.test", role: "CUSTOMER" },
  };
}

describe("apiFetch", () => {
  let fetchMock: FetchMock;

  beforeEach(() => {
    window.localStorage.clear();
    window.sessionStorage.clear();
    fetchMock = vi.fn();
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("refreshes the access token once and replays the request after a 401", async () => {
    storeAuth(authResponse("expired-token"));
    fetchMock
      .mockResolvedValueOnce(jsonResponse({ message: "Token expired." }, 401))
      .mockResolvedValueOnce(jsonResponse(authResponse("fresh-token")))
      .mockResolvedValueOnce(jsonResponse({ id: "b1" }));

    const result = await apiFetch<{ id: string }>("/bookings/b1");

    expect(result).toEqual({ id: "b1" });
    expect(fetchMock).toHaveBeenCalledTimes(3);
    expect(fetchMock.mock.calls[1][0]).toContain("/auth/refresh");
    expect(getAccessToken()).toBe("fresh-token");

    const replayHeaders = fetchMock.mock.calls[2][1].headers as Headers;
    expect(replayHeaders.get("Authorization")).toBe("Bearer fresh-token");
  });

  it("runs a single refresh for requests that fail concurrently", async () => {
    storeAuth(authResponse("expired-token"));
    fetchMock.mockImplementation((url: string) => {
      if (url.includes("/auth/refresh")) return Promise.resolve(jsonResponse(authResponse("fresh-token")));
      const authorization = "Bearer expired-token";
      const headers = fetchMock.mock.calls.at(-1)?.[1]?.headers as Headers | undefined;
      return Promise.resolve(headers?.get("Authorization") === authorization ? jsonResponse({}, 401) : jsonResponse({ ok: true }));
    });

    await Promise.all([apiFetch("/bookings/a"), apiFetch("/bookings/b"), apiFetch("/bookings/c")]);

    const refreshCalls = fetchMock.mock.calls.filter(([url]) => String(url).includes("/auth/refresh"));
    expect(refreshCalls).toHaveLength(1);
  });

  it("clears the session when the refresh token is rejected", async () => {
    storeAuth(authResponse("expired-token"));
    fetchMock
      .mockResolvedValueOnce(jsonResponse({ message: "Token expired." }, 401))
      .mockResolvedValueOnce(jsonResponse({ message: "Refresh token revoked." }, 401));

    await expect(apiFetch("/bookings/b1")).rejects.toThrow("Token expired.");
    expect(getAccessToken()).toBeNull();
  });

  it("does not try to refresh a failed login", async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse({ message: "Invalid credentials." }, 401));

    await expect(apiFetch("/auth/login", { method: "POST", body: "{}" })).rejects.toThrow("Invalid credentials.");
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it("reuses one idempotency key while an operation keeps failing", async () => {
    fetchMock
      .mockResolvedValueOnce(jsonResponse({ message: "Gateway timeout." }, 504))
      .mockResolvedValueOnce(jsonResponse({ id: "p1" }));

    const options = { method: "POST", body: "{}", idempotencyScope: "payment-initiate:b1" };
    await expect(apiFetch("/payments/initiate", options)).rejects.toThrow("Gateway timeout.");
    await apiFetch("/payments/initiate", options);

    const keys = fetchMock.mock.calls.map(([, init]) => (init.headers as Headers).get("Idempotency-Key"));
    expect(keys[0]).toBeTruthy();
    expect(keys[1]).toBe(keys[0]);
  });

  it("starts a new idempotency key once the operation succeeds", async () => {
    fetchMock.mockImplementation(() => Promise.resolve(jsonResponse({ id: "p1" })));

    const options = { method: "POST", body: "{}", idempotencyScope: "payment-initiate:b1" };
    await apiFetch("/payments/initiate", options);
    await apiFetch("/payments/initiate", options);

    const keys = fetchMock.mock.calls.map(([, init]) => (init.headers as Headers).get("Idempotency-Key"));
    expect(keys[1]).not.toBe(keys[0]);
  });

  it("gives unscoped mutations a fresh key per call", async () => {
    fetchMock.mockImplementation(() => Promise.resolve(jsonResponse({})));

    await apiFetch("/notifications/n1/read", { method: "POST" });
    await apiFetch("/notifications/n1/read", { method: "POST" });

    const keys = fetchMock.mock.calls.map(([, init]) => (init.headers as Headers).get("Idempotency-Key"));
    expect(keys[1]).not.toBe(keys[0]);
  });
});

describe("idempotencyKeyFor", () => {
  beforeEach(() => window.sessionStorage.clear());

  it("returns the same key until it is cleared", () => {
    const first = idempotencyKeyFor("lock-seats:s1:a,b");
    expect(idempotencyKeyFor("lock-seats:s1:a,b")).toBe(first);

    clearIdempotencyKey("lock-seats:s1:a,b");
    expect(idempotencyKeyFor("lock-seats:s1:a,b")).not.toBe(first);
  });

  it("keeps separate operations on separate keys", () => {
    expect(idempotencyKeyFor("lock-seats:s1:a")).not.toBe(idempotencyKeyFor("lock-seats:s1:b"));
  });
});
