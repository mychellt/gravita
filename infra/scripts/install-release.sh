#!/usr/bin/env bash
# Installs a release bundle into /opt/gravita and deploys it. Run as root over SSM by
# .github/workflows/deploy-homolog.yml:
#   install-release.sh <bundle-dir> <app-image>
# The bundle holds: .env, Caddyfile, docker-compose.yaml, scripts/, web/.
set -euo pipefail

bundle="${1:?bundle dir required}"
image="${2:?app image required}"
target=/opt/gravita
user=deploy

install -d -o "$user" -g "$user" "$target" "$target/web" "$target/scripts"
install -o "$user" -g "$user" -m 600 "$bundle/.env" "$target/.env"
install -o "$user" -g "$user" -m 644 "$bundle/Caddyfile" "$bundle/docker-compose.yaml" "$target/"

# rsync into the existing directories: Caddy bind-mounts ./web, and a replaced
# directory would leave the container serving the old (deleted) one.
rsync -a --delete --chown="$user:$user" "$bundle/web/" "$target/web/"
rsync -a --delete --chown="$user:$user" "$bundle/scripts/" "$target/scripts/"
chmod +x "$target"/scripts/*.sh

sudo -u "$user" -H "$target/scripts/deploy.sh" "$image"
