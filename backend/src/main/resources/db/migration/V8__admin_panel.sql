-- filepath: /backend/src/main/resources/db/migration/V8__admin_panel.sql
-- Panel admin: akun admin terpisah dari user, sesi admin, undangan email, audit log, dan blokir user.

CREATE TABLE admin_users (
    id                   UUID PRIMARY KEY,
    email                VARCHAR(254) NOT NULL,
    full_name            VARCHAR(100) NOT NULL,
    password_hash        VARCHAR(100) NOT NULL,
    role                 VARCHAR(10)  NOT NULL,
    status               VARCHAR(10)  NOT NULL DEFAULT 'ACTIVE',
    must_change_password BOOLEAN      NOT NULL DEFAULT FALSE,
    failed_login_count   INT          NOT NULL DEFAULT 0,
    locked_until         TIMESTAMPTZ,
    last_login_at        TIMESTAMPTZ,
    created_by           UUID REFERENCES admin_users (id) ON DELETE SET NULL,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_admin_users_role CHECK (role IN ('OWNER', 'ADMIN')),
    CONSTRAINT chk_admin_users_status CHECK (status IN ('ACTIVE', 'DISABLED'))
);
CREATE UNIQUE INDEX uq_admin_users_email ON admin_users (lower(email));
-- Hanya boleh ada satu OWNER
CREATE UNIQUE INDEX uq_admin_users_single_owner ON admin_users (role) WHERE role = 'OWNER';

CREATE TABLE admin_refresh_tokens (
    id          UUID PRIMARY KEY,
    admin_id    UUID         NOT NULL REFERENCES admin_users (id) ON DELETE CASCADE,
    token_hash  VARCHAR(64)  NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ  NOT NULL,
    revoked_at  TIMESTAMPTZ,
    replaced_by UUID,
    user_agent  VARCHAR(255),
    ip          VARCHAR(45),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_admin_refresh_tokens_admin ON admin_refresh_tokens (admin_id);

CREATE TABLE admin_invitations (
    id          UUID PRIMARY KEY,
    email       VARCHAR(254) NOT NULL,
    full_name   VARCHAR(100) NOT NULL,
    token_hash  VARCHAR(64)  NOT NULL UNIQUE,
    invited_by  UUID REFERENCES admin_users (id) ON DELETE SET NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,
    accepted_at TIMESTAMPTZ,
    revoked_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
-- Satu undangan aktif per email
CREATE UNIQUE INDEX uq_admin_invitations_pending ON admin_invitations (lower(email))
    WHERE accepted_at IS NULL AND revoked_at IS NULL;

CREATE TABLE admin_audit_logs (
    id          BIGSERIAL PRIMARY KEY,
    admin_id    UUID REFERENCES admin_users (id) ON DELETE SET NULL,
    action      VARCHAR(40)  NOT NULL,
    target_type VARCHAR(20),
    target_id   VARCHAR(64),
    detail      JSONB,
    ip          VARCHAR(45),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_admin_audit_logs_created ON admin_audit_logs (created_at DESC);
CREATE INDEX idx_admin_audit_logs_target ON admin_audit_logs (target_type, target_id);

-- Blokir user oleh admin
ALTER TABLE users ADD COLUMN blocked_at TIMESTAMPTZ;
ALTER TABLE users ADD COLUMN blocked_reason VARCHAR(255);
ALTER TABLE users ADD COLUMN blocked_by UUID REFERENCES admin_users (id) ON DELETE SET NULL;
CREATE INDEX idx_users_blocked ON users (blocked_at) WHERE blocked_at IS NOT NULL;
