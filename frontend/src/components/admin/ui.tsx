"use client";

import { Armchair, ChevronRight, Loader2, Sparkles } from "lucide-react";
import { useMemo, useState } from "react";
import type { FormEvent, ReactNode } from "react";

export function Occupancy({ sold, total }: { sold?: number | null; total?: number | null }) {
  if (!total) return null;
  const soldSeats = sold ?? 0;
  const percent = Math.round((soldSeats / total) * 100);

  return (
    <div className="w-36 shrink-0">
      <div className="flex items-baseline justify-between gap-2 text-sm">
        <span className="font-mono text-accent">
          {soldSeats}/{total}
        </span>
        <span className="text-xs text-muted">{percent}% full</span>
      </div>
      <div className="mt-1.5 h-1.5 overflow-hidden rounded-full bg-line">
        <div className="h-full rounded-full bg-accent" style={{ width: `${Math.min(100, percent)}%` }} />
      </div>
    </div>
  );
}

export function AccessRequired() {
  return (
    <div className="mt-6 rounded-lg border border-danger/40 bg-danger/10 p-6">
      <p className="font-mono text-xs uppercase text-danger">Admin access required</p>
      <h2 className="mt-3 text-3xl font-semibold">Login as an admin or staff user</h2>
      <p className="mt-3 max-w-2xl text-muted">This area manages ticket sales, movies, branches, halls, seat layouts, and showtimes.</p>
    </div>
  );
}

export function WorkspaceGrid({ children }: { children: ReactNode }) {
  return <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_430px]">{children}</div>;
}

export function Panel({ children }: { children: ReactNode }) {
  return <section className="rounded-lg border border-line bg-panel/72 p-5 shadow-[0_24px_80px_rgba(0,0,0,0.22)] sm:p-6">{children}</section>;
}

export function PanelHeader({ eyebrow, title, helper, icon }: { eyebrow: string; title: string; helper: string; icon: ReactNode }) {
  return (
    <div className="flex items-start justify-between gap-4">
      <div>
        <p className="font-mono text-xs uppercase text-accent">{eyebrow}</p>
        <h2 className="mt-2 text-2xl font-semibold sm:text-3xl">{title}</h2>
        <p className="mt-2 max-w-2xl text-sm leading-6 text-muted">{helper}</p>
      </div>
      <span className="grid size-11 shrink-0 place-items-center rounded-md border border-line bg-background text-accent">{icon}</span>
    </div>
  );
}

export function StatusStrip({ error, success }: { error: string; success: string }) {
  if (!error && !success) return null;
  return (
    <div
      role={error ? "alert" : "status"}
      className={`mt-5 rounded-md border p-4 ${error ? "border-danger/40 bg-danger/10 text-danger" : "border-success/40 bg-success/10 text-success"}`}
    >
      {error || success}
    </div>
  );
}

export function MetricCard({ icon, label, value, detail }: { icon: ReactNode; label: string; value: ReactNode; detail: string }) {
  return (
    <article className="rounded-lg border border-line bg-panel/80 p-4 transition duration-300 hover:-translate-y-1 hover:border-accent/60">
      <div className="flex items-center justify-between gap-3">
        <p className="text-sm text-muted">{label}</p>
        <span className="grid size-9 place-items-center rounded-md bg-background text-accent">{icon}</span>
      </div>
      <p className="mt-4 font-mono text-2xl font-semibold text-accent">{value}</p>
      <p className="mt-1 text-sm text-muted">{detail}</p>
    </article>
  );
}

export function RevenueLine({ label, value, strong = false }: { label: string; value: string; strong?: boolean }) {
  return (
    <div className="flex items-center justify-between gap-4 rounded-md border border-line bg-background p-4">
      <p className="text-sm text-muted">{label}</p>
      <p className={`font-mono font-semibold ${strong ? "text-3xl text-accent" : "text-lg text-foreground"}`}>{value}</p>
    </div>
  );
}

export function AdminForm({
  onSubmit,
  buttonLabel,
  icon,
  isSaving,
  children,
}: {
  onSubmit: (event: FormEvent) => void;
  buttonLabel: string;
  icon: ReactNode;
  isSaving: boolean;
  children: ReactNode;
}) {
  return (
    <form onSubmit={onSubmit} className="mt-5">
      <div className="grid gap-4">{children}</div>
      <button
        type="submit"
        disabled={isSaving}
        className="group mt-5 flex w-full items-center justify-between rounded-md bg-accent px-4 py-3 font-semibold text-background transition active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-60"
      >
        <span className="flex items-center gap-2">
          {isSaving ? <Loader2 size={17} className="animate-spin" aria-hidden /> : icon}
          {isSaving ? "Saving" : buttonLabel}
        </span>
        <span className="grid size-8 place-items-center rounded-md bg-background/15 transition duration-300 group-hover:translate-x-1">
          <ChevronRight size={17} aria-hidden />
        </span>
      </button>
    </form>
  );
}

export function Input({
  label,
  value,
  onChange,
  type = "text",
  min,
  max,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  type?: string;
  min?: number;
  max?: number;
}) {
  return (
    <label className="block text-sm font-medium text-muted">
      {label}
      <input
        type={type}
        min={min}
        max={max}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="mt-2 w-full rounded-md border border-line bg-background px-3 py-3 text-foreground outline-none transition duration-300 placeholder:text-muted/60 focus:border-accent focus:ring-2 focus:ring-accent/20"
      />
    </label>
  );
}

export function Textarea({ label, value, onChange }: { label: string; value: string; onChange: (value: string) => void }) {
  return (
    <label className="block text-sm font-medium text-muted">
      {label}
      <textarea
        value={value}
        onChange={(event) => onChange(event.target.value)}
        rows={4}
        className="mt-2 w-full resize-none rounded-md border border-line bg-background px-3 py-3 text-foreground outline-none transition duration-300 placeholder:text-muted/60 focus:border-accent focus:ring-2 focus:ring-accent/20"
      />
    </label>
  );
}

export function Select({
  label,
  value,
  onChange,
  options,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  options: Array<{ value: string; label: string }>;
}) {
  return (
    <label className="block text-sm font-medium text-muted">
      {label}
      <select
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="mt-2 w-full rounded-md border border-line bg-background px-3 py-3 text-foreground outline-none transition duration-300 focus:border-accent focus:ring-2 focus:ring-accent/20"
      >
        <option value="">Select</option>
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
    </label>
  );
}

export function FilterableInventoryPanel<T>({
  title,
  eyebrow,
  icon,
  items,
  matches,
  renderItem,
  searchPlaceholder,
  pageSize = 20,
}: {
  title: string;
  eyebrow: string;
  icon: ReactNode;
  items: T[];
  matches: (item: T, query: string) => boolean;
  renderItem: (item: T) => ReactNode;
  searchPlaceholder: string;
  pageSize?: number;
}) {
  const [query, setQuery] = useState("");
  const [visible, setVisible] = useState(pageSize);

  const filtered = useMemo(() => {
    const needle = query.trim().toLowerCase();
    return needle ? items.filter((item) => matches(item, needle)) : items;
  }, [items, matches, query]);

  const shown = filtered.slice(0, visible);

  return (
    <Panel>
      <PanelHeader
        eyebrow={eyebrow}
        title={title}
        helper={filtered.length === 1 ? "1 item" : `${filtered.length} items`}
        icon={icon}
      />
      <label className="sr-only" htmlFor={`search-${eyebrow}`}>
        {searchPlaceholder}
      </label>
      <input
        id={`search-${eyebrow}`}
        value={query}
        onChange={(event) => {
          setQuery(event.target.value);
          setVisible(pageSize);
        }}
        placeholder={searchPlaceholder}
        className="mt-4 w-full rounded-md border border-line bg-background px-3 py-2.5 text-sm outline-none focus:border-accent"
      />
      <div className="mt-4 max-h-[640px] space-y-3 overflow-y-auto pr-1 cinema-scrollbar-none">
        {shown.length > 0 ? (
          shown.map(renderItem)
        ) : (
          <EmptyState label="Nothing matches" helper="Try a different search, or clear the box." />
        )}
      </div>
      {filtered.length > shown.length && (
        <button
          type="button"
          onClick={() => setVisible((current) => current + pageSize)}
          className="mt-4 w-full rounded-md border border-line px-4 py-2.5 text-sm font-semibold text-muted hover:border-accent hover:text-accent"
        >
          Show {Math.min(pageSize, filtered.length - shown.length)} more of {filtered.length}
        </button>
      )}
    </Panel>
  );
}

export function InventoryPanel({ title, eyebrow, count, icon, children }: { title: string; eyebrow: string; count: number; icon: ReactNode; children: ReactNode }) {
  return (
    <Panel>
      <PanelHeader eyebrow={eyebrow} title={title} helper={count === 1 ? "1 item" : `${count} items`} icon={icon} />
      <div className="mt-5 max-h-[640px] space-y-3 overflow-y-auto pr-1 cinema-scrollbar-none">
        {count > 0 ? children : <EmptyState label="No records yet" helper="Create the first item from the form." />}
      </div>
    </Panel>
  );
}

export function InventoryRow({ title, meta, detail }: { title: string; meta: string; detail: string }) {
  return (
    <article className="rounded-md border border-line bg-background p-4">
      <div className="flex items-start justify-between gap-4">
        <div className="min-w-0">
          <p className="truncate font-semibold">{title}</p>
          <p className="mt-1 truncate text-sm text-muted">{meta}</p>
        </div>
        <span className="shrink-0 rounded-md bg-panel px-2.5 py-1 font-mono text-xs text-accent">{detail}</span>
      </div>
    </article>
  );
}

export function SeatPreview({ rows, columns }: { rows: number; columns: number }) {
  return (
    <div className="mt-4 overflow-x-auto cinema-scrollbar-none">
      <div className="min-w-max rounded-md border border-line bg-panel p-3">
        <div className="mb-3 rounded-md border border-line bg-background py-2 text-center font-mono text-xs uppercase text-muted">Screen</div>
        <div className="grid gap-2" style={{ gridTemplateColumns: `repeat(${columns}, minmax(24px, 1fr))` }}>
          {Array.from({ length: rows * columns }).map((_, index) => (
            <span key={index} className="grid size-6 place-items-center rounded border border-line bg-background text-accent">
              <Armchair size={13} aria-hidden />
            </span>
          ))}
        </div>
      </div>
    </div>
  );
}

export function EmptyState({ label, helper }: { label: string; helper: string }) {
  return (
    <div className="rounded-md border border-dashed border-line bg-background p-5 text-center">
      <Sparkles size={22} className="mx-auto text-accent" aria-hidden />
      <p className="mt-3 font-semibold">{label}</p>
      <p className="mt-1 text-sm text-muted">{helper}</p>
    </div>
  );
}

export function clamp(value: number, min: number, max: number) {
  return Math.min(max, Math.max(min, value));
}
