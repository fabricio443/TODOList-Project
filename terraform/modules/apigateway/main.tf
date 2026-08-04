terraform {
  required_version = ">= 1.5.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.region
}

resource "aws_api_gateway_rest_api" "this" {
  name        = var.api_name
  description = "API Gateway para o projeto TODO List"
}

resource "aws_api_gateway_resource" "task_lists" {
  rest_api_id = aws_api_gateway_rest_api.this.id
  parent_id   = aws_api_gateway_rest_api.this.root_resource_id
  path_part   = "task-lists"
}

resource "aws_api_gateway_method" "post" {
  rest_api_id   = aws_api_gateway_rest_api.this.id
  resource_id   = aws_api_gateway_resource.task_lists.id
  http_method   = "POST"
  authorization = "NONE"
}

resource "aws_api_gateway_method" "get" {
  rest_api_id   = aws_api_gateway_rest_api.this.id
  resource_id   = aws_api_gateway_resource.task_lists.id
  http_method   = "GET"
  authorization = "NONE"
}

resource "aws_api_gateway_method" "put" {
  rest_api_id   = aws_api_gateway_rest_api.this.id
  resource_id   = aws_api_gateway_resource.task_lists.id
  http_method   = "PUT"
  authorization = "NONE"
}

resource "aws_api_gateway_integration" "post_integration" {
  rest_api_id             = aws_api_gateway_rest_api.this.id
  resource_id             = aws_api_gateway_resource.task_lists.id
  http_method             = aws_api_gateway_method.post.http_method
  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = var.create_lambda_invoke_arn
}

resource "aws_api_gateway_integration" "get_integration" {
  rest_api_id             = aws_api_gateway_rest_api.this.id
  resource_id             = aws_api_gateway_resource.task_lists.id
  http_method             = aws_api_gateway_method.get.http_method
  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = var.list_lambda_invoke_arn
}

resource "aws_api_gateway_integration" "put_integration" {
  rest_api_id             = aws_api_gateway_rest_api.this.id
  resource_id             = aws_api_gateway_resource.task_lists.id
  http_method             = aws_api_gateway_method.put.http_method
  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = var.update_lambda_invoke_arn
}

resource "aws_lambda_permission" "create_permission" {
  statement_id  = "AllowExecutionFromAPIGatewayCreate"
  action        = "lambda:InvokeFunction"
  function_name = var.create_lambda_function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.this.execution_arn}/*/*"
}

resource "aws_lambda_permission" "list_permission" {
  statement_id  = "AllowExecutionFromAPIGatewayList"
  action        = "lambda:InvokeFunction"
  function_name = var.list_lambda_function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.this.execution_arn}/*/*"
}

resource "aws_lambda_permission" "update_permission" {
  statement_id  = "AllowExecutionFromAPIGatewayUpdate"
  action        = "lambda:InvokeFunction"
  function_name = var.update_lambda_function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.this.execution_arn}/*/*"
}

resource "aws_api_gateway_deployment" "this" {
  depends_on = [
    aws_api_gateway_integration.post_integration,
    aws_api_gateway_integration.get_integration,
    aws_api_gateway_integration.put_integration,
    aws_lambda_permission.create_permission,
    aws_lambda_permission.list_permission,
    aws_lambda_permission.update_permission
  ]

  rest_api_id = aws_api_gateway_rest_api.this.id
  stage_name  = var.stage_name
}

output "api_gateway_id" {
  value = aws_api_gateway_rest_api.this.id
}

output "api_gateway_url" {
  value = "https://${aws_api_gateway_rest_api.this.id}.execute-api.${var.region}.amazonaws.com/${var.stage_name}"
}

output "execution_arn" {
  value = aws_api_gateway_rest_api.this.execution_arn
}
