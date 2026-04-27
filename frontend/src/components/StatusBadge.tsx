export function StatusBadge({ status }: { status: string }) {
  const tone =
    status === "PAID" || status === "SUCCEEDED" || status === "AVAILABLE"
      ? "border-success/40 bg-success/10 text-success"
      : status === "LOCKED" || status === "PENDING"
        ? "border-accent/40 bg-accent/10 text-accent"
        : status === "BOOKED" || status === "FAILED" || status === "EXPIRED"
          ? "border-danger/40 bg-danger/10 text-danger"
          : "border-line bg-panel text-muted";

  return (
    <span className={`inline-flex items-center rounded-md border px-2 py-1 text-xs font-medium ${tone}`}>
      {status.replaceAll("_", " ")}
    </span>
  );
}
