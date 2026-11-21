variable "name" {
  description = "Prefix for every resource name (e.g. gravita-homolog)."
  type        = string
}

variable "instance_type" {
  description = "Graviton (ARM) instance. t4g.small (2 GB) fits the credits; t4g.medium (4 GB) is more comfortable."
  type        = string
  default     = "t4g.small"
}

variable "availability_zone_index" {
  description = "Which AZ of the region to use (change it if the instance type is not offered there)."
  type        = number
  default     = 0
}

variable "root_volume_gbs" {
  type    = number
  default = 30
}

variable "swap_gbs" {
  description = "Swap file size; a safety net for the 2 GB instance."
  type        = number
  default     = 2
}

variable "vpc_cidr" {
  type    = string
  default = "10.10.0.0/16"
}

variable "subnet_cidr" {
  type    = string
  default = "10.10.1.0/24"
}

variable "ssh_public_keys" {
  description = "Public SSH keys for the deploy user. Optional when using SSM Session Manager only."
  type        = list(string)
  default     = []
}

variable "ssh_allowed_cidrs" {
  description = "CIDRs allowed to reach port 22. Empty = SSH closed (use SSM Session Manager)."
  type        = list(string)
  default     = []
}

variable "deploy_user" {
  description = "Linux user that owns /opt/gravita and runs Docker."
  type        = string
  default     = "deploy"
}