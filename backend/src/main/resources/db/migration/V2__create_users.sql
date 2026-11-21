-- Usuarios y roles (módulo identity, SRS §8.5).
-- El correo se guarda normalizado en minúsculas: el índice único evita cuentas duplicadas.
CREATE TABLE users (
    id                    uuid         PRIMARY KEY,
    email                 varchar(254) NOT NULL,
    password_hash         varchar(100) NOT NULL,
    name                  varchar(60)  NOT NULL,
    status                varchar(30)  NOT NULL,
    region                varchar(60),
    commune               varchar(60),
    photo_url             varchar(500),
    failed_login_attempts integer      NOT NULL DEFAULT 0,
    locked_until          timestamptz,
    created_at            timestamptz  NOT NULL,
    updated_at            timestamptz  NOT NULL,
    version               bigint       NOT NULL DEFAULT 0,
    CONSTRAINT ck_users_status CHECK (status IN ('PENDING_VERIFICATION', 'ACTIVE', 'SUSPENDED')),
    CONSTRAINT ck_users_failed_attempts CHECK (failed_login_attempts >= 0),
    CONSTRAINT ck_users_email_lowercase CHECK (email = lower(email))
);

CREATE UNIQUE INDEX ux_users_email ON users (email);

CREATE TABLE user_roles (
    user_id uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role    varchar(20) NOT NULL,
    PRIMARY KEY (user_id, role),
    CONSTRAINT ck_user_roles_role CHECK (role IN ('USER', 'ADMIN'))
);

COMMENT ON TABLE users IS 'Cuentas de usuario. Estados del SRS: PENDIENTE_VERIFICACION, ACTIVA, SUSPENDIDA.';
COMMENT ON COLUMN users.password_hash IS 'Hash BCrypt (factor 12). Nunca la contraseña en claro (RNF-SEG-01).';
