"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { LogIn } from "lucide-react";
import { FormEvent, useState } from "react";

import { AuthFrame } from "@/components/AuthFrame";
import { apiFetch, storeAuth } from "@/lib/api";
import type { AuthResponse } from "@/lib/types";

export default function LoginPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const router = useRouter();

  async function submit(event: FormEvent) {
    event.preventDefault();
    setError("");
    try {
      const auth = await apiFetch<AuthResponse>("/auth/login", {
        method: "POST",
        body: JSON.stringify({ email, password }),
      });
      storeAuth(auth);
      const next = new URLSearchParams(window.location.search).get("next");
      router.push(next?.startsWith("/") && !next.startsWith("//") ? next : "/movies");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Login failed.");
    }
  }

  return (
    <AuthFrame
      mode="login"
      title="Sign in"
      subtitle="Access your tickets, booking history, and seat locks from one account."
      visualTitle="Your seats are waiting"
      visualCopy="Pick a showtime, lock your chair, and finish checkout before the lights dim."
    >
      <form onSubmit={submit} className="space-y-6">
        <label className="block text-sm font-medium text-foreground" htmlFor="email">
          Email address
          <input
            id="email"
            value={email}
            type="email"
            autoComplete="email"
            required
            onChange={(event) => setEmail(event.target.value)}
            className="mt-2 w-full border-0 border-b border-line bg-transparent px-0 py-3 text-base text-foreground outline-none placeholder:text-muted/60 focus:border-accent"
            placeholder="you@example.com"
          />
        </label>

        <label className="block text-sm font-medium text-foreground" htmlFor="password">
          Password
          <input
            id="password"
            value={password}
            type="password"
            autoComplete="current-password"
            required
            onChange={(event) => setPassword(event.target.value)}
            className="mt-2 w-full border-0 border-b border-line bg-transparent px-0 py-3 text-base text-foreground outline-none placeholder:text-muted/60 focus:border-accent"
            placeholder="Enter your password"
          />
        </label>

        {error && <p className="rounded-md border border-danger/40 bg-danger/10 p-3 text-sm text-danger">{error}</p>}

        <button
          type="submit"
          className="flex w-full items-center justify-center gap-2 rounded-md bg-accent px-4 py-3 font-semibold text-background hover:bg-accent-strong hover:text-foreground active:scale-[0.98]"
        >
          <LogIn size={18} aria-hidden />
          Sign in
        </button>

        <p className="text-sm text-muted">
          New here?{" "}
          <Link href="/register" className="font-semibold text-accent hover:text-foreground">
            Create an account
          </Link>
        </p>
      </form>
    </AuthFrame>
  );
}
