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
