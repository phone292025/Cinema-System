"use client";

import Image from "next/image";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { Bell, Film, LayoutDashboard, LogIn, LogOut, ShieldCheck, Ticket, UserRound } from "lucide-react";
import { useEffect, useState } from "react";

import { apiFetch, getStoredUser, logout } from "@/lib/api";
import type { NotificationList, User } from "@/lib/types";

const nav = [
  { href: "/movies", label: "Movies", icon: Film },
  { href: "/bookings", label: "Bookings", icon: Ticket },
  { href: "/staff", label: "Staff", icon: ShieldCheck, roles: ["ADMIN", "STAFF"] },
  { href: "/admin", label: "Admin", icon: LayoutDashboard, roles: ["ADMIN"] },
];

export function AppShell({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [unreadNotifications, setUnreadNotifications] = useState(0);
  const pathname = usePathname();
  const router = useRouter();

  useEffect(() => {
    const sync = () => setUser(getStoredUser());
    sync();
    window.addEventListener("cinema-auth", sync);
    window.addEventListener("storage", sync);
    return () => {
      window.removeEventListener("cinema-auth", sync);
      window.removeEventListener("storage", sync);
    };
  }, []);

  useEffect(() => {
    if (!user) {
      setUnreadNotifications(0);
      return;
    }
    apiFetch<NotificationList>("/notifications")
      .then((items) => setUnreadNotifications(items.unreadCount))
      .catch(() => setUnreadNotifications(0));
  }, [user]);

  async function handleLogout() {
    await logout();
    router.push("/");
  }

  return (
    <div className="min-h-screen">
      <header className="sticky top-0 z-30 border-b border-line bg-background/92 backdrop-blur">
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
                  {unreadNotifications > 0 && (
                    <span className="absolute -right-1 -top-1 grid min-w-5 place-items-center rounded-full bg-accent px-1.5 py-0.5 text-[10px] font-semibold text-background">
                      {unreadNotifications}
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
      <main>{children}</main>
    </div>
  );
}
