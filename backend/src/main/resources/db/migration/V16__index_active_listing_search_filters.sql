-- RNF-REN-02: acelerar filtros combinados y orden paginado de publicaciones activas.
CREATE INDEX ix_listings_active_region_condition_recent
    ON listings (region, condition, published_at DESC NULLS LAST, id)
    WHERE status = 'ACTIVA';

CREATE INDEX ix_listings_active_price_id
    ON listings (price, id)
    WHERE status = 'ACTIVA';

CREATE INDEX ix_listings_active_recent_id
    ON listings (published_at DESC NULLS LAST, id)
    WHERE status = 'ACTIVA';
