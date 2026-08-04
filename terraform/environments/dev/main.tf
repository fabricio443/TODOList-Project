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

module "apigateway" {
  source                    = "../../modules/apigateway"
  region                    = "us-east-1"
  api_name                  = "todo-list-api"
  stage_name                = "dev"
  create_lambda_invoke_arn  = module.lambda_create.aws_lambda_function_invoke_arn
  create_lambda_function_name = module.lambda_create.aws_lambda_function_name
  list_lambda_invoke_arn    = module.lambda_list.aws_lambda_function_invoke_arn
  list_lambda_function_name = module.lambda_list.aws_lambda_function_name
  update_lambda_invoke_arn  = module.lambda_update.aws_lambda_function_invoke_arn
  update_lambda_function_name = module.lambda_update.aws_lambda_function_name
}
