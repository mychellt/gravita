output "queue_urls" {
  description = "Queue URL by base name."
  value       = { for k, q in aws_sqs_queue.this : k => q.url }
}

output "queue_names" {
  description = "Actual (prefixed) queue name by base name."
  value       = { for k, q in aws_sqs_queue.this : k => q.name }
}

output "app_policy_json" {
  description = "IAM policy granting the app access to these queues."
  value       = data.aws_iam_policy_document.app.json
}
