package com.exemplo.lambda;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mockito;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

class CreateTaskItemLambdaTest {

    @Test
    void shouldCreateTaskItemForExistingList() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        CreateTaskItemLambda lambda = new CreateTaskItemLambda(dynamoDB, new ObjectMapper(), "todo-list");

        GetItemResult getItemResult = new GetItemResult();
        getItemResult.setItem(Map.of("PK", new AttributeValue("LIST#123"), "SK", new AttributeValue("META"), "name", new AttributeValue("lista")));
        when(dynamoDB.getItem(any(GetItemRequest.class))).thenReturn(getItemResult);

        Map<String, Object> input = Map.of(
                "pathParameters", Map.of("listId", "123"),
                "body", "{\"name\":\"tarefa 1\"}"
        );

        Map<String, Object> response = lambda.handleRequest(input, null);

        assertEquals(201, response.get("statusCode"));
        verify(dynamoDB).putItem(any(PutItemRequest.class));
    }

    @Test
    void shouldReturn404WhenListDoesNotExist() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        CreateTaskItemLambda lambda = new CreateTaskItemLambda(dynamoDB, new ObjectMapper(), "todo-list");

        GetItemResult getItemResult = new GetItemResult();
        getItemResult.setItem(null);
        when(dynamoDB.getItem(any(GetItemRequest.class))).thenReturn(getItemResult);

        Map<String, Object> input = Map.of(
                "pathParameters", Map.of("listId", "missing"),
                "body", "{\"name\":\"tarefa 1\"}"
        );

        Map<String, Object> response = lambda.handleRequest(input, null);

        assertEquals(404, response.get("statusCode"));
    }

    @Test
    void shouldReturn400WhenPathParameterListIdMissing() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        CreateTaskItemLambda lambda = new CreateTaskItemLambda(dynamoDB, new ObjectMapper(), "todo-list");

        Map<String, Object> input = Map.of(
                "body", "{\"name\":\"tarefa 1\"}"
        );

        Map<String, Object> response = lambda.handleRequest(input, null);

        assertEquals(400, response.get("statusCode"));
    }

    @Test
    void shouldReturn400WhenNameEmpty() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        CreateTaskItemLambda lambda = new CreateTaskItemLambda(dynamoDB, new ObjectMapper(), "todo-list");

        Map<String, Object> input = Map.of(
                "pathParameters", Map.of("listId", "123"),
                "body", "{\"name\":\"   \"}"
        );

        Map<String, Object> response = lambda.handleRequest(input, null);

        assertEquals(400, response.get("statusCode"));
    }

    @Test
    void shouldReturn400ForInvalidJson() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        CreateTaskItemLambda lambda = new CreateTaskItemLambda(dynamoDB, new ObjectMapper(), "todo-list");

        Map<String, Object> input = Map.of(
                "pathParameters", Map.of("listId", "123"),
                "body", "{invalid json}"
        );

        Map<String, Object> response = lambda.handleRequest(input, null);

        assertEquals(400, response.get("statusCode"));
    }

    @Test
    void shouldReturn500OnDynamoError() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        CreateTaskItemLambda lambda = new CreateTaskItemLambda(dynamoDB, new ObjectMapper(), "todo-list");
        when(dynamoDB.getItem(any(GetItemRequest.class))).thenThrow(new RuntimeException("boom"));

        Map<String, Object> input = Map.of(
                "pathParameters", Map.of("listId", "123"),
                "body", "{\"name\":\"tarefa 1\"}"
        );

        Map<String, Object> response = lambda.handleRequest(input, null);

        assertEquals(500, response.get("statusCode"));
    }
}
