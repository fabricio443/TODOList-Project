variable "region" {
  description = "AWS region"
  type        = string
  default     = "us-east-1"
}

variable "function_name" {
  description = "Name of the Lambda function"
  type        = string
  default     = "todo-create-task-list"
}

variable "lambda_zip_path" {
  description = "Path to the Lambda deployment package zip"
  type        = string
}

variable "table_name" {
  description = "DynamoDB table name"
  type        = string
}

variable "table_arn" {
  description = "DynamoDB table ARN"
  type        = string
}

variable "handler" {
  description = "Lambda handler"
  type        = string
  default     = "com.exemplo.lambda.CreateTaskListLambda::handleRequest"
}

variable "dynamodb_actions" {
  description = "DynamoDB permissions for the Lambda"
  type        = list(string)
  default     = ["dynamodb:PutItem"]
}

variable "s3_bucket_arn" {
  description = "S3 bucket ARN the Lambda should access (optional)"
  type        = string
  default     = ""
}

variable "s3_bucket_name" {
  description = "S3 bucket name to provide to the Lambda as environment variable (optional)"
  type        = string
  default     = ""
}

variable "s3_actions" {
  description = "S3 actions for the Lambda role (optional)"
  type        = list(string)
  default     = []
}

variable "enable_s3" {
  description = "Whether to attach S3 policy to the Lambda (use when bucket access is required)"
  type        = bool
  default     = false
}

variable "sqs_queue_url" {
  description = "SQS queue URL to provide to the Lambda as environment variable"
  type        = string
  default     = ""
}

variable "sqs_queue_arn" {
  description = "SQS queue ARN for Lambda IAM policy"
  type        = string
  default     = ""
}

variable "sqs_actions" {
  description = "SQS actions for the Lambda role (optional)"
  type        = list(string)
  default     = []
}

variable "enable_sqs" {
  description = "Whether to attach SQS policy to the Lambda"
  type        = bool
  default     = false
}

variable "enable_ses" {
  description = "Whether to attach SES policy to the Lambda"
  type        = bool
  default     = false
}

variable "ses_actions" {
  description = "SES actions for the Lambda role (optional)"
  type        = list(string)
  default     = []
}

variable "ses_from_email" {
  description = "SES source email address (optional)"
  type        = string
  default     = ""
}
