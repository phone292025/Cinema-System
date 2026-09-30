import { Loader2, RotateCcw } from "lucide-react";
import type { ReactNode } from "react";

export function InlineError({ message, className = "" }: { message: string; className?: string }) {
  if (!message) return null;
  return (
    <p role="alert" className={`rounded-md border border-danger/40 bg-danger/10 p-4 text-danger ${className}`}>
      {message}
    </p>
  );
}

type ErrorStateProps = {
  title: string;
  message: string;
  onRetry?: () => void;
  retrying?: boolean;
  children?: ReactNode;
  className?: string;
};

export function ErrorState({ title, message, onRetry, retrying = false, children, className = "" }: ErrorStateProps) {
  return (
    <div role="alert" className={`rounded-lg border border-danger/40 bg-danger/10 p-5 ${className}`}>
      <p className="font-semibold text-danger">{title}</p>
      <p className="mt-1 text-sm text-muted">{message}</p>
      {(onRetry || children) && (
        <div className="mt-4 flex flex-wrap items-center gap-3">
          {onRetry && (
            <button
              type="button"
              onClick={onRetry}
              disabled={retrying}
              className="flex items-center gap-2 rounded-md border border-danger/50 px-4 py-2 text-sm font-semibold text-danger hover:bg-danger/10 disabled:cursor-not-allowed disabled:opacity-60"
            >
              {retrying ? <Loader2 size={16} className="animate-spin" aria-hidden /> : <RotateCcw size={16} aria-hidden />}
              {retrying ? "Retrying" : "Retry"}
            </button>
          )}
          {children}
        </div>
      )}
    </div>
  );
}

export function LoadingState({ label, className = "" }: { label: string; className?: string }) {
  return (
    <div role="status" className={`flex items-center gap-3 text-sm text-muted ${className}`}>
      <Loader2 size={18} className="animate-spin text-accent" aria-hidden />
      {label}
    </div>
  );
}
