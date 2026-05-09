"use client";

import { Bell, CheckCheck } from "lucide-react";
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
          {notifications.map((item) => (
            <article key={item.id} className="rounded-lg border border-line bg-panel p-5">
              <div className="flex items-start justify-between gap-4">
                <div className="min-w-0">
                  <p className="font-mono text-xs uppercase text-accent">{item.type.replaceAll("_", " ")}</p>
                  <h2 className="mt-2 text-xl font-semibold">{item.title}</h2>
                  <p className="mt-2 text-sm leading-6 text-muted">{item.message}</p>
                  <p className="mt-3 text-xs text-muted">{new Date(item.createdAt).toLocaleString()}</p>
                </div>
                {!item.readAt && (
                  <button
                    type="button"
                    onClick={() => markRead(item.id)}
                    className="rounded-md bg-accent px-3 py-2 text-sm font-semibold text-background"
                  >
                    Read
                  </button>
                )}
              </div>
            </article>
          ))}
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
