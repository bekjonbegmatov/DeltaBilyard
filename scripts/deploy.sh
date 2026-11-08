#!/usr/bin/env bash
set -euo pipefail
if ! docker info >/dev/null 2>&1; then exec sudo -E bash "$0" "$@"; fi
cd "$(dirname "$0")/.."
python3 scripts/configure.py
docker compose config --quiet
if docker compose ps --status running --services | grep -qx backend; then
  active=$(docker compose exec -T backend bash -c 'exec 3<>/dev/tcp/localhost/8080; printf "POST /api/admin/drain HTTP/1.0\r\nX-Admin-Key: %s\r\nContent-Length: 0\r\n\r\n" "$ADMIN_KEY" >&3; cat <&3' | tail -1)
  if ! printf '%s' "$active" | python3 -c 'import json,sys;sys.exit(0 if json.load(sys.stdin)["active"]==0 else 1)'; then
    echo 'Active matches remain. New rooms are paused; retry make build after they finish.'; exit 1
  fi
  bash scripts/backup.sh
fi
docker compose build
docker compose up -d --wait --wait-timeout 180
curl --fail --silent http://127.0.0.1:7584/health
printf '\nDeltaBilyard is ready.\n'
