#!/usr/bin/env bash
# filepath: /deploy/scripts/deploy.sh
# Deploy / update production di server:  bash deploy/scripts/deploy.sh
# Langkah: cek config → backup DB → git pull → build image → jalankan ulang container.
set -euo pipefail
cd "$(dirname "$0")/../.."

COMPOSE=(docker compose -f docker-compose.prod.yml)

[ -f .env ] || { echo "❌ File .env belum ada. Jalankan: cp .env.example .env lalu isi."; exit 1; }
[ -f backend/.env.production ] || {
  echo "❌ backend/.env.production belum ada. Jalankan: cp backend/.env.production.example backend/.env.production lalu isi."
  exit 1
}
chmod 600 .env backend/.env.production

# Backup sebelum update (hanya jika database sudah pernah jalan)
if [ -n "$("${COMPOSE[@]}" ps -q --status running postgres 2>/dev/null)" ]; then
  bash deploy/scripts/backup-db.sh
fi

if [ "${SKIP_PULL:-0}" != "1" ]; then
  git pull --ff-only
fi

"${COMPOSE[@]}" build --pull
"${COMPOSE[@]}" up -d --remove-orphans
docker image prune -f >/dev/null

echo "⏳ Menunggu backend sehat (migrasi Flyway berjalan saat start)..."
for _ in $(seq 1 40); do
  status=$(docker inspect -f '{{.State.Health.Status}}' "$("${COMPOSE[@]}" ps -q backend)" 2>/dev/null || echo starting)
  if [ "$status" = "healthy" ]; then
    "${COMPOSE[@]}" ps
    echo "✅ Deploy selesai."
    exit 0
  fi
  sleep 5
done

echo "❌ Backend belum sehat setelah 200 detik. Cek log: docker compose -f docker-compose.prod.yml logs --tail=200 backend"
exit 1
