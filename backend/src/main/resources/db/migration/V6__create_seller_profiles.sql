-- HU-07 / CU-15 / RF-USR-02: perfil vendedor y aceptación de términos de venta.
CREATE TABLE seller_profiles (
    user_id              uuid         PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    store_name           varchar(40)  NOT NULL,
    normalized_store_name varchar(40) NOT NULL,
    terms_version        varchar(20)  NOT NULL,
    terms_accepted_at    timestamptz  NOT NULL,
    CONSTRAINT ck_seller_profiles_store_name_length CHECK (char_length(btrim(store_name)) BETWEEN 3 AND 40),
    CONSTRAINT ck_seller_profiles_normalized CHECK (normalized_store_name = lower(btrim(store_name)))
);

CREATE UNIQUE INDEX ux_seller_profiles_store_name ON seller_profiles (normalized_store_name);
