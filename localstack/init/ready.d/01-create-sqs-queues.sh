#!/bin/bash
set -euo pipefail

QUEUES=(
  gravita-email-notifications
  gravita-quote-whatsapp
  user_registration_queue
  password_reset_queue
)

for queue in "${QUEUES[@]}"; do
  awslocal sqs create-queue --queue-name "$queue" >/dev/null
  echo "SQS queue ready: $queue"
done
