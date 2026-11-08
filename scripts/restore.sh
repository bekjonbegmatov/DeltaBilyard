#!/usr/bin/env bash
set -euo pipefail
if ! docker info >/dev/null 2>&1; then exec sudo -E bash "$0" "$@"; fi
cd "$(dirname "$0")/.."
: "${FILE:?Use make restore FILE=backups/name.dump}"
[ -f "$FILE" ]
docker compose stop web backend
cat "$FILE" | docker compose exec -T postgres sh -c 'pg_restore -U "$POSTGRES_USER" -d bilyard --clean --if-exists --exit-on-error'
docker compose up -d --wait backend web
