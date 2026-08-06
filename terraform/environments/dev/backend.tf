terraform {
  backend "s3" {
    bucket       = "todolist-project-terraform-state-dev"
    key          = "todolist/dev/terraform.tfstate"
    region       = "us-east-1"
    use_lockfile = true
    encrypt      = true
  }
}