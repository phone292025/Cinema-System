import Image from "next/image";
import Link from "next/link";
import { ArrowRight, CalendarDays, CheckCircle2, Clock3, Film, LockKeyhole, Ticket, WalletCards } from "lucide-react";
import type { LucideIcon } from "lucide-react";

import { AppShell } from "@/components/AppShell";
import { demoMovies, featuredMovie } from "@/lib/demo-data";

const heroMovie = featuredMovie;
const posterRail = demoMovies.slice(0, 8);
const sideMovies = [demoMovies[2], demoMovies[1], demoMovies[10]].filter(Boolean);

const bookingTools: { title: string; body: string; icon: LucideIcon; href: string }[] = [
  { title: "Browse showtimes", body: "Start with the movie list and jump straight to a session.", icon: Film, href: "/movies" },
  { title: "Lock seats", body: "Choose the exact chairs before checkout starts.", icon: LockKeyhole, href: `/movies/${heroMovie.id}` },
  { title: "Booking history", body: "Check paid bookings and saved confirmations.", icon: Ticket, href: "/bookings" },
];

const flow: { title: string; body: string; icon: LucideIcon }[] = [
  { title: "Pick a session", body: "Choose a movie, day, hall, and showtime.", icon: CalendarDays },
  { title: "Hold your chairs", body: "Selected seats are reserved temporarily while you pay.", icon: LockKeyhole },
  { title: "Mock payment", body: "Complete checkout and keep one booking record.", icon: WalletCards },
  { title: "Confirmed", body: "Return later from booking history.", icon: CheckCircle2 },
];

export default function HomePageCinematicRedesign() {
  return (
    <AppShell>
      <section className="relative min-h-[calc(100dvh-65px)] overflow-hidden bg-[#080908]">
        <Image
          src="/posters/dark-knight-hero-hd.jpg"
          alt="The Dark Knight cinema artwork"
          fill
          priority
          sizes="100vw"
          className="object-cover opacity-55 [object-position:center_38%]"
        />
        <div className="absolute inset-0 bg-[linear-gradient(90deg,rgba(8,9,8,0.98)_0%,rgba(8,9,8,0.86)_42%,rgba(8,9,8,0.42)_100%)]" />
        <div className="absolute inset-x-0 bottom-0 h-40 bg-[linear-gradient(180deg,rgba(8,9,8,0)_0%,#080908_78%)]" />

        <div className="relative mx-auto grid min-h-[calc(100dvh-65px)] max-w-7xl content-center gap-10 px-4 py-10 sm:px-6 lg:grid-cols-[minmax(0,1fr)_420px]">
          <div className="max-w-3xl">
            <p className="font-mono text-xs uppercase text-accent">Cinema booking system</p>
            <h1 className="mt-4 max-w-4xl text-5xl font-semibold leading-none text-foreground sm:text-7xl lg:text-8xl">Pick the movie. Hold the seat.</h1>
            <p className="mt-6 max-w-2xl text-lg leading-8 text-[#c7d5dd]">
              Browse real posters, choose a showtime, lock your exact chair, and finish checkout from one clean cinema flow.
            </p>
            <div className="mt-8 flex flex-wrap gap-3">
              <Link
                href="/movies"
                className="group flex items-center gap-3 rounded-md bg-accent px-5 py-3 text-sm font-semibold text-background hover:bg-[#ffd184] active:scale-[0.98]"
              >
                Browse movies
                <span className="grid size-7 place-items-center rounded-md bg-background/15 transition-transform duration-300 group-hover:translate-x-1">
                  <ArrowRight size={17} aria-hidden />
                </span>
              </Link>
              <Link
                href="/bookings"
                className="flex items-center gap-2 rounded-md border border-white/18 bg-white/[0.04] px-5 py-3 text-sm font-semibold text-foreground hover:border-accent hover:text-accent active:scale-[0.98]"
              >
                My bookings
                <Ticket size={17} aria-hidden />
              </Link>
            </div>

            <dl className="mt-10 grid max-w-2xl grid-cols-3 gap-3">
              <div className="border-l border-accent/60 pl-3">
                <dt className="text-2xl font-semibold text-foreground">5 min</dt>
                <dd className="mt-1 text-xs leading-5 text-muted">temporary seat hold</dd>
              </div>
              <div className="border-l border-accent/60 pl-3">
                <dt className="text-2xl font-semibold text-foreground">10</dt>
                <dd className="mt-1 text-xs leading-5 text-muted">poster picks</dd>
              </div>
              <div className="border-l border-accent/60 pl-3">
                <dt className="text-2xl font-semibold text-foreground">1</dt>
                <dd className="mt-1 text-xs leading-5 text-muted">booking truth</dd>
              </div>
            </dl>
          </div>

          <aside className="rounded-lg border border-white/12 bg-[#101316]/92 p-3">
            <div className="relative overflow-hidden rounded-md border border-white/10 bg-panel">
              <div className="relative aspect-[4/5]">
                <Image src={heroMovie.posterUrl || "/cinema-hero.png"} alt={heroMovie.title} fill sizes="420px" className="object-cover" />
              </div>
              <div className="absolute inset-x-0 bottom-0 bg-[linear-gradient(180deg,rgba(0,0,0,0)_0%,rgba(8,9,8,0.96)_60%)] p-5 pt-20">
                <p className="font-mono text-xs uppercase text-accent">Top IMDb pick</p>
                <h2 className="mt-2 text-2xl font-semibold leading-tight">{heroMovie.title}</h2>
                <div className="mt-3 flex flex-wrap gap-2 text-xs text-[#c7d5dd]">
                  <span>{heroMovie.genre}</span>
                  <span>{heroMovie.rating}</span>
                  <span>{heroMovie.durationMinutes} min</span>
                  <span className="text-accent">IMDb {heroMovie.imdbRating?.toFixed(1)}</span>
                </div>
              </div>
            </div>
            <div className="mt-3 grid grid-cols-3 gap-2">
              {sideMovies.map((movie) => (
                <Link key={movie.id} href={`/movies/${movie.id}`} className="group relative aspect-[2/3] overflow-hidden rounded-md border border-white/10 bg-panel">
                  <Image src={movie.posterUrl || "/cinema-hero.png"} alt={movie.title} fill sizes="130px" className="object-cover transition duration-500 group-hover:scale-[1.04]" />
                </Link>
              ))}
            </div>
          </aside>
        </div>
      </section>

      <section className="bg-[#080908] px-4 py-16 sm:px-6">
        <div className="mx-auto grid max-w-7xl gap-4 lg:grid-cols-[1fr_1.25fr]">
          <div>
            <p className="font-mono text-xs uppercase text-accent">Start here</p>
            <h2 className="mt-3 max-w-xl text-4xl font-semibold leading-tight sm:text-5xl">Everything you need before the lights go down.</h2>
          </div>
          <div className="grid gap-3 sm:grid-cols-3">
            {bookingTools.map(({ title, body, icon: Icon, href }) => (
              <Link key={title} href={href} className="group rounded-lg border border-line bg-[#11161a] p-4 hover:border-accent/60 active:scale-[0.98]">
                <Icon className="text-accent" size={20} aria-hidden />
                <h3 className="mt-5 text-base font-semibold">{title}</h3>
                <p className="mt-2 text-sm leading-6 text-muted">{body}</p>
                <ArrowRight className="mt-5 text-muted transition duration-300 group-hover:translate-x-1 group-hover:text-accent" size={18} aria-hidden />
              </Link>
            ))}
          </div>
        </div>
      </section>

      <section className="overflow-hidden bg-[#0d0f12] py-16">
        <div className="mx-auto max-w-7xl px-4 sm:px-6">
          <div className="flex items-end justify-between gap-4">
            <div>
              <p className="font-mono text-xs uppercase text-accent">Real posters</p>
              <h2 className="mt-3 text-4xl font-semibold">Now showing</h2>
            </div>
            <Link href="/movies" className="hidden items-center gap-2 rounded-md border border-line px-4 py-2 text-sm font-semibold text-muted hover:border-accent hover:text-accent sm:flex">
              View all
              <ArrowRight size={16} aria-hidden />
            </Link>
          </div>
        </div>

        <div className="cinema-scrollbar-none mt-8 flex gap-4 overflow-x-auto px-4 pb-3 sm:px-6 lg:px-[calc((100vw-80rem)/2+1.5rem)]">
          {posterRail.map((movie) => (
            <Link
              key={movie.id}
              href={`/movies/${movie.id}`}
              className="group w-[230px] shrink-0 overflow-hidden rounded-lg border border-line bg-panel sm:w-[270px]"
            >
              <div className="relative aspect-[2/3]">
                <Image src={movie.posterUrl || "/cinema-hero.png"} alt={movie.title} fill sizes="270px" className="object-cover transition duration-500 group-hover:scale-[1.04]" />
                <div className="absolute left-3 top-3 rounded-md bg-background/80 px-2 py-1 text-xs font-semibold text-accent">
                  IMDb {movie.imdbRating?.toFixed(1)}
                </div>
              </div>
              <div className="p-4">
                <h3 className="line-clamp-2 min-h-12 text-base font-semibold leading-6">{movie.title}</h3>
                <p className="mt-3 text-sm text-muted">
                  {movie.genre} · {movie.durationMinutes} min
                </p>
              </div>
            </Link>
          ))}
        </div>
      </section>

      <section className="bg-[#080908] px-4 py-16 sm:px-6">
        <div className="mx-auto max-w-7xl">
          <div className="max-w-2xl">
            <p className="font-mono text-xs uppercase text-accent">Booking flow</p>
            <h2 className="mt-3 text-4xl font-semibold leading-tight">From poster to paid booking without losing the chair.</h2>
          </div>

          <div className="mt-8 grid gap-3 md:grid-cols-4">
            {flow.map(({ title, body, icon: Icon }, index) => (
              <article key={title} className="rounded-lg border border-line bg-[#11161a] p-5">
                <div className="flex items-center justify-between">
                  <Icon className="text-accent" size={22} aria-hidden />
                  <span className="font-mono text-xs text-muted">{String(index + 1).padStart(2, "0")}</span>
                </div>
                <h3 className="mt-8 text-lg font-semibold">{title}</h3>
                <p className="mt-2 text-sm leading-6 text-muted">{body}</p>
              </article>
            ))}
          </div>

          <div className="mt-4 grid gap-4 lg:grid-cols-[1.2fr_0.8fr]">
            <Link href="/movies" className="group relative min-h-[360px] overflow-hidden rounded-lg border border-line bg-panel">
              <Image src="/posters/godfather-hero-hd.jpg" alt="The Godfather cinema artwork" fill sizes="800px" className="object-cover opacity-72 transition duration-700 group-hover:scale-[1.03]" />
              <div className="absolute inset-0 bg-[linear-gradient(90deg,rgba(8,9,8,0.9)_0%,rgba(8,9,8,0.38)_100%)]" />
              <div className="relative flex min-h-[360px] max-w-xl flex-col justify-end p-6">
                <p className="font-mono text-xs uppercase text-accent">Ready to book</p>
                <h3 className="mt-3 text-3xl font-semibold leading-tight">Open the movie board and choose your session.</h3>
                <span className="mt-6 flex w-fit items-center gap-2 rounded-md bg-accent px-4 py-3 text-sm font-semibold text-background">
                  See showtimes
                  <ArrowRight size={17} aria-hidden />
                </span>
              </div>
            </Link>

            <aside className="rounded-lg border border-line bg-[#11161a] p-5">
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
        </div>
      </section>
    </AppShell>
  );
}
