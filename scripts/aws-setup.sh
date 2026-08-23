#!/usr/bin/env bash
set -euo pipefail

# One-time setup script for low-cost Angular SPA hosting on AWS (S3 website hosting only).
# Usage:
#   REGION=us-east-1 APP_NAME=gravita-web ./scripts/aws-setup.sh

: "${REGION:=us-east-1}"
: "${APP_NAME:=gravita-web}"

if ! command -v aws >/dev/null 2>&1; then
  echo "AWS CLI not found. Install it first: https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html"
  exit 1
fi

ACCOUNT_ID="$(aws sts get-caller-identity --query Account --output text)"
BUCKET_NAME="${APP_NAME}-${ACCOUNT_ID}-${REGION}"

echo "Using bucket: ${BUCKET_NAME}"

if [[ "${REGION}" == "us-east-1" ]]; then
  aws s3api create-bucket --bucket "${BUCKET_NAME}" >/dev/null 2>&1 || true
else
  aws s3api create-bucket \
    --bucket "${BUCKET_NAME}" \
    --create-bucket-configuration LocationConstraint="${REGION}" >/dev/null 2>&1 || true
fi

aws s3api delete-public-access-block --bucket "${BUCKET_NAME}" >/dev/null 2>&1 || true

aws s3api put-bucket-ownership-controls \
  --bucket "${BUCKET_NAME}" \
  --ownership-controls Rules=[{ObjectOwnership=BucketOwnerPreferred}] >/dev/null

aws s3api put-bucket-website \
  --bucket "${BUCKET_NAME}" \
  --website-configuration '{"IndexDocument":{"Suffix":"index.html"},"ErrorDocument":{"Key":"app.html"}}' >/dev/null

cat > /tmp/bucket-policy.json <<JSON
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "PublicReadGetObject",
      "Effect": "Allow",
      "Principal": "*",
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::${BUCKET_NAME}/*"
    }
  ]
}
JSON

aws s3api put-bucket-policy --bucket "${BUCKET_NAME}" --policy file:///tmp/bucket-policy.json >/dev/null

WEBSITE_URL="http://${BUCKET_NAME}.s3-website-${REGION}.amazonaws.com"

cat <<EOF
Setup complete.
Bucket: ${BUCKET_NAME}
Website URL: ${WEBSITE_URL}

Next:
1) Add these as GitHub repository variables:
   AWS_REGION=${REGION}
   S3_BUCKET=${BUCKET_NAME}
2) Configure GitHub OIDC role (see README deployment section).
3) Push to main branch to deploy automatically.
EOF
