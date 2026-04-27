import Image from "next/image";
import Link from "next/link";
import { ArrowRight, CalendarDays, LockKeyhole, Ticket } from "lucide-react";
import type { LucideIcon } from "lucide-react";

import { AppShell } from "@/components/AppShell";
import { demoMovies, featuredMovie } from "@/lib/demo-data";

const features: { title: string; body: string; icon: LucideIcon }[] = [
  { title: "Seat locking", body: "Redis TTL locks prevent double booking.", icon: LockKeyhole },
  { title: "Mock payment", body: "Callbacks are idempotent and update bookings once.", icon: Ticket },
  { title: "Showtime browsing", body: "Pick a movie time and choose seats from a live map.", icon: CalendarDays },
];

export default function HomePageBeforeRedesign() {
  return (
    <AppShell>
      <section className="relative min-h-[calc(100vh-65px)] overflow-hidden">
        <Image src="/cinema-hero.png" alt="Modern cinema lobby" fill priority className="object-cover" />
        <div className="absolute inset-0 bg-[linear-gradient(90deg,rgba(13,15,18,0.96)_0%,rgba(13,15,18,0.82)_43%,rgba(13,15,18,0.38)_100%)]" />
        <div className="relative mx-auto grid min-h-[calc(100vh-65px)] max-w-7xl content-center px-4 py-16 sm:px-6">
          <div className="max-w-2xl">
            <p className="mb-4 font-mono text-xs uppercase text-accent">Modular monolith MVP</p>
            <h1 className="text-5xl font-semibold leading-tight text-foreground sm:text-7xl">Cinema</h1>
            <p className="mt-5 max-w-xl text-lg leading-8 text-muted">
              Browse showtimes, lock real seats temporarily, complete a mock payment, and keep your booking history in one place.
            </p>
            <div className="mt-8 flex flex-wrap gap-3">
              <Link href="/movies" className="flex items-center gap-2 rounded-md bg-accent px-5 py-3 text-sm font-semibold text-background">
                Book tickets
                <ArrowRight size={18} aria-hidden />
              </Link>
            </div>
          </div>
          <div className="mt-14 grid max-w-4xl gap-3 sm:grid-cols-3">
            {features.map(({ title, body, icon: Icon }) => (
              <div key={title} className="rounded-lg border border-line bg-background/72 p-4 backdrop-blur">
                <Icon className="mb-3 text-accent" size={20} aria-hidden />
                <h2 className="text-sm font-semibold">{title}</h2>
                <p className="mt-2 text-sm leading-6 text-muted">{body}</p>
              </div>
            ))}
          </div>
        </div>
      </section>
      <section className="mx-auto grid max-w-7xl gap-8 px-4 py-14 sm:px-6 lg:grid-cols-[360px_1fr]">
        <div className="relative min-h-[520px] overflow-hidden rounded-lg border border-line bg-panel">
          <Image src={featuredMovie.posterUrl || "/cinema-hero.png"} alt={featuredMovie.title} fill className="object-cover" />
        </div>
        <div className="flex flex-col justify-center">
          <p className="font-mono text-xs uppercase text-accent">Top IMDb pick</p>
          <h2 className="mt-3 text-4xl font-semibold sm:text-5xl">{featuredMovie.title}</h2>
          <p className="mt-4 max-w-2xl text-lg leading-8 text-muted">{featuredMovie.description}</p>
          <div className="mt-5 flex flex-wrap gap-3 text-sm text-muted">
            <span>{featuredMovie.genre}</span>
            <span>{featuredMovie.rating}</span>
            <span>{featuredMovie.durationMinutes} minutes</span>
            {featuredMovie.imdbRating ? <span className="text-accent">IMDb {featuredMovie.imdbRating.toFixed(1)}</span> : null}
          </div>
          <Link
            href={`/movies/${featuredMovie.id}`}
            className="mt-8 flex w-fit items-center gap-2 rounded-md bg-accent px-5 py-3 text-sm font-semibold text-background"
          >
            View showtimes
            <ArrowRight size={18} aria-hidden />
          </Link>
        </div>
      </section>
      <section className="mx-auto max-w-7xl px-4 pb-16 sm:px-6">
        <div className="mb-6 flex items-end justify-between gap-4">
          <div>
            <p className="font-mono text-xs uppercase text-accent">Real posters</p>
            <h2 className="mt-2 text-3xl font-semibold">Best-rated cinema picks</h2>
          </div>
          <Link href="/movies" className="hidden items-center gap-2 text-sm font-semibold text-accent sm:flex">
            Browse all
            <ArrowRight size={16} aria-hidden />
          </Link>
        </div>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
          {demoMovies.slice(0, 10).map((movie) => (
            <Link key={movie.id} href={`/movies/${movie.id}`} className="group overflow-hidden rounded-lg border border-line bg-panel">
              <div className="relative aspect-[2/3]">
                <Image src={movie.posterUrl || "/cinema-hero.png"} alt={movie.title} fill className="object-cover transition group-hover:scale-[1.03]" />
              </div>
              <div className="p-3">
                <h3 className="line-clamp-2 min-h-10 text-sm font-semibold">{movie.title}</h3>
                <p className="mt-2 text-xs text-accent">IMDb {movie.imdbRating?.toFixed(1)}</p>
              </div>
            </Link>
          ))}
        </div>
      </section>
    </AppShell>
  );
}
