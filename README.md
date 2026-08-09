# 📝 TODOList Project

> API serverless para gerenciamento de listas e tarefas, construída em **Java** sobre a **AWS**, com infraestrutura provisionada via **Terraform** e arquitetura orientada a eventos para processamento assíncrono de relatórios.

---

## 📑 Sumário

* [Sobre o projeto](#-sobre-o-projeto)
* [Tecnologias](#-tecnologias)
* [Arquitetura](#️-arquitetura)
* [Funcionalidades](#-funcionalidades)
* [Fluxo de relatórios](#-fluxo-de-relatórios)
* [Modelagem de dados](#️-modelagem-de-dados-dynamodb)
* [Autenticação](#-autenticação)
* [Infraestrutura como código](#️-infraestrutura-como-código)
* [Estrutura do projeto](#-estrutura-do-projeto)
* [Pré-requisitos](#-pré-requisitos)
* [Como executar](#️-como-executar)
* [Testes](#-testes)
* [Deploy](#-deploy)
* [Objetivo do projeto](#-objetivo-do-projeto)
* [Autor](#-autor)

---

## 📖 Sobre o projeto

O **TODOList Project** é uma API serverless para criação e gerenciamento de listas de tarefas (*task lists*) e seus itens (*task items*), com suporte à geração assíncrona de relatórios enviados por e-mail.

O projeto foi desenvolvido para explorar, na prática, uma arquitetura serverless utilizando serviços da AWS, abrangendo autenticação, persistência de dados, APIs REST, mensageria, armazenamento de objetos, processamento assíncrono e infraestrutura como código.

---

## 🚀 Tecnologias

| Categoria              | Tecnologia         |
| ---------------------- | ------------------ |
| **Linguagem**          | Java 17            |
| **Build**              | Maven              |
| **Compute**            | AWS Lambda         |
| **API**                | Amazon API Gateway |
| **Banco de dados**     | Amazon DynamoDB    |
| **Autenticação**       | Amazon Cognito     |
| **Mensageria**         | Amazon SQS         |
| **Armazenamento**      | Amazon S3          |
| **E-mail**             | Amazon SES         |
| **Segurança e acesso** | AWS IAM            |
| **Infraestrutura**     | Terraform          |
| **Testes**             | JUnit, Mockito     |

---

## 🏗️ Arquitetura

```text
                         ┌──────────────────┐
                         │    API Client    │
                         │  Postman / App   │
                         └────────┬─────────┘
                                  │
                                  ▼
                         ┌──────────────────┐
                         │   API Gateway    │
                         └────────┬─────────┘
                                  │
                        Autorização via Cognito
                                  │
              ┌───────────────────┴───────────────────┐
              │                                       │
              ▼                                       ▼
     ┌──────────────────────┐              ┌──────────────────────┐
     │ Task List / Task Item│              │  Submit User Request │
     │        Lambdas       │              │        Lambda        │
     └───────────┬──────────┘              └───────────┬──────────┘
                 │                                     │
                 ▼                                     ▼
          ┌────────────┐                         ┌────────────┐
          │  DynamoDB  │                         │  Amazon SQS│
          └────────────┘                         └─────┬──────┘
                                                       │
                                                       ▼
                                             ┌──────────────────────┐
                                             │ Process User Request │
                                             │        Lambda        │
                                             └───────────┬──────────┘
                                                         │
                                  ┌──────────────────────┼──────────────────────┐
                                  ▼                      ▼                      ▼
                            ┌───────────┐          ┌───────────┐          ┌───────────┐
                            │    S3     │          │ DynamoDB  │          │    SES    │
                            │ Relatório │          │  Consulta │          │  E-mail   │
                            └───────────┘          └───────────┘          └───────────┘
```

### Resumo do fluxo

1. O cliente autentica-se utilizando **Amazon Cognito**.
2. O cliente envia requisições autenticadas para o **API Gateway**.
3. O API Gateway direciona as requisições para as **AWS Lambda Functions**.
4. As operações de listas e tarefas utilizam o **Amazon DynamoDB** para persistência.
5. Solicitações de relatório são enviadas de forma assíncrona para o **Amazon SQS**.
6. A `ProcessUserRequestLambda` consome a mensagem da fila.
7. A Lambda consulta os dados no **DynamoDB**.
8. O relatório é gerado e armazenado no **Amazon S3**.
9. O usuário recebe uma notificação por e-mail através do **Amazon SES**.

---

## 📋 Funcionalidades

### 📌 Task Lists

* ➕ Criar lista de tarefas
* 🔍 Consultar uma lista
* 📄 Listar listas do usuário
* ✏️ Atualizar lista
* 🗑️ Excluir lista

### ✅ Task Items

* ➕ Criar tarefa em uma lista
* 📄 Listar tarefas de uma lista
* ✏️ Atualizar tarefa
* 🗑️ Excluir tarefa

### 📊 Relatórios

* 📨 Solicitar geração de relatório
* ⚡ Processar a solicitação de forma assíncrona
* 📄 Gerar relatório com os dados da lista
* ☁️ Armazenar o relatório no Amazon S3
* 📬 Enviar o relatório por e-mail utilizando Amazon SES

---

## 📨 Fluxo de relatórios

O processamento de relatórios utiliza uma arquitetura assíncrona baseada em **Amazon SQS**:

```text
Cliente
   │
   ▼
POST /user-requests
   │
   ▼
API Gateway
   │
   ▼
SubmitUserRequestLambda
   │
   ▼
Amazon SQS
   │
   ▼
ProcessUserRequestLambda
   │
   ├──────────────► DynamoDB
   │                  │
   │                  ▼
   │              Dados da lista
   │
   ├──────────────► Amazon S3
   │                  │
   │                  ▼
   │             Arquivo CSV
   │
   └──────────────► Amazon SES
                      │
                      ▼
                  E-mail do usuário
```

A API retorna **HTTP 202 Accepted** imediatamente após a solicitação ser enviada para a fila, enquanto o processamento do relatório ocorre de forma independente.

Esse modelo permite desacoplar a requisição HTTP do processamento do relatório e evita que o cliente precise aguardar a conclusão da operação.

---

## 🗄️ Modelagem de dados — DynamoDB

O projeto utiliza **Single Table Design** para armazenar listas e tarefas em uma única tabela.

### Task List

```text
PK = LIST#<listId>
SK = META
```

### Task Item

```text
PK = LIST#<listId>
SK = TASK#<taskId>
```

O projeto também utiliza **Global Secondary Indexes (GSI)** para suportar consultas específicas sem a necessidade de realizar operações de `Scan`.

Essa abordagem permite organizar diferentes tipos de entidades dentro da mesma tabela, mantendo padrões de acesso previsíveis.

---

## 🔐 Autenticação

A autenticação é realizada utilizando **Amazon Cognito** e tokens **JWT**.

```text
Usuário
   │
   ▼
Amazon Cognito
   │
   ▼
JWT Token
   │
   ▼
API Gateway
   │
   ▼
AWS Lambda
```

O **API Gateway** utiliza o Cognito para validar a autenticação das requisições antes de encaminhá-las para as Lambdas.

As informações do usuário autenticado são utilizadas pela aplicação para garantir o acesso aos recursos pertencentes ao usuário.

---

## ☁️ Infraestrutura como código

Toda a infraestrutura da aplicação é provisionada utilizando **Terraform**.

A infraestrutura está organizada em módulos reutilizáveis e ambientes separados:

```text
terraform/
├── environments/
│   └── dev/
│
└── modules/
    ├── apigateway/
    ├── dynamodb/
    └── lambda/
```

Entre os principais recursos provisionados estão:

* AWS Lambda
* Amazon API Gateway
* Amazon DynamoDB
* Amazon Cognito
* Amazon SQS
* Amazon S3
* Amazon SES
* AWS IAM
* Event Source Mapping entre SQS e Lambda

---

## 📁 Estrutura do projeto

A estrutura atual do código Java está organizada por Lambda:

```text
TODOList-Project/
│
├── .github/
│   └── workflows/
│
├── scripts/
│
├── src/
│   ├── main/
│   │   └── java/
│   │       └── com/
│   │           └── exemplo/
│   │               └── lambda/
│   │                   ├── CreateTaskListLambda.java
│   │                   ├── GetTaskListLambda.java
│   │                   ├── ListTaskListsLambda.java
│   │                   ├── UpdateTaskListLambda.java
│   │                   ├── DeleteTaskListLambda.java
│   │                   ├── CreateTaskItemLambda.java
│   │                   ├── ListTaskItemsLambda.java
│   │                   ├── UpdateTaskItemLambda.java
│   │                   ├── DeleteTaskItemLambda.java
│   │                   ├── SubmitUserRequestLambda.java
│   │                   └── ProcessUserRequestLambda.java
│   │
│   └── test/
│       └── java/
│           └── com/
│               └── exemplo/
│                   └── lambda/
│
├── terraform/
│   ├── environments/
│   │   └── dev/
│   │
│   └── modules/
│       ├── apigateway/
│       ├── dynamodb/
│       └── lambda/
│
├── pom.xml
└── README.md
```

---

## 🔧 Pré-requisitos

Antes de executar o projeto, é necessário ter:

* Java 17
* Maven
* AWS CLI
* Terraform
* Conta AWS
* Credenciais AWS configuradas

Configure suas credenciais utilizando:

```bash
aws configure
```

---

## ▶️ Como executar

Clone o repositório:

```bash
git clone <url-do-repositorio>
cd TODOList-Project
```

Compile o projeto:

```bash
mvn clean package
```

Acesse o ambiente Terraform:

```bash
cd terraform/environments/dev
```

Inicialize o Terraform:

```bash
terraform init
```

Visualize o plano:

```bash
terraform plan
```

Aplique a infraestrutura:

```bash
terraform apply
```

---

## 🧪 Testes

Os testes automatizados utilizam **JUnit** e **Mockito**.

Para executar os testes:

```bash
mvn test
```

Para compilar o projeto:

```bash
mvn package
```

---

## 📦 Deploy

O deploy da aplicação é realizado utilizando **Terraform**.

```bash
cd terraform/environments/dev

terraform init
terraform plan
terraform apply
```

O Terraform é responsável por provisionar e atualizar os recursos AWS utilizados pela aplicação, incluindo as funções Lambda e suas respectivas configurações de infraestrutura.

---

## 🎯 Objetivo do projeto

O projeto foi desenvolvido com foco no aprendizado prático de **desenvolvimento backend, computação serverless e serviços AWS**.

Os principais conceitos explorados foram:

* 🔹 Desenvolvimento de APIs REST
* 🔹 Java 17
* 🔹 AWS Lambda
* 🔹 Amazon API Gateway
* 🔹 Amazon DynamoDB
* 🔹 Single Table Design
* 🔹 Amazon Cognito
* 🔹 JSON Web Tokens (JWT)
* 🔹 Amazon SQS
* 🔹 Processamento assíncrono
* 🔹 Amazon S3
* 🔹 Amazon SES
* 🔹 AWS IAM
* 🔹 Terraform
* 🔹 Infraestrutura como código
* 🔹 Testes automatizados com JUnit e Mockito
* 🔹 Arquitetura orientada a eventos

---

## 👨‍💻 Autor

**Fabrício Costa**

Backend Developer com foco em **Java, AWS e desenvolvimento de aplicações serverless**.
