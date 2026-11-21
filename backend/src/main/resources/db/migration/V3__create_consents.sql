-- Consentimiento informado al registrarse (CU-01 paso 4, RNF-LEG-01).
CREATE TABLE consents (
    id              uuid        PRIMARY KEY,
    user_id         uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    terms_version   varchar(20) NOT NULL,
    privacy_version varchar(20) NOT NULL,
    accepted_at     timestamptz NOT NULL
);

CREATE INDEX ix_consents_user ON consents (user_id);
