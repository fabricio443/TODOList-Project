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
