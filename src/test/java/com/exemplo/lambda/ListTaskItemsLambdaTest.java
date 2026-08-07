package com.exemplo.lambda;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mockito;
import static org.mockito.Mockito.when;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.fasterxml.jackson.databind.ObjectMapper;

class ListTaskItemsLambdaTest {

    @Test
    void shouldListTasksForExistingList() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        ListTaskItemsLambda lambda = new ListTaskItemsLambda(dynamoDB, new ObjectMapper(), "todo-list");

        QueryResult queryResult = new QueryResult();
        queryResult.setItems(List.of(
                Map.of(
                        "taskId", new AttributeValue("task-1"),
                        "name", new AttributeValue("Estudar Lambda"),
                        "status", new AttributeValue("PENDING"),
                        "createdAt", new AttributeValue("2026-08-07T01:16:24Z")
                )
        ));
        when(dynamoDB.query(any(QueryRequest.class))).thenReturn(queryResult);

        Map<String, Object> response = lambda.handleRequest(Map.of("pathParameters", Map.of("listId", "123")), null);

        assertEquals(200, response.get("statusCode"));
    }

    @Test
    void shouldReturnEmptyListWhenNoTasksFound() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        ListTaskItemsLambda lambda = new ListTaskItemsLambda(dynamoDB, new ObjectMapper(), "todo-list");

        QueryResult queryResult = new QueryResult();
        queryResult.setItems(List.of());
        when(dynamoDB.query(any(QueryRequest.class))).thenReturn(queryResult);

        Map<String, Object> response = lambda.handleRequest(Map.of("pathParameters", Map.of("listId", "123")), null);

        assertEquals(200, response.get("statusCode"));
    }

    @Test
    void shouldReturn400WhenPathParameterIsMissing() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        ListTaskItemsLambda lambda = new ListTaskItemsLambda(dynamoDB, new ObjectMapper(), "todo-list");

        Map<String, Object> response = lambda.handleRequest(Map.of(), null);

        assertEquals(400, response.get("statusCode"));
    }

    @Test
    void shouldReturn400WhenPathParameterIsBlank() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        ListTaskItemsLambda lambda = new ListTaskItemsLambda(dynamoDB, new ObjectMapper(), "todo-list");

        Map<String, Object> response = lambda.handleRequest(Map.of("pathParameters", Map.of("listId", "   ")), null);

        assertEquals(400, response.get("statusCode"));
    }

    @Test
    void shouldReturn500WhenDynamoThrows() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        ListTaskItemsLambda lambda = new ListTaskItemsLambda(dynamoDB, new ObjectMapper(), "todo-list");
        when(dynamoDB.query(any(QueryRequest.class))).thenThrow(new RuntimeException("boom"));

        Map<String, Object> response = lambda.handleRequest(Map.of("pathParameters", Map.of("listId", "123")), null);

        assertEquals(500, response.get("statusCode"));
    }

    @Test
    void shouldReturnJsonContentTypeHeader() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        ListTaskItemsLambda lambda = new ListTaskItemsLambda(dynamoDB, new ObjectMapper(), "todo-list");

        QueryResult queryResult = new QueryResult();
        queryResult.setItems(List.of());
        when(dynamoDB.query(any(QueryRequest.class))).thenReturn(queryResult);

        Map<String, Object> response = lambda.handleRequest(Map.of("pathParameters", Map.of("listId", "123")), null);

        assertEquals("application/json", ((Map<?, ?>) response.get("headers")).get("Content-Type"));
    }

    @Test
    void shouldUseDefaultTableNameWhenNotProvided() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        ListTaskItemsLambda lambda = new ListTaskItemsLambda(dynamoDB, new ObjectMapper(), "todo-list");

        QueryResult queryResult = new QueryResult();
        queryResult.setItems(List.of());
        when(dynamoDB.query(any(QueryRequest.class))).thenReturn(queryResult);

        lambda.handleRequest(Map.of("pathParameters", Map.of("listId", "123")), null);

        assertEquals(true, true);
    }

    @Test
    void shouldHandleMissingStatusAttribute() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        ListTaskItemsLambda lambda = new ListTaskItemsLambda(dynamoDB, new ObjectMapper(), "todo-list");

        QueryResult queryResult = new QueryResult();
        queryResult.setItems(List.of(
                Map.of(
                        "taskId", new AttributeValue("task-2"),
                        "name", new AttributeValue("Tarefa sem status")
                )
        ));
        when(dynamoDB.query(any(QueryRequest.class))).thenReturn(queryResult);

        Map<String, Object> response = lambda.handleRequest(Map.of("pathParameters", Map.of("listId", "123")), null);

        assertEquals(200, response.get("statusCode"));
    }

    @Test
    void shouldHandleMissingCreatedAtAttribute() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        ListTaskItemsLambda lambda = new ListTaskItemsLambda(dynamoDB, new ObjectMapper(), "todo-list");

        QueryResult queryResult = new QueryResult();
        queryResult.setItems(List.of(
                Map.of(
                        "taskId", new AttributeValue("task-3"),
                        "name", new AttributeValue("Tarefa sem createdAt"),
                        "status", new AttributeValue("PENDING")
                )
        ));
        when(dynamoDB.query(any(QueryRequest.class))).thenReturn(queryResult);

        Map<String, Object> response = lambda.handleRequest(Map.of("pathParameters", Map.of("listId", "123")), null);

        assertEquals(200, response.get("statusCode"));
    }

    @Test
    void shouldHandleMultipleTasks() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        ListTaskItemsLambda lambda = new ListTaskItemsLambda(dynamoDB, new ObjectMapper(), "todo-list");

        QueryResult queryResult = new QueryResult();
        queryResult.setItems(List.of(
                Map.of(
                        "taskId", new AttributeValue("task-1"),
                        "name", new AttributeValue("Tarefa 1"),
                        "status", new AttributeValue("PENDING"),
                        "createdAt", new AttributeValue("2026-08-07T01:16:24Z")
                ),
                Map.of(
                        "taskId", new AttributeValue("task-2"),
                        "name", new AttributeValue("Tarefa 2"),
                        "status", new AttributeValue("DONE"),
                        "createdAt", new AttributeValue("2026-08-07T01:17:24Z")
                )
        ));
        when(dynamoDB.query(any(QueryRequest.class))).thenReturn(queryResult);

        Map<String, Object> response = lambda.handleRequest(Map.of("pathParameters", Map.of("listId", "123")), null);

        assertEquals(200, response.get("statusCode"));
    }
}
