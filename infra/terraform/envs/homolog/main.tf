locals {
  name = "gravita-homolog"

  # Same queues the app uses locally (localstack/init/ready.d/01-create-sqs-queues.sh).
  queue_names = [
    "gravita-email-notifications",
    "gravita-quote-whatsapp",
    "user_registration_queue",
    "password_reset_queue",
  ]
}

data "aws_caller_identity" "current" {}

module "vm" {
  source = "../../modules/aws-vm"

  name                    = local.name
  instance_type           = var.instance_type
  availability_zone_index = var.availability_zone_index
  ssh_public_keys         = var.ssh_public_keys
  ssh_allowed_cidrs       = var.ssh_allowed_cidrs
}

module "sqs" {
  source = "../../modules/sqs-queues"

  name_prefix = "homolog-"
  queue_names = local.queue_names
}

# The app reaches SQS through the VM's role: no access keys in the env file.
resource "aws_iam_role_policy" "vm_sqs" {
  name   = "sqs-queues"
  role   = module.vm.role_name
  policy = module.sqs.app_policy_json
}

# --- Backups -------------------------------------------------------------------

resource "aws_s3_bucket" "backups" {
  bucket = "${local.name}-backups-${data.aws_caller_identity.current.account_id}"
}

resource "aws_s3_bucket_public_access_block" "backups" {
  bucket                  = aws_s3_bucket.backups.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "backups" {
  bucket = aws_s3_bucket.backups.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_lifecycle_configuration" "backups" {
  bucket = aws_s3_bucket.backups.id

  rule {
    id     = "expire-db-dumps"
    status = "Enabled"
    filter {
      prefix = "db/"
    }
    expiration {
      days = var.backup_retention_days
    }
  }

  # Release bundles from the deploy workflow (deleted after install; this is the fallback).
  rule {
    id     = "expire-deploy-bundles"
    status = "Enabled"
    filter {
      prefix = "deploy/"
    }
    expiration {
      days = 1
    }
  }

  # Homologation fiscal XMLs have no legal validity.
  rule {
    id     = "expire-homolog-xmls"
    status = "Enabled"
    filter {
      prefix = "homolog/"
    }
    expiration {
      days = 30
    }
  }
}

resource "aws_iam_role_policy" "vm_backups" {
  name = "backups-bucket"
  role = module.vm.role_name

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = ["s3:ListBucket"]
        Resource = aws_s3_bucket.backups.arn
      },
      {
        Effect   = "Allow"
        Action   = ["s3:GetObject", "s3:PutObject", "s3:DeleteObject"]
        Resource = "${aws_s3_bucket.backups.arn}/*"
      },
    ]
  })
}

# --- Cost guardrail ------------------------------------------------------------

resource "aws_budgets_budget" "homolog" {
  name         = "${local.name}-monthly"
  budget_type  = "COST"
  limit_amount = "30"
  limit_unit   = "USD"
  time_unit    = "MONTHLY"

  # Counts only resources tagged Environment=homolog. Activate the "Environment"
  # cost allocation tag once in Billing → Cost allocation tags.
  cost_filter {
    name   = "TagKeyValue"
    values = ["user:Environment$homolog"]
  }

  dynamic "notification" {
    for_each = length(var.budget_alert_emails) > 0 ? [50, 100] : []
    content {
      comparison_operator        = "GREATER_THAN"
      threshold                  = notification.value
      threshold_type             = "PERCENTAGE"
      notification_type          = notification.value == 100 ? "FORECASTED" : "ACTUAL"
      subscriber_email_addresses = var.budget_alert_emails
    }
  }
}
