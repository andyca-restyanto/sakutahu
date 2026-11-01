-- filepath: /backend/src/main/resources/db/migration/V10__admin_password_reset.sql
-- Lupa password admin/OWNER. Terpisah dari password_reset_tokens milik user.
CREATE TABLE admin_password_reset_tokens (
    id           UUID PRIMARY KEY,
    admin_id     UUID         NOT NULL REFERENCES admin_users (id) ON DELETE CASCADE,
    token_hash   VARCHAR(64)  NOT NULL UNIQUE,
    expires_at   TIMESTAMPTZ  NOT NULL,
    used_at      TIMESTAMPTZ,
    ip           VARCHAR(45),
    -- NULL = diminta sendiri dari halaman login; berisi id OWNER jika dikirim dari menu Admin
    requested_by UUID REFERENCES admin_users (id) ON DELETE SET NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_admin_password_reset_tokens_admin ON admin_password_reset_tokens (admin_id);
