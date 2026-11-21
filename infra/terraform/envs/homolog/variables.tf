variable "aws_region" {
  description = "us-east-1 is ~50% cheaper than sa-east-1; fine for homologation with test data."
  type        = string
  default     = "us-east-1"
}

variable "aws_profile" {
  type    = string
  default = "gravita-homolog"
}

variable "instance_type" {
  type    = string
  default = "t4g.small"
}

variable "availability_zone_index" {
  type    = number
  default = 0
}

variable "ssh_public_keys" {
  type    = list(string)
  default = []
}

variable "ssh_allowed_cidrs" {
  description = "Empty = SSH closed; use `aws ssm start-session` instead."
  type        = list(string)
  default     = []
}

variable "github_repository" {
  description = "owner/name of the repository whose deploy-homolog workflow may deploy."
  type        = string
  default     = "mychellt/gravita"
}

variable "create_github_oidc_provider" {
  description = "Set to false if the AWS account already has the GitHub OIDC provider."
  type        = bool
  default     = true
}

variable "budget_alert_emails" {
  description = "Who gets an e-mail at 50% actual and 100% forecasted of the US$30 monthly budget."
  type        = list(string)
  default     = []
}

variable "backup_retention_days" {
  type    = number
  default = 14
}
