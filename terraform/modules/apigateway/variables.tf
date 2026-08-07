variable "region" {
  description = "AWS region"
  type        = string
  default     = "us-east-1"
}

variable "api_name" {
  description = "Name of the API Gateway"
  type        = string
  default     = "todo-list-api"
}

variable "stage_name" {
  description = "Deployment stage"
  type        = string
  default     = "dev"
}

variable "create_lambda_invoke_arn" {
  description = "Invoke ARN of the create lambda"
  type        = string
}

variable "create_lambda_function_name" {
  description = "Function name of the create lambda"
  type        = string
}

variable "list_lambda_invoke_arn" {
  description = "Invoke ARN of the list lambda"
  type        = string
}

variable "list_lambda_function_name" {
  description = "Function name of the list lambda"
  type        = string
}

variable "add_task_lambda_invoke_arn" {
  description = "Invoke ARN of the add task lambda"
  type        = string
}

variable "add_task_lambda_function_name" {
  description = "Function name of the add task lambda"
  type        = string
}

variable "list_task_items_lambda_invoke_arn" {
  description = "Invoke ARN of the list task items lambda"
  type        = string
}

variable "list_task_items_lambda_function_name" {
  description = "Function name of the list task items lambda"
  type        = string
}

variable "update_lambda_invoke_arn" {
  description = "Invoke ARN of the update lambda"
  type        = string
}

variable "update_lambda_function_name" {
  description = "Function name of the update lambda"
  type        = string
}

variable "update_task_item_lambda_invoke_arn" {
  description = "Invoke ARN of the update task item lambda"
  type        = string
}

variable "update_task_item_lambda_function_name" {
  description = "Function name of the update task item lambda"
  type        = string
}
