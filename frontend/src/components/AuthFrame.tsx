import Image from "next/image";
import Link from "next/link";
import { ArrowLeft, Clapperboard, Sparkles } from "lucide-react";
import type { ReactNode } from "react";

type AuthFrameProps = {
  mode: "login" | "register";
  title: string;
  subtitle: string;
  visualTitle: string;
  visualCopy: string;
  children: ReactNode;
};

export function AuthFrame({ mode, title, subtitle, visualTitle, visualCopy, children }: AuthFrameProps) {
  const isLogin = mode === "login";

  return (
    <main className="relative min-h-[100dvh] overflow-hidden bg-background">
      <Image
        src="/auth-cinema-visual.png"
        alt="A cinema lobby with red curtains, warm lights, and popcorn."
        fill
        priority
        sizes="100vw"
        className="object-cover"
      />
      <div className="absolute inset-0 bg-[linear-gradient(90deg,rgba(13,15,18,0.88),rgba(13,15,18,0.34)_48%,rgba(13,15,18,0.82)),linear-gradient(0deg,rgba(13,15,18,0.78),rgba(13,15,18,0.08)_54%,rgba(13,15,18,0.42))]" />
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_72%_42%,rgba(244,184,96,0.18),transparent_28%),radial-gradient(circle_at_18%_8%,rgba(233,92,75,0.18),transparent_32%)]" />

      <section className="relative grid min-h-[100dvh] w-full gap-8 p-5 pt-24 sm:p-8 sm:pt-28 lg:grid-cols-[minmax(0,1fr)_minmax(390px,520px)] lg:items-center lg:p-12 xl:p-16">
        <Link
          href="/"
          className="absolute left-5 top-5 flex w-fit items-center gap-2 rounded-md border border-white/15 bg-background/35 px-3 py-2 text-sm text-foreground backdrop-blur hover:border-accent hover:text-accent sm:left-8 sm:top-8 lg:left-12 lg:top-12 xl:left-16 xl:top-16"
        >
          <ArrowLeft size={16} aria-hidden />
          Back
        </Link>

        <div className="max-w-2xl lg:pr-8">
          <p className="flex items-center gap-2 font-mono text-xs uppercase text-accent">
            <Clapperboard size={15} aria-hidden />
            Cinema membership
          </p>
          <h2 className="mt-4 max-w-[11ch] text-5xl font-semibold leading-none text-foreground sm:text-6xl lg:text-7xl xl:text-8xl">
            {visualTitle}
          </h2>
          <p className="mt-5 max-w-md text-base leading-7 text-foreground/86 sm:text-lg">
            {visualCopy}
          </p>
        </div>

        <div className="w-full rounded-lg border border-white/15 bg-[linear-gradient(180deg,rgba(17,19,23,0.82),rgba(9,10,12,0.88))] p-5 shadow-[0_28px_80px_-42px_rgba(0,0,0,0.95)] backdrop-blur-xl sm:p-8 lg:p-10">
          <div className="mb-9 grid grid-cols-2 gap-2 rounded-md border border-line bg-background/45 p-1">
            <Link
              href="/login"
              className={`rounded-md px-3 py-2 text-center text-sm font-semibold ${
                isLogin ? "bg-accent text-background" : "text-muted hover:bg-panel hover:text-foreground"
              }`}
            >
              Sign in
            </Link>
            <Link
              href="/register"
              className={`rounded-md px-3 py-2 text-center text-sm font-semibold ${
                !isLogin ? "bg-accent text-background" : "text-muted hover:bg-panel hover:text-foreground"
              }`}
            >
              Create account
            </Link>
          </div>

          <div className="mb-8">
            <p className="flex items-center gap-2 font-mono text-xs uppercase text-accent">
              <Sparkles size={15} aria-hidden />
              {isLogin ? "Welcome back" : "Start here"}
            </p>
            <h1 className="mt-3 text-4xl font-semibold leading-tight text-foreground sm:text-5xl">
              {title}
            </h1>
            <p className="mt-3 max-w-md leading-7 text-muted">{subtitle}</p>
          </div>

          {children}
        </div>
      </section>
    </main>
  );
}
