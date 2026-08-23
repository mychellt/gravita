#!/usr/bin/env bash
set -euo pipefail

# Deploy Angular build output to S3 static website hosting.
# Required env vars:
#   AWS_REGION
#   S3_BUCKET

: "${AWS_REGION:?AWS_REGION is required}"
: "${S3_BUCKET:?S3_BUCKET is required}"

BUILD_DIR="dist/gravita-web/browser"
if [[ ! -d "${BUILD_DIR}" ]]; then
  BUILD_DIR="dist/gravita-web"
fi

if [[ ! -d "${BUILD_DIR}" ]]; then
  echo "Build output not found. Run: npm run build -- --configuration production"
  exit 1
fi

# Upload immutable assets with long cache (exclude all HTML — uploaded separately).
aws s3 sync "${BUILD_DIR}/" "s3://${S3_BUCKET}/" \
  --region "${AWS_REGION}" \
  --delete \
  --exclude "*.html" \
  --cache-control "public,max-age=31536000,immutable"

# Upload HTML files with no-cache for safe rollouts.
while IFS= read -r -d '' html; do
  key="${html#${BUILD_DIR}/}"
  aws s3 cp "${html}" "s3://${S3_BUCKET}/${key}" \
    --region "${AWS_REGION}" \
    --cache-control "no-cache,no-store,must-revalidate" \
    --content-type "text/html"
  echo "  Uploaded: ${key}"
done < <(find "${BUILD_DIR}" -name "*.html" -print0)

echo "Deployment finished: s3://${S3_BUCKET}"
