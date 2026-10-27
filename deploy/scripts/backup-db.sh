#!/usr/bin/env bash
# filepath: /deploy/scripts/backup-db.sh
# Backup database PostgreSQL (native di host VPS) ke backups/sakutahu-YYYYmmdd-HHMMSS.sql.gz (disimpan 14 hari).
#   bash deploy/scripts/backup-db.sh
# Kredensial dibaca dari file .env (POSTGRES_DB, POSTGRES_USER, POSTGRES_PASSWORD).
# Jadwalkan harian lewat cron, contoh (jam 02:00):
#   0 2 * * * cd /opt/sakutahu && bash deploy/scripts/backup-db.sh >> backups/backup.log 2>&1
set -euo pipefail
cd "$(dirname "$0")/../.."

[ -f .env ] || { echo "❌ File .env belum ada."; exit 1; }
get() { grep -E "^$1=" .env | tail -n1 | cut -d= -f2- | tr -d '\r'; }

DB_NAME=$(get POSTGRES_DB); DB_NAME=${DB_NAME:-sakutahu}
DB_USER=$(get POSTGRES_USER); DB_USER=${DB_USER:-sakutahu_app}
PGPASSWORD=$(get POSTGRES_PASSWORD)
[ -n "$PGPASSWORD" ] || { echo "❌ POSTGRES_PASSWORD kosong di .env"; exit 1; }
export PGPASSWORD

BACKUP_DIR="${BACKUP_DIR:-backups}"
KEEP_DAYS="${KEEP_DAYS:-14}"
mkdir -p "$BACKUP_DIR"
chmod 700 "$BACKUP_DIR"
umask 077

file="$BACKUP_DIR/sakutahu-$(date +%Y%m%d-%H%M%S).sql.gz"
# Tulis ke file sementara dulu, supaya backup yang gagal di tengah jalan tidak tertinggal sebagai file "valid"
pg_dump -h 127.0.0.1 -U "$DB_USER" -d "$DB_NAME" --no-owner --no-privileges | gzip > "$file.tmp"
mv "$file.tmp" "$file"

find "$BACKUP_DIR" -name 'sakutahu-*.sql.gz' -mtime +"$KEEP_DAYS" -delete
echo "💾 Backup: $file ($(du -h "$file" | cut -f1))"
