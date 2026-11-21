terraform {
  required_providers {
    aws = {
      source = "hashicorp/aws"
    }
  }
}

resource "aws_sqs_queue" "dlq" {
  for_each = toset(var.queue_names)

  name                      = "${var.name_prefix}${each.key}-dlq"
  message_retention_seconds = 1209600 # 14 days, the maximum
  sqs_managed_sse_enabled   = true
}

resource "aws_sqs_queue" "this" {
  for_each = toset(var.queue_names)

  name                      = "${var.name_prefix}${each.key}"
  message_retention_seconds = var.message_retention_seconds
  sqs_managed_sse_enabled   = true

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.dlq[each.key].arn
    maxReceiveCount     = var.max_receive_count
  })
}

# Least-privilege access to the queues, for whoever runs the app (attach it to a role).
data "aws_iam_policy_document" "app" {
  statement {
    actions = [
      "sqs:SendMessage",
      "sqs:ReceiveMessage",
      "sqs:DeleteMessage",
      "sqs:ChangeMessageVisibility",
      "sqs:GetQueueUrl",
      "sqs:GetQueueAttributes",
    ]
    resources = [for q in aws_sqs_queue.this : q.arn]
  }
}
