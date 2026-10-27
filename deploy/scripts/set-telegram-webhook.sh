#!/usr/bin/env bash
# filepath: /deploy/scripts/set-telegram-webhook.sh
# Daftarkan webhook bot Telegram ke backend production (cukup sekali, atau setiap domain/secret berubah).
#   bash deploy/scripts/set-telegram-webhook.sh
# Nilai dibaca dari backend/.env.production: TELEGRAM_BOT_TOKEN, TELEGRAM_WEBHOOK_SECRET, APP_BACKEND_URL.
set -euo pipefail
cd "$(dirname "$0")/../.."

ENV_FILE=backend/.env.production
get() { grep -E "^$1=" "$ENV_FILE" | tail -n1 | cut -d= -f2- | tr -d '\r'; }

TOKEN=$(get TELEGRAM_BOT_TOKEN)
SECRET=$(get TELEGRAM_WEBHOOK_SECRET)
BASE_URL=$(get APP_BACKEND_URL)
[ -n "$TOKEN" ] && [ -n "$SECRET" ] && [ -n "$BASE_URL" ] || {
  echo "❌ TELEGRAM_BOT_TOKEN, TELEGRAM_WEBHOOK_SECRET, dan APP_BACKEND_URL wajib diisi di $ENV_FILE"; exit 1;
}

WEBHOOK_URL="${BASE_URL%/}/api/v1/telegram/webhook"
echo "🔗 Mendaftarkan webhook: $WEBHOOK_URL"
# Token bot tidak dicetak ke layar
curl -fsS "https://api.telegram.org/bot${TOKEN}/setWebhook" \
  --data-urlencode "url=${WEBHOOK_URL}" \
  --data-urlencode "secret_token=${SECRET}" \
  --data-urlencode 'allowed_updates=["message","callback_query"]'
echo
curl -fsS "https://api.telegram.org/bot${TOKEN}/getWebhookInfo"
echo
