package com.exemplo.lambda;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mockito;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

class GenerateTaskListReportLambdaTest {

    @Test
    void shouldGenerateCsvAndUploadToS3() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        AmazonS3 s3 = Mockito.mock(AmazonS3.class);

        GenerateTaskListReportLambda lambda = new GenerateTaskListReportLambda(dynamoDB, s3, new ObjectMapper(), "todo-list", "reports-bucket");

        QueryResult qr = new QueryResult();
        qr.setItems(List.of(Map.of(
                "PK", new AttributeValue("LIST#123"),
                "SK", new AttributeValue("TASK#t1"),
                "taskId", new AttributeValue("t1"),
                "name", new AttributeValue("Task One"),
                "status", new AttributeValue("DONE"),
                "createdAt", new AttributeValue("2026-08-01T00:00:00Z")
        )));

        when(dynamoDB.query(any(QueryRequest.class))).thenReturn(qr);

        Map<String, Object> input = Map.of("pathParameters", Map.of("listId", "123"));

        Map<String, Object> response = lambda.handleRequest(input, null);

        assertEquals(200, response.get("statusCode"));
        verify(s3).putObject(any(PutObjectRequest.class));
    }

    @Test
    void shouldReturn400WhenPathParamsMissing() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        AmazonS3 s3 = Mockito.mock(AmazonS3.class);
        GenerateTaskListReportLambda lambda = new GenerateTaskListReportLambda(dynamoDB, s3, new ObjectMapper(), "todo-list", "reports-bucket");

        Map<String, Object> input = Map.of("pathParameters", Map.of());

        Map<String, Object> response = lambda.handleRequest(input, null);

        assertEquals(400, response.get("statusCode"));
    }

    @Test
    void shouldReturn500WhenS3Fails() {
        AmazonDynamoDB dynamoDB = Mockito.mock(AmazonDynamoDB.class);
        AmazonS3 s3 = Mockito.mock(AmazonS3.class);

        QueryResult qr = new QueryResult();
        qr.setItems(List.of(Map.of(
                "PK", new AttributeValue("LIST#123"),
                "SK", new AttributeValue("TASK#t1"),
                "taskId", new AttributeValue("t1")
        )));
        when(dynamoDB.query(any(QueryRequest.class))).thenReturn(qr);

        Mockito.doThrow(new RuntimeException("s3 error")).when(s3).putObject(any(PutObjectRequest.class));

        GenerateTaskListReportLambda lambda = new GenerateTaskListReportLambda(dynamoDB, s3, new ObjectMapper(), "todo-list", "reports-bucket");
        Map<String, Object> input = Map.of("pathParameters", Map.of("listId", "123"));

        Map<String, Object> response = lambda.handleRequest(input, null);

        assertEquals(500, response.get("statusCode"));
    }
}
