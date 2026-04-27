ALTER TABLE movies ADD COLUMN IF NOT EXISTS imdb_rating NUMERIC(3, 1);

UPDATE movies SET imdb_rating = 0 WHERE imdb_rating IS NULL;

CREATE INDEX IF NOT EXISTS idx_movies_imdb_rating ON movies(imdb_rating DESC);
