#!/usr/bin/env bash
# Shared helpers for the scripts that run on the homologation VM (from /opt/gravita/scripts).
set -euo pipefail

GRAVITA_DIR="${GRAVITA_DIR:-/opt/gravita}"
cd "$GRAVITA_DIR"

if [[ ! -f .env ]]; then
  echo "Missing $GRAVITA_DIR/.env" >&2
  exit 1
fi

set -a
# shellcheck disable=SC1091
source .env
set +a

compose() {
  docker compose -f "$GRAVITA_DIR/docker-compose.yaml" "$@"
}

# Runs the AWS CLI in a container; credentials come from the EC2 instance role.
s3() {
  docker run --rm \
    -e AWS_DEFAULT_REGION="$AWS_REGION" \
    -v /var/backups/gravita:/backups \
    amazon/aws-cli s3 "$@"
}

confirm() {
  read -r -p "$1 Type 'yes' to continue: " answer
  [[ "$answer" == "yes" ]] || { echo "Aborted."; exit 1; }
}