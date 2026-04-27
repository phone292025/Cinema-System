"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { UserPlus } from "lucide-react";
import { FormEvent, useState } from "react";

import { AuthFrame } from "@/components/AuthFrame";
import { apiFetch, storeAuth } from "@/lib/api";
import type { AuthResponse } from "@/lib/types";

const fields = [
  { key: "name", label: "Full name", type: "text", autoComplete: "name", placeholder: "Your name" },
  { key: "email", label: "Email address", type: "email", autoComplete: "email", placeholder: "you@example.com" },
  { key: "phone", label: "Mobile number", type: "tel", autoComplete: "tel", placeholder: "+60 12 345 6789" },
  { key: "password", label: "Password", type: "password", autoComplete: "new-password", placeholder: "Create a password" },
] as const;

export default function RegisterPage() {
  const [form, setForm] = useState({ name: "", email: "", phone: "", password: "" });
  const [error, setError] = useState("");
  const router = useRouter();

  async function submit(event: FormEvent) {
    event.preventDefault();
    setError("");
    try {
      const auth = await apiFetch<AuthResponse>("/auth/register", {
        method: "POST",
        body: JSON.stringify(form),
      });
      storeAuth(auth);
      router.push("/movies");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Registration failed.");
    }
  }

  return (
    <AuthFrame
      mode="register"
      title="Create account"
      subtitle="Save your profile once, then move from movie to seat map without retyping details."
      visualTitle="Make movie night simple"
      visualCopy="Register, choose your cinema, and keep every booking ready for the next visit."
    >
      <form onSubmit={submit} className="space-y-5">
        {fields.map((field) => (
          <label key={field.key} className="block text-sm font-medium text-foreground">
            {field.label}
            <input
              value={form[field.key]}
              type={field.type}
              autoComplete={field.autoComplete}
              required
              minLength={field.key === "password" ? 8 : undefined}
              onChange={(event) => setForm((current) => ({ ...current, [field.key]: event.target.value }))}
              className="mt-2 w-full border-0 border-b border-line bg-transparent px-0 py-3 text-base text-foreground outline-none placeholder:text-muted/60 focus:border-accent"
              placeholder={field.placeholder}
            />
          </label>
        ))}

        {error && <p className="rounded-md border border-danger/40 bg-danger/10 p-3 text-sm text-danger">{error}</p>}

        <button
          type="submit"
          className="flex w-full items-center justify-center gap-2 rounded-md bg-accent px-4 py-3 font-semibold text-background hover:bg-accent-strong hover:text-foreground active:scale-[0.98]"
        >
          <UserPlus size={18} aria-hidden />
          Create account
        </button>

        <p className="text-sm text-muted">
          Already registered?{" "}
          <Link href="/login" className="font-semibold text-accent hover:text-foreground">
            Sign in
          </Link>
        </p>
      </form>
    </AuthFrame>
  );
}
