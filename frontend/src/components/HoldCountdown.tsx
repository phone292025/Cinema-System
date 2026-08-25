"use client";

import { AlarmClock } from "lucide-react";
import { useEffect, useState } from "react";

function secondsLeft(expiresAt?: string) {
  if (!expiresAt) return null;
  return Math.max(0, Math.round((new Date(expiresAt).getTime() - Date.now()) / 1000));
}

export function HoldCountdown({ expiresAt, onExpire }: { expiresAt?: string; onExpire?: () => void }) {
  const [remaining, setRemaining] = useState<number | null>(() => secondsLeft(expiresAt));

  useEffect(() => {
    if (!expiresAt) return undefined;

    const timer = window.setInterval(() => {
      const next = secondsLeft(expiresAt);
      setRemaining(next);
      if (next === 0) {
        window.clearInterval(timer);
        onExpire?.();
      }
    }, 1000);

    return () => window.clearInterval(timer);
  }, [expiresAt, onExpire]);

  if (remaining === null) return null;

  const minutes = Math.floor(remaining / 60);
  const seconds = remaining % 60;
  const expired = remaining === 0;
  const urgent = !expired && remaining <= 60;

  return (
    <div
      role="status"
      aria-live={urgent ? "assertive" : "polite"}
      className={`flex items-center gap-3 rounded-md border px-4 py-3 ${
        expired
          ? "border-danger/40 bg-danger/10 text-danger"
          : urgent
            ? "border-danger/40 bg-danger/10 text-danger"
            : "border-accent/30 bg-accent/10 text-accent"
      }`}
    >
      <AlarmClock size={18} aria-hidden />
      {expired ? (
        <p className="text-sm font-medium">Your hold has expired. These seats are back on sale.</p>
      ) : (
        <p className="text-sm font-medium">
          Seats held for{" "}
          <span className="font-mono text-base tabular-nums">
            {minutes}:{String(seconds).padStart(2, "0")}
          </span>
        </p>
      )}
    </div>
  );
}
