-- filepath: /backend/src/main/resources/db/migration/V7__plans_receipts.sql
-- Paket VIP/Free, pengaturan fitur, dan pencatatan dari foto struk.

-- 1. Paket user. VIP diatur manual lewat SQL (lihat README "Paket VIP & akses fitur").
ALTER TABLE users ADD COLUMN plan VARCHAR(10) NOT NULL DEFAULT 'FREE';
ALTER TABLE users ADD COLUMN plan_expires_at TIMESTAMPTZ;
ALTER TABLE users ADD CONSTRAINT chk_users_plan CHECK (plan IN ('FREE', 'VIP'));

-- 2. Pengaturan aplikasi (bisa diubah tanpa deploy; dibaca ulang maks. 60 detik)
CREATE TABLE app_settings (
    key         VARCHAR(80)  PRIMARY KEY,
    value       VARCHAR(255) NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
INSERT INTO app_settings (key, value) VALUES
    ('receipt_scan.access', 'VIP_ONLY'),          -- ALL | VIP_ONLY
    ('receipt_scan.free_monthly_quota', '3');     -- kuota user FREE per bulan saat access = ALL

-- 3. Riwayat pembacaan struk (foto TIDAK disimpan; hanya hasil & status)
CREATE TABLE receipt_scans (
    id               UUID PRIMARY KEY,
    user_id          UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    chat_id          BIGINT        NOT NULL,
    message_id       BIGINT        NOT NULL,
    bot_message_id   BIGINT,
    file_id          VARCHAR(255)  NOT NULL,
    file_unique_id   VARCHAR(128)  NOT NULL,
    caption          VARCHAR(255),
    status           VARCHAR(16)   NOT NULL,
    amount           NUMERIC(15,2),
    merchant         VARCHAR(120),
    receipt_date     DATE,
    confidence       VARCHAR(10),
    transaction_id   UUID,
    error_code       VARCHAR(40),
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    processed_at     TIMESTAMPTZ,
    CONSTRAINT chk_receipt_scans_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'DONE', 'NEEDS_AMOUNT', 'FAILED')),
    CONSTRAINT ux_receipt_scans_file UNIQUE (user_id, file_unique_id)
);
CREATE INDEX ix_receipt_scans_user_created ON receipt_scans (user_id, created_at);
CREATE INDEX ix_receipt_scans_status ON receipt_scans (status, created_at);

-- 4. Penanda transaksi yang berasal dari struk
ALTER TABLE transactions ADD COLUMN receipt_scan_id UUID REFERENCES receipt_scans (id) ON DELETE SET NULL;
