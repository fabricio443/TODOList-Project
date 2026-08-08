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
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.fasterxml.jackson.databind.ObjectMapper;

class DeleteTaskItemLambdaTest {

    @Test
    void shouldDeleteTaskItemAndReturnNoContent() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        DeleteTaskItemLambda lambda = new DeleteTaskItemLambda(dynamoDB, new ObjectMapper(), "todo-list");

        GetItemResult getItemResult = new GetItemResult();
        getItemResult.setItem(Map.of(
                "PK", new AttributeValue("LIST#123"),
                "SK", new AttributeValue("TASK#t1"),
                "name", new AttributeValue("old")
        ));
        when(dynamoDB.getItem(any(GetItemRequest.class))).thenReturn(getItemResult);

        Map<String, Object> input = Map.of(
                "pathParameters", Map.of("listId", "123", "taskId", "t1")
        );

        Map<String, Object> response = lambda.handleRequest(input, null);

        assertEquals(204, response.get("statusCode"));
        verify(dynamoDB).deleteItem(any(DeleteItemRequest.class));
    }

    @Test
    void shouldReturn400WhenPathParamsMissing() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        DeleteTaskItemLambda lambda = new DeleteTaskItemLambda(dynamoDB, new ObjectMapper(), "todo-list");

        Map<String, Object> input = Map.of("pathParameters", Map.of("listId", "123"));

        Map<String, Object> response = lambda.handleRequest(input, null);

        assertEquals(400, response.get("statusCode"));
    }

    @Test
    void shouldReturn404WhenItemNotFound() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        DeleteTaskItemLambda lambda = new DeleteTaskItemLambda(dynamoDB, new ObjectMapper(), "todo-list");

        GetItemResult getItemResult = new GetItemResult();
        getItemResult.setItem(null);
        when(dynamoDB.getItem(any(GetItemRequest.class))).thenReturn(getItemResult);

        Map<String, Object> input = Map.of(
                "pathParameters", Map.of("listId", "123", "taskId", "t1")
        );

        Map<String, Object> response = lambda.handleRequest(input, null);

        assertEquals(404, response.get("statusCode"));
    }

    @Test
    void shouldReturn500OnDynamoError() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        DeleteTaskItemLambda lambda = new DeleteTaskItemLambda(dynamoDB, new ObjectMapper(), "todo-list");
        GetItemResult getItemResult = new GetItemResult();
        getItemResult.setItem(Map.of(
                "PK", new AttributeValue("LIST#123"),
                "SK", new AttributeValue("TASK#t1")
        ));
        when(dynamoDB.getItem(any(GetItemRequest.class))).thenReturn(getItemResult);
        Mockito.doThrow(new RuntimeException("boom")).when(dynamoDB).deleteItem(any(DeleteItemRequest.class));

        Map<String, Object> input = Map.of(
                "pathParameters", Map.of("listId", "123", "taskId", "t1")
        );

        Map<String, Object> response = lambda.handleRequest(input, null);

        assertEquals(500, response.get("statusCode"));
    }
}
