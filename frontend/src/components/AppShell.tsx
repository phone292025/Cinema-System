"use client";

import Image from "next/image";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import {
  Bell,
  Clapperboard,
  Film,
  LayoutDashboard,
  LogIn,
  LogOut,
  MapPin,
  QrCode,
  ShieldCheck,
  Ticket,
  UserRound,
} from "lucide-react";
import { useEffect, useState, useSyncExternalStore } from "react";

import { apiFetch, getStoredUser, logout, subscribeToAuthChanges } from "@/lib/api";
import type { NotificationList } from "@/lib/types";

const nav = [
  { href: "/movies", label: "Movies", icon: Film },
  { href: "/bookings", label: "Bookings", icon: Ticket },
  { href: "/staff", label: "Staff", icon: ShieldCheck, roles: ["ADMIN", "STAFF"] },
  { href: "/admin", label: "Admin", icon: LayoutDashboard, roles: ["ADMIN"] },
];

const mobileNav = [
  { href: "/", label: "Home", icon: Clapperboard },
  { href: "/movies", label: "Movies", icon: Film },
  { href: "/bookings", label: "Bookings", icon: Ticket },
  { href: "/notifications", label: "Alerts", icon: Bell, authOnly: true },
  { href: "/login", label: "Profile", icon: UserRound, guestOnly: true },
];

export function AppShell({ children }: { children: React.ReactNode }) {
  const user = useSyncExternalStore(subscribeToAuthChanges, getStoredUser, () => null);
  const [unreadNotifications, setUnreadNotifications] = useState<{ userId: string; count: number } | null>(null);
  const pathname = usePathname();
  const router = useRouter();

  useEffect(() => {
    if (!user) return undefined;
    let cancelled = false;
    apiFetch<NotificationList>("/notifications")
      .then((items) => {
        if (!cancelled) setUnreadNotifications({ userId: user.id, count: items.unreadCount });
      })
      .catch(() => {
        if (!cancelled) setUnreadNotifications({ userId: user.id, count: 0 });
      });
    return () => {
      cancelled = true;
    };
  }, [user]);

  const unreadNotificationCount = user && unreadNotifications?.userId === user.id ? unreadNotifications.count : 0;

  async function handleLogout() {
    await logout();
    router.push("/");
  }

  return (
    <div className="min-h-screen overflow-x-hidden">
      <header className="sticky top-0 z-30 border-b border-line bg-background/92 backdrop-blur md:hidden">
        <div className="flex min-w-0 items-center justify-between gap-2 px-4 py-4">
          <Link href="/" className="grid size-10 place-items-center rounded-md border border-white/10 bg-panel/80 text-foreground" aria-label="Home">
            <Clapperboard size={22} aria-hidden />
          </Link>
          <Link href="/movies" className="flex min-w-0 items-center gap-2 text-sm font-semibold uppercase text-foreground">
            <MapPin size={20} aria-hidden />
            <span className="truncate">Central Cineplex</span>
          </Link>
          <div className="flex shrink-0 items-center gap-2">
            <Link
              href="/staff"
              className="grid size-10 place-items-center rounded-md border border-white/10 bg-panel/80 text-foreground"
              aria-label="Staff scanner"
              title="Staff scanner"
            >
              <QrCode size={20} aria-hidden />
            </Link>
            {user ? (
              <Link
                href="/notifications"
                className="relative grid size-10 place-items-center rounded-md border border-white/10 bg-panel/80 text-foreground"
                aria-label="Notifications"
                title="Notifications"
              >
                <Bell size={20} aria-hidden />
                {unreadNotificationCount > 0 && <span className="absolute right-2 top-2 size-2 rounded-full bg-accent-strong" />}
              </Link>
            ) : (
              <Link
                href="/login"
                className="grid size-10 place-items-center rounded-md border border-white/10 bg-panel/80 text-foreground"
                aria-label="Sign in"
                title="Sign in"
              >
                <LogIn size={20} aria-hidden />
              </Link>
            )}
          </div>
        </div>
      </header>

      <header className="sticky top-0 z-30 hidden border-b border-line bg-background/92 backdrop-blur md:block">
        <div className="mx-auto flex max-w-7xl items-center justify-between gap-4 px-4 py-3 sm:px-6">
          <Link href="/" className="flex items-center gap-2 text-sm font-semibold text-foreground">
            <span className="relative size-9 overflow-hidden rounded-md border border-accent/40 bg-panel">
              <Image src="/cinema-logo.png" alt="" fill sizes="36px" className="object-cover" />
            </span>
            Cinema
          </Link>
          <nav className="hidden items-center gap-1 md:flex">
            {nav.filter((item) => !item.roles || (user && item.roles.includes(user.role))).map((item) => {
              const Icon = item.icon;
              const active = pathname.startsWith(item.href);
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  className={`flex items-center gap-2 rounded-md px-3 py-2 text-sm ${
                    active ? "bg-panel text-accent" : "text-muted hover:bg-panel hover:text-foreground"
                  }`}
                >
                  <Icon size={16} aria-hidden />
                  {item.label}
                </Link>
              );
            })}
          </nav>
          <div className="flex items-center gap-2">
            {user ? (
              <>
                <Link
                  href="/notifications"
                  className="relative grid size-10 place-items-center rounded-md border border-line text-muted hover:border-accent hover:text-accent"
                  aria-label="Notifications"
                  title="Notifications"
                >
                  <Bell size={18} aria-hidden />
                  {unreadNotificationCount > 0 && (
                    <span className="absolute -right-1 -top-1 grid min-w-5 place-items-center rounded-full bg-accent px-1.5 py-0.5 text-[10px] font-semibold text-background">
                      {unreadNotificationCount}
                    </span>
                  )}
                </Link>
                <span className="hidden items-center gap-2 text-sm text-muted sm:flex">
                  <UserRound size={16} aria-hidden />
                  {user.name}
                </span>
                <button
                  type="button"
                  onClick={handleLogout}
                  className="grid size-10 place-items-center rounded-md border border-line text-muted hover:border-accent hover:text-accent"
                  aria-label="Log out"
                  title="Log out"
                >
                  <LogOut size={18} aria-hidden />
                </button>
              </>
            ) : (
              <Link
                href="/login"
                className="flex items-center gap-2 rounded-md border border-line px-3 py-2 text-sm text-muted hover:border-accent hover:text-accent"
              >
                <LogIn size={16} aria-hidden />
                Login
              </Link>
            )}
          </div>
        </div>
      </header>
      <main className="overflow-x-hidden pb-24 md:pb-0">{children}</main>
      <nav className="fixed inset-x-0 bottom-0 z-30 border-t border-white/10 bg-[#0b0c0e]/95 px-2 pb-[max(env(safe-area-inset-bottom),0.5rem)] pt-2 backdrop-blur md:hidden">
        <div className="grid grid-cols-4 gap-1">
          {mobileNav
            .filter((item) => !(item.authOnly && !user) && !(item.guestOnly && user))
            .slice(0, 4)
            .map((item) => {
              const Icon = item.icon;
              const active = item.href === "/" ? pathname === "/" : pathname.startsWith(item.href);
              return (
                <Link
                  key={`${item.href}-${item.label}`}
                  href={item.href}
                  className={`grid justify-items-center gap-1 rounded-md px-1 py-2 text-[11px] font-medium ${
                    active ? "text-accent" : "text-muted"
                  }`}
                >
                  <Icon size={24} aria-hidden />
                  <span>{item.label}</span>
                </Link>
              );
            })}
        </div>
      </nav>
    </div>
  );
}
