"use client";

import { ArrowRight, Bell, CheckCheck, Loader2 } from "lucide-react";
import Link from "next/link";
import { useState } from "react";

import { AppShell } from "@/components/AppShell";
import { ErrorState, InlineError } from "@/components/Feedback";
import { apiFetch, errorMessage, notifyNotificationsChanged } from "@/lib/api";
import type { NotificationList } from "@/lib/types";
import { useApiQuery } from "@/lib/useApiQuery";

// The server returns only the most recent notifications.
const NOTIFICATION_LIMIT = 100;

export default function NotificationsPage() {
  const { data, error, loading, reload } = useApiQuery<NotificationList>("/notifications");
  const [actionError, setActionError] = useState("");
  const [pending, setPending] = useState<string | null>(null);
  const notifications = data?.notifications ?? [];
  const unreadCount = data?.unreadCount ?? notifications.filter((item) => !item.readAt).length;

  async function mutate(key: string, path: string, fallback: string) {
    if (pending) return;
    setActionError("");
    setPending(key);
    try {
      await apiFetch<void>(path, { method: "POST" });
      reload();
      notifyNotificationsChanged();
    } catch (err) {
      setActionError(errorMessage(err, fallback));
    } finally {
      setPending(null);
    }
  }

  return (
    <AppShell>
      <section className="mx-auto max-w-5xl px-4 py-10 sm:px-6">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <p className="font-mono text-xs uppercase text-accent">Notification center</p>
            <h1 className="mt-2 text-4xl font-semibold">Cinema updates</h1>
          </div>
          <button
            type="button"
            onClick={() => mutate("all", "/notifications/read-all", "Could not mark notifications as read.")}
            disabled={pending !== null || unreadCount === 0}
            className="flex items-center gap-2 rounded-md border border-line px-4 py-3 text-sm text-muted hover:border-accent hover:text-accent disabled:cursor-not-allowed disabled:opacity-50"
          >
            {pending === "all" ? <Loader2 size={17} className="animate-spin" aria-hidden /> : <CheckCheck size={17} aria-hidden />}
            Mark all read
          </button>
        </div>

        <InlineError className="mt-5" message={actionError} />
        {error && !data ? (
          <ErrorState className="mt-5" title="We couldn't load your notifications" message={error.message} onRetry={reload} retrying={loading} />
        ) : null}

        <div className="mt-6 grid gap-3">
          {!data && !error
            ? Array.from({ length: 3 }).map((_, index) => (
                <div key={index} aria-hidden className="h-32 animate-pulse rounded-lg border border-line bg-panel" />
              ))
            : null}
          {notifications.map((item) => {
            const unread = !item.readAt;
            return (
              <article
                key={item.id}
                className={`rounded-lg border p-5 ${
                  unread ? "border-accent/40 bg-panel" : "border-line bg-panel/50"
                }`}
              >
                <div className="flex items-start justify-between gap-4">
                  <div className="min-w-0">
                    <p className="flex items-center gap-2 font-mono text-xs uppercase text-accent">
                      {unread && (
                        <>
                          <span className="size-2 rounded-full bg-accent" aria-hidden />
                          <span className="sr-only">Unread:</span>
                        </>
                      )}
                      {item.type.replaceAll("_", " ")}
                    </p>
                    <h2 className={`mt-2 text-xl ${unread ? "font-semibold text-foreground" : "font-medium text-muted"}`}>
                      {item.title}
                    </h2>
                    <p className="mt-2 text-sm leading-6 text-muted">{item.message}</p>
                    <div className="mt-3 flex flex-wrap items-center gap-4">
                      <p className="text-xs text-muted">{new Date(item.createdAt).toLocaleString()}</p>
                      {item.bookingId && (
                        <Link
                          href={`/confirmation/${item.bookingId}`}
                          className="flex items-center gap-1.5 text-xs font-semibold text-accent hover:underline"
                        >
                          View booking
                          <ArrowRight size={13} aria-hidden />
                        </Link>
                      )}
                    </div>
                  </div>
                  {unread && (
                    <button
                      type="button"
                      onClick={() => mutate(item.id, `/notifications/${item.id}/read`, "Could not mark the notification as read.")}
                      disabled={pending !== null}
                      aria-label={`Mark "${item.title}" as read`}
                      className="shrink-0 rounded-md border border-line px-3 py-2 text-sm text-muted hover:border-accent hover:text-accent disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      {pending === item.id ? "Marking…" : "Mark read"}
                    </button>
                  )}
                </div>
              </article>
            );
          })}
          {data && notifications.length === 0 && (
            <div className="rounded-lg border border-line bg-panel p-8 text-center text-muted">
              <Bell className="mx-auto text-accent" size={34} aria-hidden />
              <p className="mt-3">No notifications yet.</p>
            </div>
          )}
          {notifications.length >= NOTIFICATION_LIMIT && (
            <p className="text-center text-xs text-muted">Showing your {NOTIFICATION_LIMIT} most recent notifications.</p>
          )}
        </div>
      </section>
    </AppShell>
  );
}
