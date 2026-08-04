variable "region" {
  description = "AWS region where the table will be created"
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Environment name"
  type        = string
  default     = "dev"
}

variable "table_name" {
  description = "Name of the DynamoDB table"
  type        = string
  default     = "todo-list"
}
