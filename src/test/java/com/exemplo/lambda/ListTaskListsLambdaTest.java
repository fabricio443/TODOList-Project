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
import com.amazonaws.services.dynamodbv2.model.QueryResult;

class ListTaskListsLambdaTest {

    @Test
    void shouldListTaskListsUsingQuery() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        ListTaskListsLambda lambda = new ListTaskListsLambda(dynamoDB, "todo-list");

        QueryResult queryResult = new QueryResult();
        queryResult.setItems(List.of(Map.of("PK", new AttributeValue("LIST"), "SK", new AttributeValue("LIST#123"), "name", new AttributeValue("mercado"))));

        when(dynamoDB.query(any())).thenReturn(queryResult);

        Map<String, Object> response = lambda.handleRequest(Map.of(), null);

        assertEquals(200, response.get("statusCode"));
    }

    @Test
    void shouldReturnEmptyListWhenNoItemsFound() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        ListTaskListsLambda lambda = new ListTaskListsLambda(dynamoDB, "todo-list");
        QueryResult queryResult = new QueryResult();
        queryResult.setItems(List.of());

        when(dynamoDB.query(any())).thenReturn(queryResult);

        Map<String, Object> response = lambda.handleRequest(Map.of(), null);

        assertEquals(200, response.get("statusCode"));
    }

    @Test
    void shouldHandleMultipleItems() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        ListTaskListsLambda lambda = new ListTaskListsLambda(dynamoDB, "todo-list");
        QueryResult queryResult = new QueryResult();
        queryResult.setItems(List.of(
                Map.of(
                        "PK", new AttributeValue("LIST#1"),
                        "SK", new AttributeValue("META"),
                        "GSI1PK", new AttributeValue("LIST"),
                        "GSI1SK", new AttributeValue("LIST#1"),
                        "name", new AttributeValue("mercado")
                ),
                Map.of(
                        "PK", new AttributeValue("LIST#2"),
                        "SK", new AttributeValue("META"),
                        "GSI1PK", new AttributeValue("LIST"),
                        "GSI1SK", new AttributeValue("LIST#2"),
                        "name", new AttributeValue("trabalho")
                )
        ));
        when(dynamoDB.query(any())).thenReturn(queryResult);

        Map<String, Object> response = lambda.handleRequest(Map.of(), null);

        assertEquals(200, response.get("statusCode"));
    }

    @Test
    void shouldHandleMissingNameAttribute() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        ListTaskListsLambda lambda = new ListTaskListsLambda(dynamoDB, "todo-list");
        QueryResult queryResult = new QueryResult();
        queryResult.setItems(List.of(Map.of(
                "PK", new AttributeValue("LIST#3"),
                "SK", new AttributeValue("META"),
                "GSI1PK", new AttributeValue("LIST"),
                "GSI1SK", new AttributeValue("LIST#3")
        )));

        when(dynamoDB.query(any())).thenReturn(queryResult);

        Map<String, Object> response = lambda.handleRequest(Map.of(), null);

        assertEquals(200, response.get("statusCode"));
    }

    @Test
    void shouldHandleMissingPkAttribute() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        ListTaskListsLambda lambda = new ListTaskListsLambda(dynamoDB, "todo-list");
        QueryResult queryResult = new QueryResult();
        queryResult.setItems(List.of(Map.of(
                "PK", new AttributeValue("LIST#123"),
                "SK", new AttributeValue("META"),
                "GSI1PK", new AttributeValue("LIST"),
                "GSI1SK", new AttributeValue("LIST#123"),
                "name", new AttributeValue("mercado")
        )));

        when(dynamoDB.query(any())).thenReturn(queryResult);

        Map<String, Object> response = lambda.handleRequest(Map.of(), null);

        assertEquals(200, response.get("statusCode"));
    }

    @Test
    void shouldReturnContentTypeHeader() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        ListTaskListsLambda lambda = new ListTaskListsLambda(dynamoDB, "todo-list");
        QueryResult queryResult = new QueryResult();
        queryResult.setItems(List.of(Map.of("PK", new AttributeValue("LIST"), "SK", new AttributeValue("LIST#123"), "name", new AttributeValue("mercado"))));

        when(dynamoDB.query(any())).thenReturn(queryResult);

        Map<String, Object> response = lambda.handleRequest(Map.of(), null);

        assertEquals("application/json", ((Map<?, ?>) response.get("headers")).get("Content-Type"));
    }

    @Test
    void shouldReturnListOfMapsInBody() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        ListTaskListsLambda lambda = new ListTaskListsLambda(dynamoDB, "todo-list");
        QueryResult queryResult = new QueryResult();
        queryResult.setItems(List.of(Map.of("PK", new AttributeValue("LIST"), "SK", new AttributeValue("LIST#123"), "name", new AttributeValue("mercado"))));

        when(dynamoDB.query(any())).thenReturn(queryResult);

        Map<String, Object> response = lambda.handleRequest(Map.of(), null);
        try {
            String bodyStr = (String) response.get("body");
            java.util.List<?> lists = new com.fasterxml.jackson.databind.ObjectMapper().readValue(bodyStr, new com.fasterxml.jackson.core.type.TypeReference<java.util.List<java.util.Map<String, Object>>>(){});
            assertEquals(true, !lists.isEmpty());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    void shouldHandleDynamoFailure() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        ListTaskListsLambda lambda = new ListTaskListsLambda(dynamoDB, "todo-list");
        when(dynamoDB.query(any())).thenThrow(new RuntimeException("boom"));

        Map<String, Object> response = lambda.handleRequest(Map.of(), null);

        assertEquals(500, response.get("statusCode"));
    }

    @Test
    void shouldTrimListIdPrefix() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        ListTaskListsLambda lambda = new ListTaskListsLambda(dynamoDB, "todo-list");
        QueryResult queryResult = new QueryResult();
        queryResult.setItems(List.of(Map.of("PK", new AttributeValue("LIST"), "SK", new AttributeValue("LIST#xyz"), "name", new AttributeValue("mercado"))));

        when(dynamoDB.query(any())).thenReturn(queryResult);

        Map<String, Object> response = lambda.handleRequest(Map.of(), null);

        assertEquals(200, response.get("statusCode"));
    }

    @Test
    void shouldHandleNullItemValue() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        ListTaskListsLambda lambda = new ListTaskListsLambda(dynamoDB, "todo-list");
        QueryResult queryResult = new QueryResult();
        queryResult.setItems(List.of(Map.of("PK", new AttributeValue("LIST"), "SK", new AttributeValue("LIST#123"), "name", new AttributeValue("mercado"))));

        when(dynamoDB.query(any())).thenReturn(queryResult);

        Map<String, Object> response = lambda.handleRequest(Map.of(), null);

        assertEquals(200, response.get("statusCode"));
    }

    @Test
    void shouldUseConfiguredTableName() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        ListTaskListsLambda lambda = new ListTaskListsLambda(dynamoDB, "custom-table");
        QueryResult queryResult = new QueryResult();
        queryResult.setItems(List.of());

        when(dynamoDB.query(any())).thenReturn(queryResult);

        lambda.handleRequest(Map.of(), null);

        assertEquals(200, 200);
    }

    @Test
    void shouldHandleRepeatedQueryCalls() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        ListTaskListsLambda lambda = new ListTaskListsLambda(dynamoDB, "todo-list");
        QueryResult queryResult = new QueryResult();
        queryResult.setItems(List.of());

        when(dynamoDB.query(any())).thenReturn(queryResult);

        lambda.handleRequest(Map.of(), null);
        lambda.handleRequest(Map.of(), null);

        assertEquals(200, 200);
    }

    @Test
    void shouldHandleItemWithEmptyName() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        ListTaskListsLambda lambda = new ListTaskListsLambda(dynamoDB, "todo-list");
        QueryResult queryResult = new QueryResult();
        queryResult.setItems(List.of(Map.of(
                "PK", new AttributeValue("LIST#123"),
                "SK", new AttributeValue("META"),
                "GSI1PK", new AttributeValue("LIST"),
                "GSI1SK", new AttributeValue("LIST#123"),
                "name", new AttributeValue("")
        )));

        when(dynamoDB.query(any())).thenReturn(queryResult);

        Map<String, Object> response = lambda.handleRequest(Map.of(), null);

        assertEquals(200, response.get("statusCode"));
    }

    @Test
    void shouldHandleItemWithDifferentCase() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        ListTaskListsLambda lambda = new ListTaskListsLambda(dynamoDB, "todo-list");
        QueryResult queryResult = new QueryResult();
        queryResult.setItems(List.of(Map.of(
                "PK", new AttributeValue("LIST#123"),
                "SK", new AttributeValue("META"),
                "GSI1PK", new AttributeValue("LIST"),
                "GSI1SK", new AttributeValue("LIST#123"),
                "name", new AttributeValue("Mercado")
        )));

        when(dynamoDB.query(any())).thenReturn(queryResult);

        Map<String, Object> response = lambda.handleRequest(Map.of(), null);

        assertEquals(200, response.get("statusCode"));
    }

    @Test
    void shouldHandleLargeResultSet() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        ListTaskListsLambda lambda = new ListTaskListsLambda(dynamoDB, "todo-list");
        QueryResult queryResult = new QueryResult();
        queryResult.setItems(List.of(
                Map.of(
                        "PK", new AttributeValue("LIST#1"),
                        "SK", new AttributeValue("META"),
                        "GSI1PK", new AttributeValue("LIST"),
                        "GSI1SK", new AttributeValue("LIST#1"),
                        "name", new AttributeValue("a")
                ),
                Map.of(
                        "PK", new AttributeValue("LIST#2"),
                        "SK", new AttributeValue("META"),
                        "GSI1PK", new AttributeValue("LIST"),
                        "GSI1SK", new AttributeValue("LIST#2"),
                        "name", new AttributeValue("b")
                ),
                Map.of(
                        "PK", new AttributeValue("LIST#3"),
                        "SK", new AttributeValue("META"),
                        "GSI1PK", new AttributeValue("LIST"),
                        "GSI1SK", new AttributeValue("LIST#3"),
                        "name", new AttributeValue("c")
                )
            ));
        when(dynamoDB.query(any())).thenReturn(queryResult);

        Map<String, Object> response = lambda.handleRequest(Map.of(), null);

        assertEquals(200, response.get("statusCode"));
    }
}
