#!/usr/bin/env bash
# Wipe the homologation database and start the app on an empty schema.
# Use before a release-candidate test round. Takes a backup first.
# shellcheck source=SCRIPTDIR/lib.sh
source "$(dirname "$0")/lib.sh"

confirm "This DROPS the '${POSTGRES_DB}' database (a backup is taken first)."

"$(dirname "$0")/backup.sh"

compose stop app
compose exec -T postgres psql -U "$POSTGRES_USER" -d postgres -v ON_ERROR_STOP=1 \
  -c "DROP DATABASE IF EXISTS \"${POSTGRES_DB}\" WITH (FORCE);" \
  -c "CREATE DATABASE \"${POSTGRES_DB}\";"
compose start app

echo "Database reset. The app recreates the schema on start-up."