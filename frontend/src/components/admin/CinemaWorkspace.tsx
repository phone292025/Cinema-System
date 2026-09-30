"use client";

import { Building2, MapPinned, Plus } from "lucide-react";
import type { FormEvent } from "react";

import type { Cinema } from "@/lib/types";
import { AdminForm, Input, InventoryPanel, InventoryRow, Panel, PanelHeader, Textarea, WorkspaceGrid } from "./ui";

export function CinemaWorkspace({
  cinemas,
  cinemaForm,
  setCinemaForm,
  onSubmit,
  isSaving,
}: {
  cinemas: Cinema[];
  cinemaForm: { name: string; location: string; address: string; city: string };
  setCinemaForm: React.Dispatch<React.SetStateAction<{ name: string; location: string; address: string; city: string }>>;
  onSubmit: (event: FormEvent) => void;
  isSaving: boolean;
}) {
  return (
    <WorkspaceGrid>
      <Panel>
        <PanelHeader eyebrow="Add branch" title="Cinema management" helper="Create cinema branches used by halls and showtimes." icon={<Building2 size={19} aria-hidden />} />
        <AdminForm onSubmit={onSubmit} buttonLabel="Add cinema branch" isSaving={isSaving} icon={<Plus size={17} aria-hidden />}>
          <div className="grid gap-3 lg:grid-cols-2">
            <Input label="Branch name" value={cinemaForm.name} onChange={(value) => setCinemaForm((current) => ({ ...current, name: value }))} />
            <Input label="City" value={cinemaForm.city} onChange={(value) => setCinemaForm((current) => ({ ...current, city: value }))} />
          </div>
          <Input label="Location" value={cinemaForm.location} onChange={(value) => setCinemaForm((current) => ({ ...current, location: value }))} />
          <Textarea label="Address" value={cinemaForm.address} onChange={(value) => setCinemaForm((current) => ({ ...current, address: value }))} />
        </AdminForm>
      </Panel>

      <InventoryPanel title="Cinema branches" eyebrow="Venues" count={cinemas.length} icon={<MapPinned size={18} aria-hidden />}>
        {cinemas.map((cinema) => (
          <InventoryRow key={cinema.id} title={cinema.name} meta={`${cinema.location} · ${cinema.city}`} detail={cinema.address} />
        ))}
      </InventoryPanel>
    </WorkspaceGrid>
  );
}
