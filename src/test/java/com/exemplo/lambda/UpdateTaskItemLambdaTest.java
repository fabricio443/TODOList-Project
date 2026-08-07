package com.exemplo.lambda;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UpdateTaskItemLambdaTest {

    @Test
    void shouldUpdateTaskItemAndReturnOkResponse() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        UpdateTaskItemLambda lambda = new UpdateTaskItemLambda(dynamoDB, new ObjectMapper(), "todo-list");

        GetItemResult getItemResult = new GetItemResult();
        getItemResult.setItem(Map.of(
                "PK", new AttributeValue("LIST#123"),
                "SK", new AttributeValue("TASK#t1"),
                "name", new AttributeValue("old"),
                "status", new AttributeValue("PENDING"),
                "createdAt", new AttributeValue("2024-01-01T00:00:00Z")
        ));
        when(dynamoDB.getItem(any(GetItemRequest.class))).thenReturn(getItemResult);

        Map<String, Object> input = Map.of(
                "pathParameters", Map.of("listId", "123", "taskId", "t1"),
                "body", "{\"name\":\"novo\"}"
        );

        Map<String, Object> response = lambda.handleRequest(input, null);

        assertEquals(200, response.get("statusCode"));
        verify(dynamoDB).updateItem(any(UpdateItemRequest.class));
    }

    @Test
    void shouldRejectMissingListId() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        UpdateTaskItemLambda lambda = new UpdateTaskItemLambda(dynamoDB, new ObjectMapper(), "todo-list");

        Map<String, Object> input = Map.of(
                "pathParameters", Map.of("taskId", "t1"),
                "body", "{\"name\":\"novo\"}"
        );

        Map<String, Object> response = lambda.handleRequest(input, null);

        assertEquals(400, response.get("statusCode"));
    }

    @Test
    void shouldRejectMissingTaskId() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        UpdateTaskItemLambda lambda = new UpdateTaskItemLambda(dynamoDB, new ObjectMapper(), "todo-list");

        Map<String, Object> input = Map.of(
                "pathParameters", Map.of("listId", "123"),
                "body", "{\"name\":\"novo\"}"
        );

        Map<String, Object> response = lambda.handleRequest(input, null);

        assertEquals(400, response.get("statusCode"));
    }

    @Test
    void shouldRejectMissingName() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        UpdateTaskItemLambda lambda = new UpdateTaskItemLambda(dynamoDB, new ObjectMapper(), "todo-list");

        Map<String, Object> input = Map.of(
                "pathParameters", Map.of("listId", "123", "taskId", "t1"),
                "body", "{\"other\":\"x\"}"
        );

        Map<String, Object> response = lambda.handleRequest(input, null);

        assertEquals(400, response.get("statusCode"));
    }

    @Test
    void shouldReturn404WhenItemNotFound() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        UpdateTaskItemLambda lambda = new UpdateTaskItemLambda(dynamoDB, new ObjectMapper(), "todo-list");

        GetItemResult getItemResult = new GetItemResult();
        getItemResult.setItem(null);
        when(dynamoDB.getItem(any(GetItemRequest.class))).thenReturn(getItemResult);

        Map<String, Object> input = Map.of(
                "pathParameters", Map.of("listId", "123", "taskId", "t1"),
                "body", "{\"name\":\"novo\"}"
        );

        Map<String, Object> response = lambda.handleRequest(input, null);

        assertEquals(404, response.get("statusCode"));
    }

    @Test
    void shouldHandleDynamoFailure() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        UpdateTaskItemLambda lambda = new UpdateTaskItemLambda(dynamoDB, new ObjectMapper(), "todo-list");

        GetItemResult getItemResult = new GetItemResult();
        getItemResult.setItem(Map.of(
                "PK", new AttributeValue("LIST#123"),
                "SK", new AttributeValue("TASK#t1"),
                "name", new AttributeValue("old")
        ));
        when(dynamoDB.getItem(any(GetItemRequest.class))).thenReturn(getItemResult);
        Mockito.doThrow(new RuntimeException("boom")).when(dynamoDB).updateItem(any(UpdateItemRequest.class));

        Map<String, Object> input = Map.of(
                "pathParameters", Map.of("listId", "123", "taskId", "t1"),
                "body", "{\"name\":\"novo\"}"
        );

        Map<String, Object> response = lambda.handleRequest(input, null);

        assertEquals(500, response.get("statusCode"));
    }
}
