#!/usr/bin/env bash
# Run on the VM after the stack files, .env and web/ have been copied to /opt/gravita.
#   deploy.sh [image]   -> optionally pin APP_IMAGE (e.g. ghcr.io/<org>/gravita:<sha>)
# shellcheck source=SCRIPTDIR/lib.sh
source "$(dirname "$0")/lib.sh"

if [[ -n "${1:-}" ]]; then
  sed -i "s|^APP_IMAGE=.*|APP_IMAGE=$1|" .env
  export APP_IMAGE="$1"
fi

echo "Deploying ${APP_IMAGE}"
compose pull
compose up -d --remove-orphans
# Pick up Caddyfile changes (up -d leaves an unchanged Caddy container running).
compose exec -T caddy caddy reload --config /etc/caddy/Caddyfile --adapter caddyfile
docker image prune -f >/dev/null

# Wait for the API to answer through Caddy (any HTTP status counts as up).
for _ in $(seq 1 30); do
  code="$(curl -s -o /dev/null -w '%{http_code}' "https://${HOMOLOG_DOMAIN}/api/" || true)"
  if [[ "$code" != "000" && "$code" != "502" && "$code" != "503" ]]; then
    echo "API is up (HTTP ${code})"
    exit 0
  fi
  sleep 5
done

echo "API did not come up in time; check: docker compose logs app" >&2
exit 1