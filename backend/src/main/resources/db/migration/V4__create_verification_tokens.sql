-- Tokens de un solo uso: verificación de correo (24 h) y recuperación de contraseña (1 h).
-- Solo se guarda el hash SHA-256 (hex) del token enviado por correo.
CREATE TABLE verification_tokens (
    id         uuid        PRIMARY KEY,
    user_id    uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    purpose    varchar(30) NOT NULL,
    token_hash varchar(64) NOT NULL,
    expires_at timestamptz NOT NULL,
    used_at    timestamptz,
    created_at timestamptz NOT NULL,
    CONSTRAINT ck_verification_tokens_purpose CHECK (purpose IN ('EMAIL_VERIFICATION', 'PASSWORD_RESET'))
);

CREATE UNIQUE INDEX ux_verification_tokens_hash ON verification_tokens (token_hash);
CREATE INDEX ix_verification_tokens_user_purpose ON verification_tokens (user_id, purpose) WHERE used_at IS NULL;
