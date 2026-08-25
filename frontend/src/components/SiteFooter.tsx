import Link from "next/link";
import { Clapperboard } from "lucide-react";

const year = new Date().getFullYear();

export function SiteFooter() {
  return (
    <footer className="border-t border-line bg-panel/40">
      <div className="mx-auto grid max-w-7xl gap-8 px-4 py-10 sm:px-6 md:grid-cols-[1.5fr_1fr_1fr]">
        <div>
          <span className="flex items-center gap-2 text-sm font-semibold text-foreground">
            <Clapperboard size={18} className="text-accent" aria-hidden />
            Cinema
          </span>
          <p className="mt-3 max-w-sm text-sm leading-6 text-muted">
            Pick a showtime, choose your seats, and pay in a couple of taps. Your seats are held while you check out.
          </p>
        </div>

        <nav aria-label="Browse">
          <h2 className="font-mono text-xs uppercase text-accent">Browse</h2>
          <ul className="mt-3 space-y-2 text-sm text-muted">
            <li>
              <Link href="/movies" className="hover:text-foreground">
                Movies
              </Link>
            </li>
            <li>
              <Link href="/bookings" className="hover:text-foreground">
                My bookings
              </Link>
            </li>
            <li>
              <Link href="/notifications" className="hover:text-foreground">
                Notifications
              </Link>
            </li>
          </ul>
        </nav>

        <div>
          <h2 className="font-mono text-xs uppercase text-accent">Visiting</h2>
          <ul className="mt-3 space-y-2 text-sm text-muted">
            <li>Central Cineplex</li>
            <li>Doors open 20 minutes before each screening</li>
            <li>Show your QR ticket at the entrance</li>
          </ul>
        </div>
      </div>

      <div className="border-t border-line px-4 py-5 text-center text-xs text-muted sm:px-6">
        &copy; {year} Cinema. A demo booking system.
      </div>
    </footer>
  );
}
