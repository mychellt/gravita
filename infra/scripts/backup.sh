#!/usr/bin/env bash
# Nightly: dump the database and upload it to S3.
# Run by /etc/cron.d/gravita-backup (installed by cloud-init).
# Remote retention is an S3 lifecycle rule (infra/terraform/envs/homolog/main.tf).
# shellcheck source=SCRIPTDIR/lib.sh
source "$(dirname "$0")/lib.sh"

stamp="$(date -u +%Y%m%dT%H%M%SZ)"
file="gravita-${stamp}.dump"

echo "[$(date -u +%FT%TZ)] Dumping ${POSTGRES_DB} to ${file}"
compose exec -T postgres pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc > "/var/backups/gravita/${file}"

echo "Uploading to s3://${BACKUP_BUCKET}/db/${file}"
s3 cp "/backups/${file}" "s3://${BACKUP_BUCKET}/db/${file}"

# Keep 3 days locally for quick restores.
find /var/backups/gravita -name 'gravita-*.dump' -mtime +3 -delete

echo "Backup finished: ${file}"