-- RF-PUB-01, RN-05 y RN-12: publicaciones, control optimista e imágenes ordenadas.
CREATE TABLE listings (
    id              uuid PRIMARY KEY,
    title           varchar(80) NOT NULL,
    description     varchar(4000) NOT NULL,
    price           bigint NOT NULL,
    stock_total     integer NOT NULL,
    stock_reserved  integer NOT NULL,
    condition       varchar(20) NOT NULL,
    category_id     uuid NOT NULL REFERENCES categories(id) ON DELETE RESTRICT,
    region          varchar(60) NOT NULL,
    commune         varchar(60) NOT NULL,
    seller_id       uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    status          varchar(20) NOT NULL,
    version         bigint NOT NULL DEFAULT 0,
    CONSTRAINT ck_listings_price CHECK (price BETWEEN 100 AND 99999999),
    CONSTRAINT ck_listings_stock CHECK (stock_total >= 0 AND stock_reserved BETWEEN 0 AND stock_total),
    CONSTRAINT ck_listings_condition CHECK (condition IN ('NUEVO', 'USADO')),
    CONSTRAINT ck_listings_status CHECK (status IN ('BORRADOR', 'ACTIVA', 'PAUSADA', 'AGOTADA', 'BLOQUEADA', 'ELIMINADA'))
);

CREATE INDEX ix_listings_seller ON listings (seller_id);
CREATE INDEX ix_listings_status ON listings (status);
CREATE INDEX ix_listings_category ON listings (category_id);

CREATE TABLE listing_images (
    id              uuid PRIMARY KEY,
    listing_id      uuid NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    position        integer NOT NULL CHECK (position BETWEEN 0 AND 7),
    image_reference varchar(500) NOT NULL,
    CONSTRAINT uq_listing_images_position UNIQUE (listing_id, position)
);
