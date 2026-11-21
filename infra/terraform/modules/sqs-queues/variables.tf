variable "name_prefix" {
  description = "Prefix for queue and IAM names (e.g. homolog-)."
  type        = string
}

variable "queue_names" {
  description = "Base queue names, the same ones the app uses locally."
  type        = list(string)
}

variable "max_receive_count" {
  description = "Deliveries before a message moves to the dead-letter queue."
  type        = number
  default     = 5
}

variable "message_retention_seconds" {
  type    = number
  default = 345600 # 4 days
}