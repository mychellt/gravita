# Remote state in S3 with native locking. The values are supplied at init time so no
# account details live in git:
#   terraform init -backend-config=backend.hcl
# See backend.hcl.example.
terraform {
  backend "s3" {}
}
