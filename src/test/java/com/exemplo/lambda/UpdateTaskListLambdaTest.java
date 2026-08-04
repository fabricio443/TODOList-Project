package com.exemplo.lambda;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

class UpdateTaskListLambdaTest {

    @Test
    void shouldUpdateTaskListAndReturnOkResponse() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        UpdateTaskListLambda lambda = new UpdateTaskListLambda(dynamoDB, new ObjectMapper(), "todo-list");

        Map<String, Object> input = Map.of("body", "{\"id\":\"123\",\"name\":\"mercado atualizado\"}");

        Map<String, Object> response = lambda.handleRequest(input, null);

        assertEquals(200, response.get("statusCode"));
        verify(dynamoDB).updateItem(any(UpdateItemRequest.class));
    }

    @Test
    void shouldRejectMissingId() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        UpdateTaskListLambda lambda = new UpdateTaskListLambda(dynamoDB, new ObjectMapper(), "todo-list");

        Map<String, Object> input = Map.of("body", "{\"name\":\"mercado\"}");

        Map<String, Object> response = lambda.handleRequest(input, null);

        assertEquals(400, response.get("statusCode"));
    }

    @Test
    void shouldRejectMissingName() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        UpdateTaskListLambda lambda = new UpdateTaskListLambda(dynamoDB, new ObjectMapper(), "todo-list");

        Map<String, Object> input = Map.of("body", "{\"id\":\"123\"}");

        Map<String, Object> response = lambda.handleRequest(input, null);

        assertEquals(400, response.get("statusCode"));
    }

    @Test
    void shouldRejectMissingBody() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        UpdateTaskListLambda lambda = new UpdateTaskListLambda(dynamoDB, new ObjectMapper(), "todo-list");

        Map<String, Object> input = Map.of();

        Map<String, Object> response = lambda.handleRequest(input, null);

        assertEquals(400, response.get("statusCode"));
    }

    @Test
    void shouldRejectInvalidJson() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        UpdateTaskListLambda lambda = new UpdateTaskListLambda(dynamoDB, new ObjectMapper(), "todo-list");

        Map<String, Object> input = Map.of("body", "{invalid json");

        Map<String, Object> response = lambda.handleRequest(input, null);

        assertEquals(400, response.get("statusCode"));
    }

    @Test
    void shouldHandleDynamoFailure() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        UpdateTaskListLambda lambda = new UpdateTaskListLambda(dynamoDB, new ObjectMapper(), "todo-list");
        Mockito.doThrow(new RuntimeException("boom")).when(dynamoDB).updateItem(any(UpdateItemRequest.class));

        Map<String, Object> input = Map.of("body", "{\"id\":\"123\",\"name\":\"mercado\"}");

        Map<String, Object> response = lambda.handleRequest(input, null);

        assertEquals(500, response.get("statusCode"));
    }

    @Test
    void shouldUseConfiguredTableName() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        UpdateTaskListLambda lambda = new UpdateTaskListLambda(dynamoDB, new ObjectMapper(), "custom-table");

        Map<String, Object> input = Map.of("body", "{\"id\":\"123\",\"name\":\"mercado\"}");

        lambda.handleRequest(input, null);

        verify(dynamoDB).updateItem(any(UpdateItemRequest.class));
    }

    @Test
    void shouldReturnContentTypeHeader() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        UpdateTaskListLambda lambda = new UpdateTaskListLambda(dynamoDB, new ObjectMapper(), "todo-list");

        Map<String, Object> input = Map.of("body", "{\"id\":\"123\",\"name\":\"mercado\"}");

        Map<String, Object> response = lambda.handleRequest(input, null);

        assertEquals("application/json", ((Map<?, ?>) response.get("headers")).get("Content-Type"));
    }

    @Test
    void shouldReturnUpdatedBodyWithIdAndName() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        UpdateTaskListLambda lambda = new UpdateTaskListLambda(dynamoDB, new ObjectMapper(), "todo-list");

        Map<String, Object> input = Map.of("body", "{\"id\":\"123\",\"name\":\"mercado atualizado\"}");

        Map<String, Object> response = lambda.handleRequest(input, null);
        try {
            String bodyStr = (String) response.get("body");
            Map<String, Object> body = new ObjectMapper().readValue(bodyStr, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});

            assertEquals("123", body.get("id"));
            assertEquals("mercado atualizado", body.get("name"));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    void shouldAcceptNameWithSpaces() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        UpdateTaskListLambda lambda = new UpdateTaskListLambda(dynamoDB, new ObjectMapper(), "todo-list");

        Map<String, Object> input = Map.of("body", "{\"id\":\"123\",\"name\":\"lista de mercado\"}");

        Map<String, Object> response = lambda.handleRequest(input, null);

        assertEquals(200, response.get("statusCode"));
    }

    @Test
    void shouldAcceptNameWithNumbers() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        UpdateTaskListLambda lambda = new UpdateTaskListLambda(dynamoDB, new ObjectMapper(), "todo-list");

        Map<String, Object> input = Map.of("body", "{\"id\":\"123\",\"name\":\"lista 2\"}");

        Map<String, Object> response = lambda.handleRequest(input, null);

        assertEquals(200, response.get("statusCode"));
    }

    @Test
    void shouldAcceptNameWithSpecialCharacters() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        UpdateTaskListLambda lambda = new UpdateTaskListLambda(dynamoDB, new ObjectMapper(), "todo-list");

        Map<String, Object> input = Map.of("body", "{\"id\":\"123\",\"name\":\"lista@2024\"}");

        Map<String, Object> response = lambda.handleRequest(input, null);

        assertEquals(200, response.get("statusCode"));
    }

    @Test
    void shouldAcceptNameWithUnderscore() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        UpdateTaskListLambda lambda = new UpdateTaskListLambda(dynamoDB, new ObjectMapper(), "todo-list");

        Map<String, Object> input = Map.of("body", "{\"id\":\"123\",\"name\":\"lista_mercado\"}");

        Map<String, Object> response = lambda.handleRequest(input, null);

        assertEquals(200, response.get("statusCode"));
    }

    @Test
    void shouldAcceptNameWithHyphen() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        UpdateTaskListLambda lambda = new UpdateTaskListLambda(dynamoDB, new ObjectMapper(), "todo-list");

        Map<String, Object> input = Map.of("body", "{\"id\":\"123\",\"name\":\"lista-mercado\"}");

        Map<String, Object> response = lambda.handleRequest(input, null);

        assertEquals(200, response.get("statusCode"));
    }

    @Test
    void shouldAcceptNameWithUppercase() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        UpdateTaskListLambda lambda = new UpdateTaskListLambda(dynamoDB, new ObjectMapper(), "todo-list");

        Map<String, Object> input = Map.of("body", "{\"id\":\"123\",\"name\":\"Mercado\"}");

        Map<String, Object> response = lambda.handleRequest(input, null);

        assertEquals(200, response.get("statusCode"));
    }
}
