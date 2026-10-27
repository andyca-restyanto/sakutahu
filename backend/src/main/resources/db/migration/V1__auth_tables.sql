-- filepath: /backend/src/main/resources/db/migration/V1__auth_tables.sql

CREATE TABLE users (
    id                  UUID PRIMARY KEY,
    email               VARCHAR(254) NOT NULL,
    password_hash       VARCHAR(100) NOT NULL,
    full_name           VARCHAR(100) NOT NULL,
    currency            VARCHAR(3)   NOT NULL DEFAULT 'IDR',
    timezone            VARCHAR(64)  NOT NULL DEFAULT 'Asia/Jakarta',
    failed_login_count  INTEGER      NOT NULL DEFAULT 0,
    locked_until        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_users_currency CHECK (currency ~ '^[A-Z]{3}$')
);
CREATE UNIQUE INDEX ux_users_email_lower ON users (lower(email));

CREATE TABLE telegram_accounts (
    id                UUID PRIMARY KEY,
    user_id           UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    telegram_user_id  BIGINT      NOT NULL,
    chat_id           BIGINT      NOT NULL,
    username          VARCHAR(64),
    linked_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ux_telegram_accounts_user UNIQUE (user_id),
    CONSTRAINT ux_telegram_accounts_tg_user UNIQUE (telegram_user_id)
);

CREATE TABLE refresh_tokens (
    id           UUID PRIMARY KEY,
    user_id      UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash   VARCHAR(64) NOT NULL,
    expires_at   TIMESTAMPTZ NOT NULL,
    revoked_at   TIMESTAMPTZ,
    replaced_by  UUID,
    user_agent   VARCHAR(255),
    ip           VARCHAR(45),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_refresh_tokens_hash ON refresh_tokens (token_hash);
CREATE INDEX ix_refresh_tokens_user ON refresh_tokens (user_id);

CREATE TABLE telegram_link_tokens (
    id          UUID PRIMARY KEY,
    user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  VARCHAR(64) NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_telegram_link_tokens_hash ON telegram_link_tokens (token_hash);
CREATE INDEX ix_telegram_link_tokens_user ON telegram_link_tokens (user_id);
