# Credentials come from a named profile in ~/.aws/credentials (or `aws sso login`).
provider "aws" {
  region  = var.aws_region
  profile = var.aws_profile

  default_tags {
    tags = {
      Project     = "gravita"
      Environment = "homolog"
      ManagedBy   = "terraform"
    }
  }
}
