-- RF-BUS-04: registrar el instante de primera publicación para ordenar por recientes.
CREATE FUNCTION set_listing_published_at() RETURNS trigger AS $$
BEGIN
    IF NEW.status = 'ACTIVA' AND (TG_OP = 'INSERT' OR OLD.status <> 'ACTIVA') THEN
        NEW.published_at = COALESCE(NEW.published_at, now());
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_listings_published_at
    BEFORE INSERT OR UPDATE OF status ON listings
    FOR EACH ROW EXECUTE FUNCTION set_listing_published_at();
