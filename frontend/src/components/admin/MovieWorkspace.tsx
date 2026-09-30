"use client";

import { Film, Plus, Search } from "lucide-react";
import type { FormEvent } from "react";

import { StatusBadge } from "@/components/StatusBadge";
import type { Movie } from "@/lib/types";
import { AdminForm, FilterableInventoryPanel, Input, Panel, PanelHeader, Select, Textarea, WorkspaceGrid } from "./ui";

export function MovieWorkspace({
  movies,
  movieForm,
  setMovieForm,
  onSubmit,
  isSaving,
  editingMovieId,
  onEdit,
  onCancelEdit,
  onSetStatus,
}: {
  editingMovieId: string | null;
  onEdit: (movie: Movie) => void;
  onCancelEdit: () => void;
  onSetStatus: (movie: Movie, status: Movie["status"]) => void;
  movies: Movie[];
  movieForm: {
    title: string;
    description: string;
    durationMinutes: string;
    genre: string;
    language: string;
    rating: string;
    imdbRating: string;
    posterUrl: string;
    releaseDate: string;
    status: string;
  };
  setMovieForm: React.Dispatch<React.SetStateAction<{
    title: string;
    description: string;
    durationMinutes: string;
    genre: string;
    language: string;
    rating: string;
    imdbRating: string;
    posterUrl: string;
    releaseDate: string;
    status: string;
  }>>;
  onSubmit: (event: FormEvent) => void;
  isSaving: boolean;
}) {
  return (
    <WorkspaceGrid>
      <Panel>
        <PanelHeader
          eyebrow={editingMovieId ? "Edit movie" : "Add movie"}
          title="Movie management"
          helper={
            editingMovieId
              ? "Change the details customers see, then save."
              : "Create the customer-facing movie card and detail page data."
          }
          icon={<Film size={19} aria-hidden />}
        />
        {editingMovieId && (
          <button
            type="button"
            onClick={onCancelEdit}
            className="mt-4 rounded-md border border-line px-3 py-1.5 text-xs font-semibold text-muted hover:border-accent hover:text-accent"
          >
            Cancel edit and add a new movie instead
          </button>
        )}
        <AdminForm
          onSubmit={onSubmit}
          buttonLabel={editingMovieId ? "Save changes" : "Add movie"}
          isSaving={isSaving}
          icon={<Plus size={17} aria-hidden />}
        >
          <div className="grid gap-3 lg:grid-cols-2">
            <Input label="Poster URL" value={movieForm.posterUrl} onChange={(value) => setMovieForm((current) => ({ ...current, posterUrl: value }))} />
            <Input label="Title" value={movieForm.title} onChange={(value) => setMovieForm((current) => ({ ...current, title: value }))} />
          </div>
          <Textarea label="Description" value={movieForm.description} onChange={(value) => setMovieForm((current) => ({ ...current, description: value }))} />
          <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
            <Input label="Duration minutes" type="number" value={movieForm.durationMinutes} onChange={(value) => setMovieForm((current) => ({ ...current, durationMinutes: value }))} />
            <Input label="Genre" value={movieForm.genre} onChange={(value) => setMovieForm((current) => ({ ...current, genre: value }))} />
            <Input label="Language" value={movieForm.language} onChange={(value) => setMovieForm((current) => ({ ...current, language: value }))} />
            <Input label="Age rating" value={movieForm.rating} onChange={(value) => setMovieForm((current) => ({ ...current, rating: value }))} />
          </div>
          <div className="grid gap-3 sm:grid-cols-3">
            <Input label="IMDb rating" type="number" value={movieForm.imdbRating} onChange={(value) => setMovieForm((current) => ({ ...current, imdbRating: value }))} />
            <Input label="Release date" type="date" value={movieForm.releaseDate} onChange={(value) => setMovieForm((current) => ({ ...current, releaseDate: value }))} />
            <Select
              label="Status"
              value={movieForm.status}
              onChange={(value) => setMovieForm((current) => ({ ...current, status: value }))}
              options={[
                { value: "NOW_SHOWING", label: "Now showing" },
                { value: "COMING_SOON", label: "Coming soon" },
                { value: "ARCHIVED", label: "Archived" },
              ]}
            />
          </div>
        </AdminForm>
      </Panel>

      <FilterableInventoryPanel
        title="Current movies"
        eyebrow="Catalog"
        icon={<Search size={18} aria-hidden />}
        items={movies}
        searchPlaceholder="Search by title or genre"
        matches={(movie, query) =>
          `${movie.title} ${movie.genre ?? ""} ${movie.status}`.toLowerCase().includes(query)
        }
        renderItem={(movie) => (
          <article key={movie.id} className="rounded-md border border-line bg-background p-4">
            <div className="flex items-start justify-between gap-4">
              <div className="min-w-0">
                <p className="truncate font-semibold">{movie.title}</p>
                <p className="mt-1 truncate text-sm text-muted">
                  {movie.genre} · {movie.rating} · {movie.durationMinutes} min
                </p>
              </div>
              <StatusBadge status={movie.status} />
            </div>
            <div className="mt-3 flex flex-wrap gap-2">
              <button
                type="button"
                onClick={() => onEdit(movie)}
                className="rounded-md border border-line px-3 py-1.5 text-xs font-semibold text-muted hover:border-accent hover:text-accent"
              >
                {editingMovieId === movie.id ? "Editing" : "Edit"}
              </button>
              {movie.status === "ARCHIVED" ? (
                <button
                  type="button"
                  onClick={() => onSetStatus(movie, "NOW_SHOWING")}
                  className="rounded-md border border-line px-3 py-1.5 text-xs font-semibold text-muted hover:border-success hover:text-success"
                >
                  Put back on sale
                </button>
              ) : (
                <button
                  type="button"
                  onClick={() => onSetStatus(movie, "ARCHIVED")}
                  className="rounded-md border border-line px-3 py-1.5 text-xs font-semibold text-muted hover:border-danger hover:text-danger"
                >
                  Archive
                </button>
              )}
            </div>
          </article>
        )}
      />
    </WorkspaceGrid>
  );
}
