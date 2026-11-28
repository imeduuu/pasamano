-- RF-CAR-05: el carrito persistente se identifica por comprador; Redis se reserva para F2 (RF-CAR-06).
CREATE TABLE carts (
    buyer_id   uuid PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE cart_items (
    id         uuid PRIMARY KEY,
    buyer_id   uuid NOT NULL REFERENCES carts(buyer_id) ON DELETE CASCADE,
    listing_id uuid NOT NULL,
    title      varchar(80) NOT NULL,
    seller_id  uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    quantity   integer NOT NULL,
    unit_price bigint NOT NULL,
    CONSTRAINT uq_cart_items_listing UNIQUE (buyer_id, listing_id),
    CONSTRAINT ck_cart_items_quantity CHECK (quantity > 0),
    CONSTRAINT ck_cart_items_price CHECK (unit_price BETWEEN 100 AND 99999999)
);

CREATE INDEX ix_cart_items_listing ON cart_items (listing_id);
