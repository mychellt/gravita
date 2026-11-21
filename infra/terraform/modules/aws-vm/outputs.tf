output "public_ip" {
  description = "Elastic IP of the homologation VM (point the DNS records here)."
  value       = aws_eip.this.public_ip
}

output "instance_id" {
  value = aws_instance.this.id
}

output "role_name" {
  description = "IAM role of the VM; attach the environment's policies to it."
  value       = aws_iam_role.vm.name
}

output "deploy_user" {
  value = var.deploy_user
}
