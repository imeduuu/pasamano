-- RF-CAT-01: categorías de catálogo, con slug único y jerarquía de hasta dos niveles.
CREATE TABLE categories (
    id uuid PRIMARY KEY,
    name varchar(80) NOT NULL,
    slug varchar(100) NOT NULL UNIQUE,
    sort_order integer NOT NULL CHECK (sort_order >= 0),
    active boolean NOT NULL DEFAULT true,
    parent_id uuid REFERENCES categories(id) ON DELETE RESTRICT,
    CONSTRAINT ck_categories_not_own_parent CHECK (parent_id IS NULL OR parent_id <> id)
);

CREATE INDEX ix_categories_parent_order ON categories(parent_id, sort_order, name);
CREATE INDEX ix_categories_active ON categories(active);

-- La regla del árbol se garantiza también ante escrituras fuera del modelo de dominio.
CREATE FUNCTION enforce_category_depth() RETURNS trigger AS $$
BEGIN
    IF NEW.parent_id IS NOT NULL AND EXISTS (
        SELECT 1 FROM categories parent WHERE parent.id = NEW.parent_id AND parent.parent_id IS NOT NULL
    ) THEN
        RAISE EXCEPTION USING ERRCODE = '23514', MESSAGE = 'Una categoría de nivel 2 no puede tener hijas';
    END IF;
    IF NEW.parent_id IS NOT NULL AND NOT EXISTS (
        SELECT 1 FROM categories parent WHERE parent.id = NEW.parent_id
    ) THEN
        RAISE EXCEPTION USING ERRCODE = '23503', MESSAGE = 'La categoría padre no existe';
    END IF;
    IF TG_OP = 'UPDATE' AND NEW.parent_id IS NOT NULL AND EXISTS (
        SELECT 1 FROM categories child WHERE child.parent_id = NEW.id
    ) THEN
        RAISE EXCEPTION USING ERRCODE = '23514', MESSAGE = 'Una categoría con hijas no puede convertirse en categoría de nivel 2';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tr_categories_depth
    BEFORE INSERT OR UPDATE OF parent_id ON categories
    FOR EACH ROW EXECUTE FUNCTION enforce_category_depth();
