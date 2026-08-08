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

resource "aws_cognito_user_pool" "this" {
  name = "todo-list-dev-user-pool"

  password_policy {
    minimum_length                   = 8
    require_lowercase                = true
    require_numbers                  = true
    require_symbols                  = false
    require_uppercase                = true
    temporary_password_validity_days = 7
  }

  auto_verified_attributes = ["email"]
  username_attributes      = ["email"]
}

resource "aws_cognito_user_pool_client" "this" {
  name         = "todo-list-dev-app-client"
  user_pool_id = aws_cognito_user_pool.this.id

  explicit_auth_flows = [
    "ALLOW_USER_PASSWORD_AUTH",
    "ALLOW_REFRESH_TOKEN_AUTH"
  ]

  generate_secret = false
}

resource "aws_api_gateway_authorizer" "cognito" {
  name            = "todo-list-cognito-authorizer"
  rest_api_id     = aws_api_gateway_rest_api.this.id
  type            = "COGNITO_USER_POOLS"
  identity_source = "method.request.header.Authorization"
  provider_arns   = [aws_cognito_user_pool.this.arn]
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
  authorization = "COGNITO_USER_POOLS"
  authorizer_id = aws_api_gateway_authorizer.cognito.id
}

resource "aws_api_gateway_method" "get" {
  rest_api_id   = aws_api_gateway_rest_api.this.id
  resource_id   = aws_api_gateway_resource.task_lists.id
  http_method   = "GET"
  authorization = "COGNITO_USER_POOLS"
  authorizer_id = aws_api_gateway_authorizer.cognito.id
}

resource "aws_api_gateway_method" "put" {
  rest_api_id   = aws_api_gateway_rest_api.this.id
  resource_id   = aws_api_gateway_resource.task_lists.id
  http_method   = "PUT"
  authorization = "COGNITO_USER_POOLS"
  authorizer_id = aws_api_gateway_authorizer.cognito.id
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

resource "aws_api_gateway_resource" "task_list_id" {
  rest_api_id = aws_api_gateway_rest_api.this.id
  parent_id   = aws_api_gateway_resource.task_lists.id
  path_part   = "{listId}"
}

resource "aws_api_gateway_method" "task_list_get" {
  rest_api_id   = aws_api_gateway_rest_api.this.id
  resource_id   = aws_api_gateway_resource.task_list_id.id
  http_method   = "GET"
  authorization = "COGNITO_USER_POOLS"
  authorizer_id = aws_api_gateway_authorizer.cognito.id
}

resource "aws_api_gateway_resource" "task_list_tasks" {
  rest_api_id = aws_api_gateway_rest_api.this.id
  parent_id   = aws_api_gateway_resource.task_list_id.id
  path_part   = "tasks"
}

resource "aws_api_gateway_method" "task_list_tasks_post" {
  rest_api_id   = aws_api_gateway_rest_api.this.id
  resource_id   = aws_api_gateway_resource.task_list_tasks.id
  http_method   = "POST"
  authorization = "COGNITO_USER_POOLS"
  authorizer_id = aws_api_gateway_authorizer.cognito.id
}

resource "aws_api_gateway_method" "task_list_tasks_get" {
  rest_api_id   = aws_api_gateway_rest_api.this.id
  resource_id   = aws_api_gateway_resource.task_list_tasks.id
  http_method   = "GET"
  authorization = "COGNITO_USER_POOLS"
  authorizer_id = aws_api_gateway_authorizer.cognito.id
}

resource "aws_api_gateway_integration" "task_list_tasks_post_integration" {
  rest_api_id             = aws_api_gateway_rest_api.this.id
  resource_id             = aws_api_gateway_resource.task_list_tasks.id
  http_method             = aws_api_gateway_method.task_list_tasks_post.http_method
  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = var.add_task_lambda_invoke_arn
}

resource "aws_api_gateway_integration" "task_list_tasks_get_integration" {
  rest_api_id             = aws_api_gateway_rest_api.this.id
  resource_id             = aws_api_gateway_resource.task_list_tasks.id
  http_method             = aws_api_gateway_method.task_list_tasks_get.http_method
  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = var.list_task_items_lambda_invoke_arn
}

resource "aws_api_gateway_integration" "task_list_get_integration" {
  rest_api_id             = aws_api_gateway_rest_api.this.id
  resource_id             = aws_api_gateway_resource.task_list_id.id
  http_method             = aws_api_gateway_method.task_list_get.http_method
  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = var.get_task_list_lambda_invoke_arn
}

resource "aws_api_gateway_resource" "task_list_tasks_taskid" {
  rest_api_id = aws_api_gateway_rest_api.this.id
  parent_id   = aws_api_gateway_resource.task_list_tasks.id
  path_part   = "{taskId}"
}

resource "aws_api_gateway_method" "task_list_tasks_taskid_put" {
  rest_api_id   = aws_api_gateway_rest_api.this.id
  resource_id   = aws_api_gateway_resource.task_list_tasks_taskid.id
  http_method   = "PUT"
  authorization = "COGNITO_USER_POOLS"
  authorizer_id = aws_api_gateway_authorizer.cognito.id
}

resource "aws_api_gateway_method" "task_list_tasks_taskid_delete" {
  rest_api_id   = aws_api_gateway_rest_api.this.id
  resource_id   = aws_api_gateway_resource.task_list_tasks_taskid.id
  http_method   = "DELETE"
  authorization = "COGNITO_USER_POOLS"
  authorizer_id = aws_api_gateway_authorizer.cognito.id
}

resource "aws_api_gateway_integration" "task_list_tasks_taskid_put_integration" {
  rest_api_id             = aws_api_gateway_rest_api.this.id
  resource_id             = aws_api_gateway_resource.task_list_tasks_taskid.id
  http_method             = aws_api_gateway_method.task_list_tasks_taskid_put.http_method
  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = var.update_task_item_lambda_invoke_arn
}

resource "aws_api_gateway_integration" "task_list_tasks_taskid_delete_integration" {
  rest_api_id             = aws_api_gateway_rest_api.this.id
  resource_id             = aws_api_gateway_resource.task_list_tasks_taskid.id
  http_method             = aws_api_gateway_method.task_list_tasks_taskid_delete.http_method
  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = var.delete_task_item_lambda_invoke_arn
}

resource "aws_lambda_permission" "add_task_permission" {
  statement_id  = "AllowExecutionFromAPIGatewayAddTask"
  action        = "lambda:InvokeFunction"
  function_name = var.add_task_lambda_function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.this.execution_arn}/*/*"
}

resource "aws_lambda_permission" "list_task_items_permission" {
  statement_id  = "AllowExecutionFromAPIGatewayListTaskItems"
  action        = "lambda:InvokeFunction"
  function_name = var.list_task_items_lambda_function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.this.execution_arn}/*/*"
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

resource "aws_lambda_permission" "update_task_item_permission" {
  statement_id  = "AllowExecutionFromAPIGatewayUpdateTaskItem"
  action        = "lambda:InvokeFunction"
  function_name = var.update_task_item_lambda_function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.this.execution_arn}/*/*"
}

resource "aws_lambda_permission" "get_task_list_permission" {
  statement_id  = "AllowExecutionFromAPIGatewayGetTaskList"
  action        = "lambda:InvokeFunction"
  function_name = var.get_task_list_lambda_function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.this.execution_arn}/*/*"
}

resource "aws_lambda_permission" "delete_task_item_permission" {
  statement_id  = "AllowExecutionFromAPIGatewayDeleteTaskItem"
  action        = "lambda:InvokeFunction"
  function_name = var.delete_task_item_lambda_function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.this.execution_arn}/*/*"
}

resource "aws_api_gateway_deployment" "this" {
  depends_on = [
    aws_api_gateway_integration.post_integration,
    aws_api_gateway_integration.get_integration,
    aws_api_gateway_integration.put_integration,
    aws_api_gateway_integration.task_list_get_integration,
    aws_api_gateway_integration.task_list_tasks_post_integration,
    aws_api_gateway_integration.task_list_tasks_get_integration,
    aws_api_gateway_integration.task_list_tasks_taskid_put_integration,
    aws_api_gateway_integration.task_list_tasks_taskid_delete_integration,
    aws_lambda_permission.create_permission,
    aws_lambda_permission.list_permission,
    aws_lambda_permission.update_permission,
    aws_lambda_permission.get_task_list_permission,
    aws_lambda_permission.add_task_permission,
    aws_lambda_permission.list_task_items_permission,
    aws_lambda_permission.update_task_item_permission,
    aws_lambda_permission.delete_task_item_permission
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

output "cognito_user_pool_id" {
  value = aws_cognito_user_pool.this.id
}

output "cognito_user_pool_client_id" {
  value = aws_cognito_user_pool_client.this.id
}

output "cognito_authorizer_id" {
  value = aws_api_gateway_authorizer.cognito.id
}
