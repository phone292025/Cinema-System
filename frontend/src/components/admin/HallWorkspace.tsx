"use client";

import { Armchair, Rows3, Theater } from "lucide-react";
import type { FormEvent } from "react";

import type { Cinema, Hall, SeatType } from "@/lib/types";
import { HallForm, MAX_HALL_COLUMNS, MAX_HALL_ROWS, seatTypeOptions } from "./model";
import { AdminForm, Input, InventoryPanel, InventoryRow, Panel, PanelHeader, SeatPreview, Select, WorkspaceGrid } from "./ui";

export function HallWorkspace({
  cinemas,
  halls,
  hallForm,
  setHallForm,
  loadHalls,
  onSubmit,
  isSaving,
  seatPreviewRows,
  seatPreviewColumns,
  generatedSeatCount,
}: {
  cinemas: Cinema[];
  halls: Hall[];
  hallForm: HallForm;
  setHallForm: React.Dispatch<React.SetStateAction<HallForm>>;
  loadHalls: (cinemaId: string) => void;
  onSubmit: (event: FormEvent) => void;
  isSaving: boolean;
  seatPreviewRows: number;
  seatPreviewColumns: number;
  generatedSeatCount: number;
}) {
  return (
    <WorkspaceGrid>
      <Panel>
        <PanelHeader eyebrow="Create hall" title="Hall and seat management" helper="The hall and its seat layout are created together, so a hall never exists without seats." icon={<Rows3 size={19} aria-hidden />} />
        <AdminForm onSubmit={onSubmit} buttonLabel="Create hall and seats" isSaving={isSaving} icon={<Theater size={17} aria-hidden />}>
          <Select
            label="Cinema branch"
            value={hallForm.cinemaId}
            onChange={(value) => {
              setHallForm((current) => ({ ...current, cinemaId: value }));
              loadHalls(value);
            }}
            options={cinemas.map((cinema) => ({ value: cinema.id, label: cinema.name }))}
          />
          <div className="grid gap-3 sm:grid-cols-2">
            <Input label="Hall name" value={hallForm.name} onChange={(value) => setHallForm((current) => ({ ...current, name: value }))} />
            <Input label="Hall type" value={hallForm.type} onChange={(value) => setHallForm((current) => ({ ...current, type: value }))} />
          </div>
          <div className="grid gap-3 sm:grid-cols-2">
            <Input label="Rows (A–Z)" type="number" min={1} max={MAX_HALL_ROWS} value={hallForm.totalRows} onChange={(value) => setHallForm((current) => ({ ...current, totalRows: value }))} />
            <Input label="Seats per row" type="number" min={1} max={MAX_HALL_COLUMNS} value={hallForm.totalColumns} onChange={(value) => setHallForm((current) => ({ ...current, totalColumns: value }))} />
          </div>
          <Select
            label="Seat type"
            value={hallForm.defaultSeatType}
            onChange={(value) => setHallForm((current) => ({ ...current, defaultSeatType: (value || "REGULAR") as SeatType }))}
            options={seatTypeOptions}
          />
        </AdminForm>

        <div className="mt-5 rounded-lg border border-line bg-background p-4">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <p className="font-semibold">Automatic seat preview</p>
              <p className="mt-1 text-sm text-muted">{generatedSeatCount} {hallForm.defaultSeatType.toLowerCase()} seats will be created with the hall.</p>
            </div>
            <span className="rounded-md bg-accent/12 px-3 py-2 font-mono text-sm text-accent">
              {hallForm.totalRows} × {hallForm.totalColumns}
            </span>
          </div>
          <SeatPreview rows={seatPreviewRows} columns={seatPreviewColumns} />
        </div>
      </Panel>

      <InventoryPanel title="Halls in selected branch" eyebrow="Layouts" count={halls.length} icon={<Armchair size={18} aria-hidden />}>
        {halls.map((hall) => (
          <InventoryRow key={hall.id} title={`${hall.name} · ${hall.type}`} meta={`${hall.totalRows} rows × ${hall.totalColumns} columns`} detail={`${hall.totalRows * hall.totalColumns} seats`} />
        ))}
      </InventoryPanel>
    </WorkspaceGrid>
  );
}
