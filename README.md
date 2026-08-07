# TODOList-Project

## API para gerenciamento de listas de tarefas

## Descrição

API desenvolvida utilizando Java, AWS Lambda e DynamoDB para gerenciamento de listas de tarefas.

A aplicação utiliza o padrão **DynamoDB Single Table Design**, permitindo que diferentes entidades sejam armazenadas na mesma tabela utilizando uma estratégia de chaves composta.

## Funcionalidades implementadas

* Criar listas de tarefas
* Listar listas de tarefas utilizando DynamoDB GSI
* Atualizar listas de tarefas
* Infraestrutura provisionada com Terraform

## Arquitetura DynamoDB

A tabela utiliza o modelo Single Table Design.

### Entidade: Lista

Chave principal:

```
PK = LIST#<listaId>
SK = META
```

Exemplo:

```json
{
  "PK": "LIST#85c429db-c426-447c-940e-702ee4ee4612",
  "SK": "META",
  "name": "Estudar AWS DynamoDB"
}
```

### Índice Global Secundário (GSI1)

O GSI1 é utilizado para listar todas as listas existentes.

Estrutura:

```
GSI1PK = LIST
GSI1SK = LIST#<listaId>
```

Consulta de listas:

```
Query GSI1

GSI1PK = LIST
```

## Estrutura do projeto

```
TODOList-Project
│
├── src/
│   ├── main/
│   │   └── java/
│   │       └── com/exemplo/lambda/
│   │           ├── CreateTaskListLambda.java
│   │           ├── ListTaskListsLambda.java
│   │           └── UpdateTaskListLambda.java
│   │
│   └── test/
│       └── java/
│           └── com/exemplo/lambda/
│
├── terraform/
│   └── Infraestrutura AWS
│
├── scripts/
│
├── pom.xml
└── README.md
```

## Tecnologias utilizadas

* Java 17
* AWS Lambda
* Amazon DynamoDB
* Amazon API Gateway
* Terraform
* Maven
* AWS SDK for Java

## Requisitos

Antes de executar o projeto, é necessário possuir:

* Java 17 instalado
* Maven instalado
* AWS CLI configurado
* Terraform instalado

## Executando os testes

Execute:

```bash
mvn test
```

## Gerando o pacote das Lambdas

Execute:

```bash
mvn package
```

## Deploy da infraestrutura

Acesse o diretório do ambiente Terraform:

```bash
cd terraform/environments/dev
```

Inicialize o Terraform:

```bash
terraform init
```

Valide as alterações:

```bash
terraform plan
```

Aplique a infraestrutura:

```bash
terraform apply
```

## Endpoints da API

### Criar lista

**POST**

```
/task-lists
```

Body:

```json
{
  "name": "Estudar AWS DynamoDB"
}
```

Resposta:

```json
{
  "name": "Estudar AWS DynamoDB",
  "id": "85c429db-c426-447c-940e-702ee4ee4612"
}
```

---

### Listar listas

**GET**

```
/task-lists
```

Resposta:

```json
[
  {
    "name": "Estudar AWS DynamoDB",
    "id": "85c429db-c426-447c-940e-702ee4ee4612"
  }
]
```

---

### Atualizar lista

**PUT**

```
/task-lists
```

Body:

```json
{
  "id": "85c429db-c426-447c-940e-702ee4ee4612",
  "name": "Estudar DynamoDB Single Table Design"
}
```

Resposta:

```json
{
  "name": "Estudar DynamoDB Single Table Design",
  "id": "85c429db-c426-447c-940e-702ee4ee4612"
}
```

---

### Listar tarefas de uma lista

**GET**

```
/task-lists/{listId}/tasks
```

Resposta:

```json
[
  {
    "taskId": "123",
    "name": "Estudar Lambda Java",
    "status": "PENDING",
    "createdAt": "2026-08-07T01:16:24Z"
  }
]
```

## Próximas implementações

* Criar CRUD de tarefas (Tasks)
* Implementar relacionamento entre listas e tarefas utilizando Single Table Design:

```
PK = LIST#<listaId>
SK = TASK#<taskId>
```

* Implementar remoção de listas e tarefas
