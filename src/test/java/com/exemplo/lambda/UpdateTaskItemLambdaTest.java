package com.exemplo.lambda;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mockito;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

class UpdateTaskItemLambdaTest {

    @Test
    void shouldUpdateTaskItemAndReturnOkResponse() throws Exception {
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
                "body", "{\"name\":\"novo\", \"status\": \"COMPLETED\"}"
        );

        Map<String, Object> response = lambda.handleRequest(input, null);

        assertEquals(200, response.get("statusCode"));

        ArgumentCaptor<UpdateItemRequest> captor = ArgumentCaptor.forClass(UpdateItemRequest.class);
        verify(dynamoDB).updateItem(captor.capture());

        UpdateItemRequest capturedRequest = captor.getValue();
        assertEquals("SET #name = :name, #status = :status", capturedRequest.getUpdateExpression());
        assertEquals("name", capturedRequest.getExpressionAttributeNames().get("#name"));
        assertEquals("status", capturedRequest.getExpressionAttributeNames().get("#status"));
        assertEquals("novo", capturedRequest.getExpressionAttributeValues().get(":name").getS());
        assertEquals("COMPLETED", capturedRequest.getExpressionAttributeValues().get(":status").getS());

        Map<String, Object> responseBody = new ObjectMapper().readValue((String) response.get("body"), Map.class);
        assertEquals("novo", responseBody.get("name"));
        assertEquals("COMPLETED", responseBody.get("status"));
    }

    @Test
    void shouldReturnUpdatedStatusWhenStatusIsProvided() throws Exception {
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
                "body", "{\"name\":\"novo\", \"status\": \"COMPLETED\"}"
        );

        Map<String, Object> response = lambda.handleRequest(input, null);

        assertEquals(200, response.get("statusCode"));
        Map<String, Object> responseBody = new ObjectMapper().readValue((String) response.get("body"), Map.class);
        assertEquals("COMPLETED", responseBody.get("status"));
    }

    @Test
    void shouldPreserveExistingStatusWhenStatusNotProvided() throws Exception {
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

        ArgumentCaptor<UpdateItemRequest> captor = ArgumentCaptor.forClass(UpdateItemRequest.class);
        verify(dynamoDB).updateItem(captor.capture());

        UpdateItemRequest capturedRequest = captor.getValue();
        assertEquals("SET #name = :name", capturedRequest.getUpdateExpression());
        assertEquals("name", capturedRequest.getExpressionAttributeNames().get("#name"));
        assertEquals("novo", capturedRequest.getExpressionAttributeValues().get(":name").getS());
        assertEquals(1, capturedRequest.getExpressionAttributeNames().size());
        assertEquals(1, capturedRequest.getExpressionAttributeValues().size());

        Map<String, Object> responseBody = new ObjectMapper().readValue((String) response.get("body"), Map.class);
        assertEquals("PENDING", responseBody.get("status"));
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
