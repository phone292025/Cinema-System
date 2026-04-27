import type { Movie, SeatAvailability, SeatAvailabilityResponse, Showtime } from "./types";

export const topRatedMovies: Movie[] = [
  {
    id: "the-shawshank-redemption",
    title: "The Shawshank Redemption",
    description: "Two imprisoned men build an unlikely friendship across decades inside Shawshank State Penitentiary.",
    durationMinutes: 142,
    genre: "Drama",
    language: "English",
    rating: "R",
    imdbRating: 9.3,
    posterUrl: "/posters/shawshank-redemption.jpg",
    releaseDate: "1994-09-23",
    status: "NOW_SHOWING",
  },
  {
    id: "the-godfather",
    title: "The Godfather",
    description: "The aging patriarch of a crime dynasty transfers control of his empire to his reluctant son.",
    durationMinutes: 175,
    genre: "Crime Drama",
    language: "English",
    rating: "R",
    imdbRating: 9.2,
    posterUrl: "/posters/godfather.jpg",
    releaseDate: "1972-03-24",
    status: "NOW_SHOWING",
  },
  {
    id: "the-dark-knight",
    title: "The Dark Knight",
    description: "Batman faces a criminal mastermind who pushes Gotham City and its heroes into chaos.",
    durationMinutes: 152,
    genre: "Action Crime",
    language: "English",
    rating: "PG-13",
    imdbRating: 9.0,
    posterUrl: "/posters/dark-knight.jpg",
    releaseDate: "2008-07-18",
    status: "NOW_SHOWING",
  },
  {
    id: "the-godfather-part-ii",
    title: "The Godfather Part II",
    description: "The Corleone family expands its power while the past reveals Vito Corleone's rise.",
    durationMinutes: 202,
    genre: "Crime Drama",
    language: "English",
    rating: "R",
    imdbRating: 9.0,
    posterUrl: "/posters/godfather-part-ii.jpg",
    releaseDate: "1974-12-20",
    status: "NOW_SHOWING",
  },
  {
    id: "12-angry-men",
    title: "12 Angry Men",
    description: "A dissenting juror forces a tense room to reconsider the fate of a young defendant.",
    durationMinutes: 96,
    genre: "Legal Drama",
    language: "English",
    rating: "Approved",
    imdbRating: 9.0,
    posterUrl: "/posters/12-angry-men.jpg",
    releaseDate: "1957-04-10",
    status: "NOW_SHOWING",
  },
  {
    id: "the-return-of-the-king",
    title: "The Lord of the Rings: The Return of the King",
    description: "The fellowship's final battle decides the fate of Middle-earth and the One Ring.",
    durationMinutes: 201,
    genre: "Fantasy Adventure",
    language: "English",
    rating: "PG-13",
    imdbRating: 9.0,
    posterUrl: "/posters/return-of-the-king.jpg",
    releaseDate: "2003-12-17",
    status: "NOW_SHOWING",
  },
  {
    id: "schindlers-list",
    title: "Schindler's List",
    description: "A German industrialist saves more than a thousand Jewish refugees during the Holocaust.",
    durationMinutes: 195,
    genre: "Historical Drama",
    language: "English",
    rating: "R",
    imdbRating: 9.0,
    posterUrl: "/posters/schindlers-list.jpg",
    releaseDate: "1993-12-15",
    status: "NOW_SHOWING",
  },
  {
    id: "pulp-fiction",
    title: "Pulp Fiction",
    description: "Los Angeles criminals, boxers, and fixers collide in a fractured crime story.",
    durationMinutes: 154,
    genre: "Crime Drama",
    language: "English",
    rating: "R",
    imdbRating: 8.9,
    posterUrl: "/posters/pulp-fiction.jpg",
    releaseDate: "1994-10-14",
    status: "NOW_SHOWING",
  },
  {
    id: "the-fellowship-of-the-ring",
    title: "The Lord of the Rings: The Fellowship of the Ring",
    description: "A hobbit joins a fellowship tasked with carrying the One Ring toward Mordor.",
    durationMinutes: 178,
    genre: "Fantasy Adventure",
    language: "English",
    rating: "PG-13",
    imdbRating: 8.9,
    posterUrl: "/posters/fellowship-of-the-ring.jpg",
    releaseDate: "2001-12-19",
    status: "NOW_SHOWING",
  },
  {
    id: "the-good-the-bad-and-the-ugly",
    title: "The Good, the Bad and the Ugly",
    description: "Three gunslingers hunt for buried gold during the American Civil War.",
    durationMinutes: 161,
    genre: "Western",
    language: "Italian",
    rating: "R",
    imdbRating: 8.8,
    posterUrl: "/posters/good-bad-ugly.jpg",
    releaseDate: "1966-12-23",
    status: "NOW_SHOWING",
  },
  {
    id: "the-super-mario-bros-movie",
    title: "The Super Mario Bros. Movie",
    description: "Brooklyn plumbers Mario and Luigi land in the Mushroom Kingdom and join Princess Peach to stop Bowser.",
    durationMinutes: 92,
    genre: "Family Animation",
    language: "English",
    rating: "PG",
    imdbRating: 7.0,
    posterUrl: "/posters/super-mario-bros-movie.jpg",
    releaseDate: "2023-04-05",
    status: "NOW_SHOWING",
  },
  {
    id: "tom-and-jerry",
    title: "Tom & Jerry",
    description: "Tom and Jerry bring their rivalry into a New York hotel before a high-profile wedding.",
    durationMinutes: 101,
    genre: "Family Comedy",
    language: "English",
    rating: "PG",
    imdbRating: 5.2,
    posterUrl: "/posters/tom-and-jerry-2021.jpg",
    releaseDate: "2021-02-26",
    status: "NOW_SHOWING",
  },
];

export const featuredMovie = topRatedMovies[0];
export const demoMovies: Movie[] = topRatedMovies;

export const projectHailMaryMovie: Movie = {
  id: "project-hail-mary",
  title: "Project Hail Mary",
  description:
    "Ryland Grace wakes alone on a deep-space mission to the Tau Ceti system and must solve a planet-saving mystery before Earth freezes.",
  durationMinutes: 156,
  genre: "Sci-Fi",
  language: "English",
  rating: "PG-13",
  imdbRating: 0,
  posterUrl: "/posters/project-hail-mary.jpg",
  releaseDate: "2026-03-20",
  status: "COMING_SOON",
};

const fallbackMoviePool = [...demoMovies, projectHailMaryMovie];

export const demoShowtimes: Showtime[] = fallbackMoviePool.flatMap((movie, index) => {
  const startHour = 14 + (index % 7);
  const basePrice = movie.id === projectHailMaryMovie.id ? 22 : (movie.imdbRating ?? 0) >= 9 ? 18 : 16;
  const firstId = movie.id === projectHailMaryMovie.id ? "project-hail-mary-imax-1800" : `${movie.id}-imax-1800`;
  const secondId = movie.id === projectHailMaryMovie.id ? "project-hail-mary-imax-2130" : `${movie.id}-vip-2130`;

  return [
    createDemoShowtime(movie, firstId, startHour, basePrice),
    createDemoShowtime(movie, secondId, startHour + 4, basePrice + 2),
  ];
});

export function findDemoMovie(id: string): Movie | undefined {
  return fallbackMoviePool.find((movie) => movie.id === id);
}

export function findDemoShowtimes(movieId: string): Showtime[] {
  return demoShowtimes.filter((showtime) => showtime.movieId === movieId);
}

export function findDemoSeatAvailability(showtimeId: string): SeatAvailabilityResponse | undefined {
  if (!demoShowtimes.some((showtime) => showtime.id === showtimeId)) {
    return undefined;
  }

  const seats: SeatAvailability[] = [];
  for (const row of ["A", "B", "C", "D", "E", "F"]) {
    for (let number = 1; number <= 8; number += 1) {
      const premium = row === "E" || row === "F";
      const booked = (row === "C" && number === 4) || (row === "C" && number === 5);
      seats.push({
        seatId: `${showtimeId}-${row}${number}`,
        rowLabel: row,
        seatNumber: number,
        seatType: premium ? "PREMIUM" : "REGULAR",
        price: premium ? 22 : 18,
        status: booked ? "BOOKED" : "AVAILABLE",
      });
    }
  }

  return { showtimeId, seats };
}

function createDemoShowtime(movie: Movie, id: string, hour: number, basePrice: number): Showtime {
  const start = new Date(Date.UTC(2026, 3, 25 + Math.floor(hour / 24), hour % 24, 0, 0));
  const end = new Date(start.getTime() + movie.durationMinutes * 60_000);

  return {
    id,
    movieId: movie.id,
    movieTitle: movie.title,
    cinemaId: "central-cineplex",
    cinemaName: "Central Cineplex",
    hallId: "hall-imax-1",
    hallName: "Hall 1 IMAX",
    startTime: start.toISOString(),
    endTime: end.toISOString(),
    basePrice,
    status: "SCHEDULED",
  };
}
