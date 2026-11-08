#!/usr/bin/env bash
set -euo pipefail
# Deploy a root-owned copy of this file to /usr/local/sbin/deltabilyard-turn-cert.
source_dir=/var/lib/caddy/.local/share/caddy/certificates/acme-v02.api.letsencrypt.org-directory/bilyard.deltavpn.me
target_dir=/home/bilyard/DeltaBilyard/infra/tls
install -d -m 755 "$target_dir"
changed=false
for pair in 'crt turn.crt' 'key turn.key'; do
  read -r suffix name <<< "$pair"
  source_file="$source_dir/bilyard.deltavpn.me.$suffix"
  if ! cmp -s "$source_file" "$target_dir/$name"; then
    install -m 600 "$source_file" "$target_dir/$name"
    changed=true
  fi
done
if $changed && docker inspect deltabilyard-livekit-1 >/dev/null 2>&1; then
  docker restart deltabilyard-livekit-1 >/dev/null
fi
