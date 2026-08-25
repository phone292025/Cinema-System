"use client";

import { ArrowRight, Bell, CheckCheck } from "lucide-react";
import Link from "next/link";
import { useEffect, useState } from "react";

import { AppShell } from "@/components/AppShell";
import { apiFetch } from "@/lib/api";
import type { Notification, NotificationList } from "@/lib/types";

export default function NotificationsPage() {
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [error, setError] = useState("");

  function load() {
    apiFetch<NotificationList>("/notifications").then((response) => setNotifications(response.notifications)).catch((err) => setError(err.message));
  }

  useEffect(load, []);

  async function markRead(id: string) {
    await apiFetch<Notification>(`/notifications/${id}/read`, { method: "POST" });
    load();
  }

  async function markAllRead() {
    await apiFetch<void>("/notifications/read-all", { method: "POST" });
    load();
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
            onClick={markAllRead}
            className="flex items-center gap-2 rounded-md border border-line px-4 py-3 text-sm text-muted hover:border-accent hover:text-accent"
          >
            <CheckCheck size={17} aria-hidden />
            Mark all read
          </button>
        </div>

        {error && <p className="mt-5 rounded-md border border-danger/40 bg-danger/10 p-4 text-danger">{error}</p>}

        <div className="mt-6 grid gap-3">
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
                      {unread && <span className="size-2 rounded-full bg-accent" aria-label="Unread" />}
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
                          View ticket
                          <ArrowRight size={13} aria-hidden />
                        </Link>
                      )}
                    </div>
                  </div>
                  {unread && (
                    <button
                      type="button"
                      onClick={() => markRead(item.id)}
                      className="shrink-0 rounded-md border border-line px-3 py-2 text-sm text-muted hover:border-accent hover:text-accent"
                    >
                      Mark read
                    </button>
                  )}
                </div>
              </article>
            );
          })}
          {notifications.length === 0 && (
            <div className="rounded-lg border border-line bg-panel p-8 text-center text-muted">
              <Bell className="mx-auto text-accent" size={34} aria-hidden />
              <p className="mt-3">No notifications yet.</p>
            </div>
          )}
        </div>
      </section>
    </AppShell>
  );
}
