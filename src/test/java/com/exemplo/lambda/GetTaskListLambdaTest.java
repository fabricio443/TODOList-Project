package com.exemplo.lambda;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mockito;
import static org.mockito.Mockito.when;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.fasterxml.jackson.databind.ObjectMapper;

class GetTaskListLambdaTest {

    @Test
    void shouldReturnTaskListWhenExists() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        GetTaskListLambda lambda = new GetTaskListLambda(dynamoDB, new ObjectMapper(), "todo-list");

        GetItemResult getItemResult = new GetItemResult();
        getItemResult.setItem(Map.of(
                "PK", new AttributeValue("LIST#123"),
                "SK", new AttributeValue("META"),
                "name", new AttributeValue("Lista de mercado")
        ));
        when(dynamoDB.getItem(any(GetItemRequest.class))).thenReturn(getItemResult);

        Map<String, Object> input = Map.of(
                "pathParameters", Map.of("listId", "123")
        );

        Map<String, Object> response = lambda.handleRequest(input, null);

        assertEquals(200, response.get("statusCode"));
    }

    @Test
    void shouldReturn404WhenTaskListDoesNotExist() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        GetTaskListLambda lambda = new GetTaskListLambda(dynamoDB, new ObjectMapper(), "todo-list");

        GetItemResult getItemResult = new GetItemResult();
        getItemResult.setItem(null);
        when(dynamoDB.getItem(any(GetItemRequest.class))).thenReturn(getItemResult);

        Map<String, Object> input = Map.of(
                "pathParameters", Map.of("listId", "missing")
        );

        Map<String, Object> response = lambda.handleRequest(input, null);

        assertEquals(404, response.get("statusCode"));
    }

    @Test
    void shouldReturn400WhenListIdIsMissing() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        GetTaskListLambda lambda = new GetTaskListLambda(dynamoDB, new ObjectMapper(), "todo-list");

        Map<String, Object> input = Map.of(
                "pathParameters", Map.of()
        );

        Map<String, Object> response = lambda.handleRequest(input, null);

        assertEquals(400, response.get("statusCode"));
    }

    @Test
    void shouldReturn400WhenListIdIsBlank() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        GetTaskListLambda lambda = new GetTaskListLambda(dynamoDB, new ObjectMapper(), "todo-list");

        Map<String, Object> input = Map.of(
                "pathParameters", Map.of("listId", "   ")
        );

        Map<String, Object> response = lambda.handleRequest(input, null);

        assertEquals(400, response.get("statusCode"));
    }

    @Test
    void shouldReturn500OnDynamoError() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        GetTaskListLambda lambda = new GetTaskListLambda(dynamoDB, new ObjectMapper(), "todo-list");
        Mockito.when(dynamoDB.getItem(any(GetItemRequest.class))).thenThrow(new RuntimeException("boom"));

        Map<String, Object> input = Map.of(
                "pathParameters", Map.of("listId", "123")
        );

        Map<String, Object> response = lambda.handleRequest(input, null);

        assertEquals(500, response.get("statusCode"));
    }
}
