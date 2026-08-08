module "dynamodb" {
  source      = "../../modules/dynamodb"
  region      = "us-east-1"
  environment = "dev"
  table_name  = "todo-list"
}

module "lambda_create" {
  source          = "../../modules/lambda"
  region          = "us-east-1"
  function_name   = "todo-create-task-list"
  lambda_zip_path = "../../../target/todolist-project-1.0-SNAPSHOT.jar"
  table_name      = module.dynamodb.table_name
  table_arn       = module.dynamodb.table_arn
  handler         = "com.exemplo.lambda.CreateTaskListLambda::handleRequest"
  dynamodb_actions = ["dynamodb:PutItem"]
}

module "lambda_list" {
  source          = "../../modules/lambda"
  region          = "us-east-1"
  function_name   = "todo-list-task-lists"
  lambda_zip_path = "../../../target/todolist-project-1.0-SNAPSHOT.jar"
  table_name      = module.dynamodb.table_name
  table_arn       = module.dynamodb.table_arn
  handler         = "com.exemplo.lambda.ListTaskListsLambda::handleRequest"
  dynamodb_actions = ["dynamodb:Query"]
}

module "lambda_update" {
  source          = "../../modules/lambda"
  region          = "us-east-1"
  function_name   = "todo-update-task-list"
  lambda_zip_path = "../../../target/todolist-project-1.0-SNAPSHOT.jar"
  table_name      = module.dynamodb.table_name
  table_arn       = module.dynamodb.table_arn
  handler         = "com.exemplo.lambda.UpdateTaskListLambda::handleRequest"
  dynamodb_actions = ["dynamodb:UpdateItem"]
}

module "lambda_create_task_item" {
  source          = "../../modules/lambda"
  region          = "us-east-1"
  function_name   = "todo-create-task-item"
  lambda_zip_path = "../../../target/todolist-project-1.0-SNAPSHOT.jar"
  table_name      = module.dynamodb.table_name
  table_arn       = module.dynamodb.table_arn
  handler         = "com.exemplo.lambda.CreateTaskItemLambda::handleRequest"
  dynamodb_actions = ["dynamodb:GetItem", "dynamodb:PutItem"]
}

module "lambda_list_task_items" {
  source          = "../../modules/lambda"
  region          = "us-east-1"
  function_name   = "todo-list-task-items"
  lambda_zip_path = "../../../target/todolist-project-1.0-SNAPSHOT.jar"
  table_name      = module.dynamodb.table_name
  table_arn       = module.dynamodb.table_arn
  handler         = "com.exemplo.lambda.ListTaskItemsLambda::handleRequest"
  dynamodb_actions = ["dynamodb:Query"]
}

module "lambda_update_task_item" {
  source          = "../../modules/lambda"
  region          = "us-east-1"
  function_name   = "todo-update-task-item"
  lambda_zip_path = "../../../target/todolist-project-1.0-SNAPSHOT.jar"
  table_name      = module.dynamodb.table_name
  table_arn       = module.dynamodb.table_arn
  handler         = "com.exemplo.lambda.UpdateTaskItemLambda::handleRequest"
  dynamodb_actions = ["dynamodb:GetItem", "dynamodb:UpdateItem"]
}

module "lambda_get_task_list" {
  source          = "../../modules/lambda"
  region          = "us-east-1"
  function_name   = "todo-get-task-list"
  lambda_zip_path = "../../../target/todolist-project-1.0-SNAPSHOT.jar"
  table_name      = module.dynamodb.table_name
  table_arn       = module.dynamodb.table_arn
  handler         = "com.exemplo.lambda.GetTaskListLambda::handleRequest"
  dynamodb_actions = ["dynamodb:GetItem"]
}

module "apigateway" {
  source                      = "../../modules/apigateway"
  region                      = "us-east-1"
  api_name                    = "todo-list-api"
  stage_name                  = "dev"
  create_lambda_invoke_arn    = module.lambda_create.aws_lambda_function_invoke_arn
  create_lambda_function_name = module.lambda_create.aws_lambda_function_name
  list_lambda_invoke_arn      = module.lambda_list.aws_lambda_function_invoke_arn
  list_lambda_function_name   = module.lambda_list.aws_lambda_function_name
  update_lambda_invoke_arn    = module.lambda_update.aws_lambda_function_invoke_arn
  update_lambda_function_name = module.lambda_update.aws_lambda_function_name
  add_task_lambda_invoke_arn  = module.lambda_create_task_item.aws_lambda_function_invoke_arn
  add_task_lambda_function_name = module.lambda_create_task_item.aws_lambda_function_name
  list_task_items_lambda_invoke_arn = module.lambda_list_task_items.aws_lambda_function_invoke_arn
  list_task_items_lambda_function_name = module.lambda_list_task_items.aws_lambda_function_name
  update_task_item_lambda_invoke_arn = module.lambda_update_task_item.aws_lambda_function_invoke_arn
  update_task_item_lambda_function_name = module.lambda_update_task_item.aws_lambda_function_name
  get_task_list_lambda_invoke_arn = module.lambda_get_task_list.aws_lambda_function_invoke_arn
  get_task_list_lambda_function_name = module.lambda_get_task_list.aws_lambda_function_name
}

output "cognito_user_pool_id" {
  value = module.apigateway.cognito_user_pool_id
}

output "cognito_user_pool_client_id" {
  value = module.apigateway.cognito_user_pool_client_id
}
