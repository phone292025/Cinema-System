"use client";

import { ShieldCheck } from "lucide-react";

import { formatDateTime } from "@/lib/format";
import type { AuditLog } from "@/lib/types";
import { EmptyState, Panel, PanelHeader } from "./ui";

export function AuditWorkspace({ logs }: { logs: AuditLog[] }) {
  return (
    <Panel>
      <PanelHeader eyebrow="Audit trail" title="System activity" helper="Append-only records from booking, payment, ticket, and admin workflows." icon={<ShieldCheck size={19} aria-hidden />} />
      <div className="mt-5 overflow-x-auto cinema-scrollbar-none">
        <table className="min-w-full text-left text-sm">
          <thead className="font-mono text-xs uppercase text-muted">
            <tr className="border-b border-line">
              <th className="py-3 pr-4">Time</th>
              <th className="py-3 pr-4">Action</th>
              <th className="py-3 pr-4">Actor</th>
              <th className="py-3 pr-4">Entity</th>
              <th className="py-3">Value</th>
            </tr>
          </thead>
          <tbody>
            {logs.map((log) => (
              <tr key={log.id} className="border-b border-line/70">
                <td className="py-3 pr-4 text-muted">{formatDateTime(log.createdAt)}</td>
                <td className="py-3 pr-4 font-semibold">{log.action.replaceAll("_", " ")}</td>
                <td className="py-3 pr-4 text-muted">{log.actorRole ?? "SYSTEM"}</td>
                <td className="py-3 pr-4 text-muted">
                  {log.entityType}
                  {log.entityId ? ` ${log.entityId.slice(0, 8)}` : ""}
                </td>
                <td className="max-w-[280px] truncate py-3 text-muted">{log.newValue ?? log.oldValue ?? "-"}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {logs.length === 0 ? <EmptyState label="No audit rows yet" helper="Important actions will appear here once the system is used." /> : null}
      </div>
    </Panel>
  );
}
