#!/usr/bin/env bash
# filepath: /deploy/scripts/backup-db.sh
# Backup database ke backups/sakutahu-YYYYmmdd-HHMMSS.sql.gz (disimpan 14 hari).
#   bash deploy/scripts/backup-db.sh
# Jadwalkan harian lewat cron, contoh (jam 02:00):
#   0 2 * * * cd /opt/sakutahu && bash deploy/scripts/backup-db.sh >> backups/backup.log 2>&1
set -euo pipefail
cd "$(dirname "$0")/../.."

BACKUP_DIR="${BACKUP_DIR:-backups}"
KEEP_DAYS="${KEEP_DAYS:-14}"
mkdir -p "$BACKUP_DIR"
chmod 700 "$BACKUP_DIR"

file="$BACKUP_DIR/sakutahu-$(date +%Y%m%d-%H%M%S).sql.gz"
docker compose -f docker-compose.prod.yml exec -T postgres \
  sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --no-owner --no-privileges' | gzip > "$file"
chmod 600 "$file"

find "$BACKUP_DIR" -name 'sakutahu-*.sql.gz' -mtime +"$KEEP_DAYS" -delete
echo "💾 Backup: $file ($(du -h "$file" | cut -f1))"
