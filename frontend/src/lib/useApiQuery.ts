"use client";

import { useCallback, useEffect, useState } from "react";

import { apiFetch } from "./api";

type Settled<T> = {
  key: string;
  attempt: number;
  error: Error | null;
  data: T | undefined;
};

export type ApiQuery<T> = {
  /** Latest successful result for this key; kept while a reload is in flight. */
  data: T | undefined;
  /** Error from the most recent attempt, if it failed. */
  error: Error | null;
  loading: boolean;
  reload: () => void;
};

function defaultLoader<T>(path: string) {
  return apiFetch<T>(path);
}

/**
 * Loads `key` with `load` (a GET of `key` by default) and re-runs whenever the key changes or
 * `reload` is called. `load` must be a stable function, e.g. declared at module scope.
 */
export function useApiQuery<T>(key: string | null, load: (key: string) => Promise<T> = defaultLoader<T>): ApiQuery<T> {
  const [attempt, setAttempt] = useState(0);
  const [settled, setSettled] = useState<Settled<T> | null>(null);

  useEffect(() => {
    if (key === null) return undefined;
    let cancelled = false;
    load(key).then(
      (data) => {
        if (!cancelled) setSettled({ key, attempt, error: null, data });
      },
      (error: unknown) => {
        if (cancelled) return;
        setSettled((previous) => ({
          key,
          attempt,
          error: error instanceof Error ? error : new Error("Request failed."),
          data: previous?.key === key ? previous.data : undefined,
        }));
      },
    );
    return () => {
      cancelled = true;
    };
  }, [key, attempt, load]);

  const reload = useCallback(() => setAttempt((current) => current + 1), []);

  const sameKey = settled !== null && settled.key === key;
  const current = sameKey && settled.attempt === attempt;

  return {
    data: sameKey ? settled.data : undefined,
    error: current ? settled.error : null,
    loading: key !== null && !current,
    reload,
  };
}
