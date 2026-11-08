#!/usr/bin/env bash
set -euo pipefail
if ! docker info >/dev/null 2>&1; then exec sudo -E bash "$0" "$@"; fi
cd "$(dirname "$0")/.."
mkdir -p backups
chmod 700 backups
file="backups/bilyard-$(date -u +%Y%m%dT%H%M%SZ).dump"
docker compose exec -T postgres sh -c 'pg_dump -U "$POSTGRES_USER" -d bilyard -Fc' > "$file"
chmod 600 "$file"
printf 'Backup saved: %s\n' "$file"
