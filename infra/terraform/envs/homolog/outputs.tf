output "vm_public_ip" {
  description = "Point homolog.<domain> and mail.homolog.<domain> here."
  value       = module.vm.public_ip
}

output "vm_instance_id" {
  value = module.vm.instance_id
}

output "shell" {
  description = "Open a shell without SSH (needs the Session Manager plugin for the AWS CLI)."
  value       = "aws ssm start-session --target ${module.vm.instance_id} --region ${var.aws_region}"
}

output "github_deploy_role_arn" {
  description = "GitHub environment variable AWS_HOMOLOG_ROLE_ARN."
  value       = aws_iam_role.github_deploy.arn
}

output "backup_bucket" {
  value = aws_s3_bucket.backups.bucket
}

output "sqs_queue_urls" {
  value = module.sqs.queue_urls
}

output "sqs_queue_names" {
  value = module.sqs.queue_names
}
