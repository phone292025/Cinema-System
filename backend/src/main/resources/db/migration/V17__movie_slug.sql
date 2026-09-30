ALTER TABLE movies ADD COLUMN slug VARCHAR(220);

UPDATE movies
SET slug = trim(both '-' from regexp_replace(lower(title), '[^a-z0-9]+', '-', 'g'));

ALTER TABLE movies ALTER COLUMN slug SET NOT NULL;

CREATE INDEX idx_movies_slug ON movies(slug);
