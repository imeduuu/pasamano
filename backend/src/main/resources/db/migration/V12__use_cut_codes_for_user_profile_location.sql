-- Decisión #13: migrar la ubicación del perfil a códigos CUT, dejando en NULL los
-- nombres que no coincidan exactamente tras normalizar mayúsculas, espacios y tildes.
ALTER TABLE users ADD COLUMN region_code varchar(2);
ALTER TABLE users ADD COLUMN commune_code varchar(5);

WITH normalized_regions AS (
    SELECT code,
           regexp_replace(translate(lower(btrim(name)), 'áéíóúüñ', 'aeiouun'), '\s+', ' ', 'g') AS normalized_name
    FROM regions
), normalized_communes AS (
    SELECT code, region_code,
           regexp_replace(translate(lower(btrim(name)), 'áéíóúüñ', 'aeiouun'), '\s+', ' ', 'g') AS normalized_name
    FROM communes
), mapped_regions AS (
    SELECT u.id, r.code
    FROM users u
    JOIN normalized_regions r
      ON r.normalized_name = regexp_replace(translate(lower(btrim(u.region)), 'áéíóúüñ', 'aeiouun'), '\s+', ' ', 'g')
), mapped_locations AS (
    SELECT u.id, mr.code AS region_code, c.code AS commune_code
    FROM users u
    JOIN mapped_regions mr ON mr.id = u.id
    JOIN normalized_communes old_commune
      ON old_commune.region_code = mr.code
     AND old_commune.normalized_name = regexp_replace(translate(lower(btrim(u.commune)), 'áéíóúüñ', 'aeiouun'), '\s+', ' ', 'g')
    JOIN communes c ON c.code = old_commune.code
)
UPDATE users u
SET region_code = mapped.region_code,
    commune_code = mapped.commune_code
FROM mapped_locations mapped
WHERE mapped.id = u.id;

ALTER TABLE users DROP COLUMN region;
ALTER TABLE users DROP COLUMN commune;

ALTER TABLE communes ADD CONSTRAINT uq_communes_region_code UNIQUE (region_code, code);
ALTER TABLE users
    ADD CONSTRAINT ck_users_location_pair CHECK ((region_code IS NULL) = (commune_code IS NULL)),
    ADD CONSTRAINT fk_users_location_region FOREIGN KEY (region_code) REFERENCES regions(code) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_users_location_commune FOREIGN KEY (region_code, commune_code)
        REFERENCES communes(region_code, code) ON DELETE RESTRICT;
