#!/usr/bin/env bash
# Restore the database from a dump.
#   restore.sh                      -> latest dump in object storage
#   restore.sh gravita-<stamp>.dump -> that dump (local copy if present, else downloaded)
# shellcheck source=SCRIPTDIR/lib.sh
source "$(dirname "$0")/lib.sh"

name="${1:-}"
if [[ -z "$name" ]]; then
  name="$(s3 ls "s3://${BACKUP_BUCKET}/db/" | awk '{print $4}' | sort | tail -n 1)"
  [[ -n "$name" ]] || { echo "No dumps found in s3://${BACKUP_BUCKET}/db/" >&2; exit 1; }
fi

if [[ ! -f "/var/backups/gravita/${name}" ]]; then
  echo "Downloading ${name}"
  s3 cp "s3://${BACKUP_BUCKET}/db/${name}" "/backups/${name}"
fi

confirm "This replaces the '${POSTGRES_DB}' database with ${name}."

compose stop app
compose exec -T postgres pg_restore -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
  --clean --if-exists --no-owner < "/var/backups/gravita/${name}"
compose start app

echo "Restored ${name}"