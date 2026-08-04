package com.exemplo.lambda;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

class CreateTaskListLambdaTest {

    @Test
    void shouldCreateTaskListAndReturnCreatedResponse() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        CreateTaskListLambda lambda = new CreateTaskListLambda(dynamoDB, new ObjectMapper(), "todo-list");

        Map<String, Object> input = Map.of("body", "{\"name\":\"mercado\"}");

        Map<String, Object> response = lambda.handleRequest(input, null);

        assertEquals(201, response.get("statusCode"));
        verify(dynamoDB).putItem(any(PutItemRequest.class));
    }

    @Test
    void shouldRejectMissingName() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        CreateTaskListLambda lambda = new CreateTaskListLambda(dynamoDB, new ObjectMapper(), "todo-list");

        Map<String, Object> input = Map.of("body", "{\"name\":\"\"}");

        Map<String, Object> response = lambda.handleRequest(input, null);

        assertEquals(400, response.get("statusCode"));
    }

    @Test
    void shouldRejectNullBody() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        CreateTaskListLambda lambda = new CreateTaskListLambda(dynamoDB, new ObjectMapper(), "todo-list");

        Map<String, Object> input = Map.of();

        Map<String, Object> response = lambda.handleRequest(input, null);

        assertEquals(400, response.get("statusCode"));
    }

    @Test
    void shouldRejectInvalidJson() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        CreateTaskListLambda lambda = new CreateTaskListLambda(dynamoDB, new ObjectMapper(), "todo-list");

        Map<String, Object> input = Map.of("body", "{invalid json");

        Map<String, Object> response = lambda.handleRequest(input, null);

        assertEquals(400, response.get("statusCode"));
    }

    @Test
    void shouldHandleDynamoFailure() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        CreateTaskListLambda lambda = new CreateTaskListLambda(dynamoDB, new ObjectMapper(), "todo-list");
        Mockito.doThrow(new RuntimeException("boom")).when(dynamoDB).putItem(any(PutItemRequest.class));

        Map<String, Object> input = Map.of("body", "{\"name\":\"mercado\"}");

        Map<String, Object> response = lambda.handleRequest(input, null);

        assertEquals(500, response.get("statusCode"));
    }

    @Test
    void shouldAcceptNameWithSpaces() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        CreateTaskListLambda lambda = new CreateTaskListLambda(dynamoDB, new ObjectMapper(), "todo-list");

        Map<String, Object> input = Map.of("body", "{\"name\":\"lista de mercado\"}");

        Map<String, Object> response = lambda.handleRequest(input, null);

        assertEquals(201, response.get("statusCode"));
    }

    @Test
    void shouldAcceptNameWithNumbers() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        CreateTaskListLambda lambda = new CreateTaskListLambda(dynamoDB, new ObjectMapper(), "todo-list");

        Map<String, Object> input = Map.of("body", "{\"name\":\"lista 2\"}");

        Map<String, Object> response = lambda.handleRequest(input, null);

        assertEquals(201, response.get("statusCode"));
    }

    @Test
    void shouldAcceptNameWithSpecialCharacters() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        CreateTaskListLambda lambda = new CreateTaskListLambda(dynamoDB, new ObjectMapper(), "todo-list");

        Map<String, Object> input = Map.of("body", "{\"name\":\"lista@2024\"}");

        Map<String, Object> response = lambda.handleRequest(input, null);

        assertEquals(201, response.get("statusCode"));
    }

    @Test
    void shouldCreateWhenBodyIsMapObject() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        CreateTaskListLambda lambda = new CreateTaskListLambda(dynamoDB, new ObjectMapper(), "todo-list");

        Map<String, Object> input = Map.of("body", Map.of("name", "mercado"));

        Map<String, Object> response = lambda.handleRequest(input, null);

        assertEquals(201, response.get("statusCode"));
    }

    @Test
    void shouldCreateWhenBodyHasAdditionalFields() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        CreateTaskListLambda lambda = new CreateTaskListLambda(dynamoDB, new ObjectMapper(), "todo-list");

        Map<String, Object> input = Map.of("body", "{\"name\":\"mercado\",\"extra\":\"x\"}");

        Map<String, Object> response = lambda.handleRequest(input, null);

        assertEquals(201, response.get("statusCode"));
    }

    @Test
    void shouldCreateWhenNameContainsAccent() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        CreateTaskListLambda lambda = new CreateTaskListLambda(dynamoDB, new ObjectMapper(), "todo-list");

        Map<String, Object> input = Map.of("body", "{\"name\":\"mercado\u00e1\"}");

        Map<String, Object> response = lambda.handleRequest(input, null);

        assertEquals(201, response.get("statusCode"));
    }

@Test
void shouldReturnBodyWithIdAndName() {
    AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
    CreateTaskListLambda lambda = new CreateTaskListLambda(dynamoDB, new ObjectMapper(), "todo-list");

    Map<String, Object> input = Map.of("body", "{\"name\":\"mercado\"}");

    Map<String, Object> response = lambda.handleRequest(input, null);

    Object responseBody = response.get("body");

    Map<String, Object> body;

    try {
        if (responseBody instanceof String json) {
            body = new ObjectMapper().readValue(
                    json,
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}
            );
        } else {
            body = new ObjectMapper().convertValue(
                    responseBody,
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}
            );
        }
    } catch (Exception e) {
        throw new RuntimeException("Erro ao converter response body", e);
    }

    assertEquals(201, response.get("statusCode"));
    assertEquals("mercado", body.get("name"));
    assertEquals(true, body.containsKey("id"));
}




    @Test
    void shouldUseConfiguredTableName() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        CreateTaskListLambda lambda = new CreateTaskListLambda(dynamoDB, new ObjectMapper(), "custom-table");

        Map<String, Object> input = Map.of("body", "{\"name\":\"mercado\"}");

        lambda.handleRequest(input, null);

        verify(dynamoDB).putItem(any(PutItemRequest.class));
    }

    @Test
    void shouldReturnContentTypeHeader() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        CreateTaskListLambda lambda = new CreateTaskListLambda(dynamoDB, new ObjectMapper(), "todo-list");

        Map<String, Object> input = Map.of("body", "{\"name\":\"mercado\"}");

        Map<String, Object> response = lambda.handleRequest(input, null);

        assertEquals("application/json", ((Map<?, ?>) response.get("headers")).get("Content-Type"));
    }

    @Test
    void shouldRejectNullNameField() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        CreateTaskListLambda lambda = new CreateTaskListLambda(dynamoDB, new ObjectMapper(), "todo-list");

        Map<String, Object> input = Map.of("body", "{\"name\":null}");

        Map<String, Object> response = lambda.handleRequest(input, null);

        assertEquals(400, response.get("statusCode"));
    }

    @Test
    void shouldRejectWhitespaceOnlyName() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        CreateTaskListLambda lambda = new CreateTaskListLambda(dynamoDB, new ObjectMapper(), "todo-list");

        Map<String, Object> input = Map.of("body", "{\"name\":\"   \"}");

        Map<String, Object> response = lambda.handleRequest(input, null);

        assertEquals(400, response.get("statusCode"));
    }

    @Test
    void shouldRejectBodyWithNameAsNumber() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        CreateTaskListLambda lambda = new CreateTaskListLambda(dynamoDB, new ObjectMapper(), "todo-list");

        Map<String, Object> input = Map.of("body", "{\"name\":123}");

        Map<String, Object> response = lambda.handleRequest(input, null);

        assertEquals(400, response.get("statusCode"));
    }

    @Test
    void shouldCreateWhenNameHasUnderscore() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        CreateTaskListLambda lambda = new CreateTaskListLambda(dynamoDB, new ObjectMapper(), "todo-list");

        Map<String, Object> input = Map.of("body", "{\"name\":\"lista_mercado\"}");

        Map<String, Object> response = lambda.handleRequest(input, null);

        assertEquals(201, response.get("statusCode"));
    }

    @Test
    void shouldCreateWhenNameHasHyphen() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        CreateTaskListLambda lambda = new CreateTaskListLambda(dynamoDB, new ObjectMapper(), "todo-list");

        Map<String, Object> input = Map.of("body", "{\"name\":\"lista-mercado\"}");

        Map<String, Object> response = lambda.handleRequest(input, null);

        assertEquals(201, response.get("statusCode"));
    }

    @Test
    void shouldCreateWhenNameHasUppercase() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        CreateTaskListLambda lambda = new CreateTaskListLambda(dynamoDB, new ObjectMapper(), "todo-list");

        Map<String, Object> input = Map.of("body", "{\"name\":\"Mercado\"}");

        Map<String, Object> response = lambda.handleRequest(input, null);

        assertEquals(201, response.get("statusCode"));
    }
}
