-- filepath: /backend/src/main/resources/db/migration/V2__email_verification.sql

ALTER TABLE users ADD COLUMN email_verified_at TIMESTAMPTZ;

CREATE TABLE email_verification_tokens (
    id          UUID PRIMARY KEY,
    user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  VARCHAR(64) NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_email_verification_tokens_hash ON email_verification_tokens (token_hash);
CREATE INDEX ix_email_verification_tokens_user ON email_verification_tokens (user_id);
