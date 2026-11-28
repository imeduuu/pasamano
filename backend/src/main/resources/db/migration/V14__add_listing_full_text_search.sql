-- HU-12, ADR-004: búsqueda de texto completo y filtros frecuentes del catálogo.
CREATE EXTENSION IF NOT EXISTS unaccent;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- unaccent() no se declara inmutable por PostgreSQL; este wrapper permite indexarla.
CREATE OR REPLACE FUNCTION immutable_unaccent(text)
RETURNS text
LANGUAGE sql
IMMUTABLE
PARALLEL SAFE
STRICT
AS $$
    SELECT public.unaccent('public.unaccent', $1)
$$;

ALTER TABLE listings
    ADD COLUMN search_vector tsvector
        GENERATED ALWAYS AS (
            setweight(to_tsvector('spanish', immutable_unaccent(title)), 'A') ||
            setweight(to_tsvector('spanish', immutable_unaccent(description)), 'B')
        ) STORED,
    ADD COLUMN published_at timestamptz;

CREATE INDEX ix_listings_search_vector ON listings USING GIN (search_vector);
CREATE INDEX ix_listings_title_trgm ON listings USING GIN (immutable_unaccent(title) gin_trgm_ops);

-- status y category_id ya cuentan con índices dedicados desde V9.
CREATE INDEX ix_listings_price ON listings (price);
CREATE INDEX ix_listings_region ON listings (region);
CREATE INDEX ix_listings_published_at ON listings (published_at DESC);
