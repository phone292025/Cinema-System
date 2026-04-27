import Image from "next/image";
import Link from "next/link";
import { ArrowRight, CalendarDays, CheckCircle2, Clock3, Film, LockKeyhole, Ticket, WalletCards } from "lucide-react";
import type { LucideIcon } from "lucide-react";

import { AppShell } from "@/components/AppShell";
import { demoMovies, featuredMovie } from "@/lib/demo-data";

const heroFeatures: { title: string; body: string; icon: LucideIcon }[] = [
  { title: "Seat locking", body: "Redis TTL locks prevent double booking.", icon: LockKeyhole },
  { title: "Mock payment", body: "Callbacks are idempotent and update bookings once.", icon: Ticket },
  { title: "Showtime browsing", body: "Pick a movie time and choose seats from a live map.", icon: CalendarDays },
];

const bookingTools: { title: string; body: string; icon: LucideIcon; href: string }[] = [
  { title: "Browse showtimes", body: "Start with the movie list and jump straight to a session.", icon: Film, href: "/movies" },
  { title: "Lock seats", body: "Choose the exact chairs before checkout starts.", icon: LockKeyhole, href: `/movies/${featuredMovie.id}` },
  { title: "Booking history", body: "Check paid bookings and saved confirmations.", icon: Ticket, href: "/bookings" },
];

const bookingFlow: { title: string; body: string; icon: LucideIcon }[] = [
  { title: "Pick a session", body: "Choose a movie, day, hall, and showtime.", icon: CalendarDays },
  { title: "Hold your chairs", body: "Selected seats are reserved temporarily while you pay.", icon: LockKeyhole },
  { title: "Mock payment", body: "Complete checkout and keep one booking record.", icon: WalletCards },
  { title: "Confirmed", body: "Return later from booking history.", icon: CheckCircle2 },
];

const posterMovies = demoMovies.slice(0, 10);

export default function Home() {
  return (
    <AppShell>
      <section className="px-3 pb-10 pt-5 md:hidden">
        <div className="mb-5 flex items-center gap-6 overflow-x-auto whitespace-nowrap">
          <span className="border-b-4 border-accent-strong pb-2 text-xl font-semibold uppercase text-foreground">Now showing</span>
          <span className="pb-2 text-xl font-semibold uppercase text-muted">Kids</span>
          <span className="pb-2 text-xl font-semibold uppercase text-muted">Coming soon</span>
        </div>

        <div className="cinema-scrollbar-none overflow-x-auto scroll-smooth pb-2">
          <div className="flex snap-x snap-mandatory gap-3">
            {posterMovies.map((movie) => (
              <Link
                key={movie.id}
                href={`/movies/${movie.id}`}
                className="group shrink-0 basis-[calc((100%_-_0.75rem)/2)] snap-start overflow-hidden rounded-lg border border-accent-strong/55 bg-panel"
              >
                <div className="relative aspect-[2/3] bg-background">
                  <Image
                    src={movie.posterUrl || "/cinema-hero.png"}
                    alt={movie.title}
                    fill
                    sizes="50vw"
                    className="object-cover transition duration-300 group-hover:scale-[1.03]"
                  />
                </div>
                <div className="flex min-h-[96px] flex-col justify-center p-3 text-center">
                  <h2 className="line-clamp-2 text-base font-semibold leading-6">{movie.title}</h2>
                  <p className="mt-2 text-sm font-semibold text-accent">IMDb {movie.imdbRating?.toFixed(1)}</p>
                </div>
              </Link>
            ))}
          </div>
        </div>

        <Link href="/movies" className="mt-4 flex items-center justify-center gap-2 rounded-md bg-accent px-5 py-3 text-sm font-semibold text-background">
          View all movies
          <ArrowRight size={18} aria-hidden />
        </Link>

        <section className="mt-8">
          <p className="font-mono text-xs uppercase text-accent">Booking flow</p>
          <h2 className="mt-2 text-2xl font-semibold leading-tight">Pick a showtime, hold your chairs, then pay.</h2>
          <div className="mt-4 grid grid-cols-2 gap-3">
            {bookingFlow.map(({ title, icon: Icon }, index) => (
              <div key={title} className="rounded-lg border border-line bg-panel p-4">
                <div className="flex items-center justify-between">
                  <Icon className="text-accent" size={20} aria-hidden />
                  <span className="font-mono text-xs text-muted">{String(index + 1).padStart(2, "0")}</span>
                </div>
                <h3 className="mt-5 text-sm font-semibold">{title}</h3>
              </div>
            ))}
          </div>
        </section>
      </section>

      <section className="relative hidden min-h-[560px] overflow-hidden md:block md:min-h-[calc(100dvh-65px)]">
        <Image src="/cinema-hero.png" alt="Modern cinema lobby" fill priority sizes="100vw" className="object-cover" />
        <div className="absolute inset-0 bg-[linear-gradient(90deg,rgba(13,15,18,0.96)_0%,rgba(13,15,18,0.82)_43%,rgba(13,15,18,0.38)_100%)]" />
        <div className="relative mx-auto grid min-h-[560px] max-w-7xl content-center px-4 py-10 sm:px-6 md:min-h-[calc(100dvh-65px)] md:py-16">
          <div className="max-w-2xl">
            <p className="mb-4 font-mono text-xs uppercase text-accent">Modular monolith MVP</p>
            <h1 className="text-4xl font-semibold leading-tight text-foreground sm:text-5xl md:text-7xl">Cinema</h1>
            <p className="mt-5 max-w-xl text-base leading-7 text-muted sm:text-lg sm:leading-8">
              Browse showtimes, lock real seats temporarily, complete a mock payment, and keep your booking history in one place.
            </p>
            <div className="mt-8 flex flex-wrap gap-3">
              <Link href="/movies" className="flex items-center gap-2 rounded-md bg-accent px-5 py-3 text-sm font-semibold text-background">
                Book tickets
                <ArrowRight size={18} aria-hidden />
              </Link>
              <Link href="/bookings" className="flex items-center gap-2 rounded-md border border-line bg-background/55 px-5 py-3 text-sm font-semibold text-foreground hover:border-accent">
                Booking history
                <Ticket size={18} aria-hidden />
              </Link>
            </div>
          </div>
          <div className="mt-10 hidden max-w-4xl gap-3 sm:grid sm:grid-cols-3 md:mt-14">
            {heroFeatures.map(({ title, body, icon: Icon }) => (
              <div key={title} className="rounded-lg border border-line bg-background/72 p-4">
                <Icon className="mb-3 text-accent" size={20} aria-hidden />
                <h2 className="text-sm font-semibold">{title}</h2>
                <p className="mt-2 text-sm leading-6 text-muted">{body}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="mx-auto hidden max-w-7xl gap-6 px-4 py-10 sm:px-6 md:grid md:py-14 lg:grid-cols-[0.9fr_1.25fr]">
        <div>
          <p className="font-mono text-xs uppercase text-accent">Start here</p>
          <h2 className="mt-3 max-w-xl text-3xl font-semibold leading-tight sm:text-4xl md:text-5xl">Everything you need before the lights go down.</h2>
        </div>
        <div className="grid gap-3 sm:grid-cols-3">
          {bookingTools.map(({ title, body, icon: Icon, href }) => (
            <Link key={title} href={href} className="group rounded-lg border border-line bg-panel p-4 hover:border-accent/70 active:scale-[0.98]">
              <Icon className="text-accent" size={20} aria-hidden />
              <h3 className="mt-5 text-base font-semibold">{title}</h3>
              <p className="mt-2 text-sm leading-6 text-muted">{body}</p>
              <ArrowRight className="mt-5 text-muted transition duration-300 group-hover:translate-x-1 group-hover:text-accent" size={18} aria-hidden />
            </Link>
          ))}
        </div>
      </section>

      <section className="mx-auto hidden max-w-7xl gap-8 px-4 py-14 sm:px-6 lg:grid lg:grid-cols-[360px_1fr]">
        <div className="relative min-h-[520px] overflow-hidden rounded-lg border border-line bg-panel">
          <Image src={featuredMovie.posterUrl || "/cinema-hero.png"} alt={featuredMovie.title} fill sizes="360px" className="object-cover" />
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

      <section className="mx-auto hidden max-w-7xl px-4 py-10 sm:px-6 md:block md:py-14">
        <div className="mb-6 flex items-end justify-between gap-4">
          <div>
            <p className="font-mono text-xs uppercase text-accent">Real posters</p>
            <h2 className="mt-2 text-3xl font-semibold sm:text-4xl">Now showing</h2>
          </div>
          <Link href="/movies" className="hidden items-center gap-2 text-sm font-semibold text-accent sm:flex">
            Browse all
            <ArrowRight size={16} aria-hidden />
          </Link>
        </div>
        <div className="cinema-scrollbar-none overflow-x-auto scroll-smooth pb-2">
          <div className="flex snap-x snap-mandatory gap-3">
            {posterMovies.map((movie) => (
              <Link
                key={movie.id}
                href={`/movies/${movie.id}`}
                className="group shrink-0 basis-[calc((100%_-_0.75rem)/2)] snap-start overflow-hidden rounded-lg border border-line bg-panel md:basis-[calc((100%_-_1.5rem)/3)] lg:basis-[calc((100%_-_2.25rem)/4)]"
              >
              <div className="relative aspect-[2/3] bg-background">
                <Image
                  src={movie.posterUrl || "/cinema-hero.png"}
                  alt={movie.title}
                  fill
                    sizes="(max-width: 1024px) 33vw, 25vw"
                    className="object-cover transition group-hover:scale-[1.03]"
                />
              </div>
                <div className="flex min-h-[96px] flex-col justify-center p-3 text-center md:min-h-[110px] md:p-4">
                  <h3 className="line-clamp-2 text-base font-semibold leading-6 md:text-lg">{movie.title}</h3>
                  <p className="mt-2 text-sm font-semibold text-accent">IMDb {movie.imdbRating?.toFixed(1)}</p>
                </div>
              </Link>
            ))}
          </div>
        </div>
      </section>

      <section className="mx-auto hidden max-w-7xl px-4 pb-16 pt-4 sm:px-6 md:block md:pt-8">
        <div className="max-w-2xl">
          <p className="font-mono text-xs uppercase text-accent">Booking flow</p>
          <h2 className="mt-3 text-3xl font-semibold leading-tight sm:text-4xl">From poster to paid booking without losing the chair.</h2>
        </div>

        <div className="mt-8 grid grid-cols-2 gap-3 md:grid-cols-4">
          {bookingFlow.map(({ title, body, icon: Icon }, index) => (
            <article key={title} className="rounded-lg border border-line bg-panel p-4 md:p-5">
              <div className="flex items-center justify-between">
                <Icon className="text-accent" size={22} aria-hidden />
                <span className="font-mono text-xs text-muted">{String(index + 1).padStart(2, "0")}</span>
              </div>
              <h3 className="mt-5 text-base font-semibold md:mt-8 md:text-lg">{title}</h3>
              <p className="mt-2 text-sm leading-6 text-muted">{body}</p>
            </article>
          ))}
        </div>

        <div className="mt-4 hidden gap-4 lg:grid lg:grid-cols-[1.2fr_0.8fr]">
          <Link href="/movies" className="group relative min-h-[340px] overflow-hidden rounded-lg border border-line bg-panel">
            <Image
              src="/posters/godfather-hero-hd.jpg"
              alt="The Godfather cinema artwork"
              fill
              sizes="800px"
              className="object-cover opacity-72 transition duration-700 group-hover:scale-[1.03]"
            />
            <div className="absolute inset-0 bg-[linear-gradient(90deg,rgba(13,15,18,0.9)_0%,rgba(13,15,18,0.44)_100%)]" />
            <div className="relative flex min-h-[340px] max-w-xl flex-col justify-end p-6">
              <p className="font-mono text-xs uppercase text-accent">Ready to book</p>
              <h3 className="mt-3 text-3xl font-semibold leading-tight">Open the movie board and choose your session.</h3>
              <span className="mt-6 flex w-fit items-center gap-2 rounded-md bg-accent px-4 py-3 text-sm font-semibold text-background">
                See showtimes
                <ArrowRight size={17} aria-hidden />
              </span>
            </div>
          </Link>

          <aside className="rounded-lg border border-line bg-panel p-5">
            <p className="font-mono text-xs uppercase text-accent">Tonight</p>
            <h3 className="mt-3 text-2xl font-semibold">Quick details</h3>
            <div className="mt-6 space-y-5">
              <div className="flex items-start gap-3">
                <Clock3 className="mt-1 text-accent" size={19} aria-hidden />
                <div>
                  <p className="font-semibold">Fast seat map</p>
                  <p className="mt-1 text-sm leading-6 text-muted">Rows, prices, booked chairs, and selected chairs stay visible in one screen.</p>
                </div>
              </div>
              <div className="flex items-start gap-3">
                <LockKeyhole className="mt-1 text-accent" size={19} aria-hidden />
                <div>
                  <p className="font-semibold">Temporary lock</p>
                  <p className="mt-1 text-sm leading-6 text-muted">The backend checks availability again before payment confirmation.</p>
                </div>
              </div>
              <div className="flex items-start gap-3">
                <WalletCards className="mt-1 text-accent" size={19} aria-hidden />
                <div>
                  <p className="font-semibold">Mock checkout</p>
                  <p className="mt-1 text-sm leading-6 text-muted">Payment callbacks are handled once, even if the callback repeats.</p>
                </div>
              </div>
            </div>
          </aside>
        </div>
      </section>
    </AppShell>
  );
}
