import type { Movie } from "./types";

// Hand-picked HD artwork for titles in the seeded catalogue. This is presentation only: which
// movies exist, their ids and their details always come from the API.
export type ShowcaseSlide = {
  title: string;
  eyebrow: string;
  description: string;
  image: string;
};

export const showcaseSlides: ShowcaseSlide[] = [
  {
    title: "The Shawshank Redemption",
    eyebrow: "Top IMDb-rated release",
    description: "A sharp prison-drama classic with the same showtime and chair selection flow.",
    image: "/posters/shawshank-hero-hd.jpg",
  },
  {
    title: "The Godfather",
    eyebrow: "Crime drama classic",
    description: "Browse the Corleone family epic, then continue into live showtimes and seat locking.",
    image: "/posters/godfather-hero-hd.jpg",
  },
  {
    title: "The Dark Knight",
    eyebrow: "Action crime feature",
    description: "Pick a Gotham showtime, choose your chair, and finish checkout from one flow.",
    image: "/posters/dark-knight-hero-hd.jpg",
  },
  {
    title: "The Lord of the Rings: The Return of the King",
    eyebrow: "Fantasy adventure",
    description: "A big-screen journey with premium seats, real poster art, and fast booking.",
    image: "/posters/return-king-hero-hd.jpg",
  },
  {
    title: "Pulp Fiction",
    eyebrow: "Cult crime classic",
    description: "Jump into a sharp Los Angeles crime story and reserve seats from the same carousel.",
    image: "/posters/pulp-fiction-hero-hd.png",
  },
  {
    title: "The Good, the Bad and the Ugly",
    eyebrow: "Western landmark",
    description: "A widescreen classic with real poster art, showtimes, and quick chair selection.",
    image: "/posters/good-bad-ugly-hero-hd.jpg",
  },
];

export const FALLBACK_ARTWORK = "/cinema-hero.png";

function titleKey(value: string) {
  return value.trim().toLowerCase();
}

export function findMovieByTitle(movies: Movie[] | undefined, title: string) {
  const key = titleKey(title);
  return movies?.find((movie) => titleKey(movie.title) === key);
}

/** Slides whose movie is currently in the public catalogue, paired with that movie. */
export function liveShowcase(movies: Movie[]) {
  return showcaseSlides.flatMap((slide) => {
    const movie = findMovieByTitle(movies, slide.title);
    return movie ? [{ ...slide, movie }] : [];
  });
}

export function backdropFor(movie: Movie) {
  const slide = showcaseSlides.find((item) => titleKey(item.title) === titleKey(movie.title));
  return slide?.image ?? movie.posterUrl ?? FALLBACK_ARTWORK;
}

export function posterFor(movie: Movie) {
  return movie.posterUrl || FALLBACK_ARTWORK;
}

const OPTIMIZABLE_HOSTS = ["upload.wikimedia.org"];

/**
 * Admins can paste a poster URL from any host, but next/image only optimises the hosts listed in
 * next.config.ts. Anything else is loaded as-is rather than rendering a broken image.
 */
export function skipImageOptimization(src: string) {
  if (src.startsWith("/")) return false;
  try {
    return !OPTIMIZABLE_HOSTS.includes(new URL(src).hostname);
  } catch {
    return true;
  }
}
