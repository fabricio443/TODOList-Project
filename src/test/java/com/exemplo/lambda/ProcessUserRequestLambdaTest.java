package com.exemplo.lambda;

import com.amazonaws.services.dynamodbv2.AbstractAmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.s3.AbstractAmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.simpleemail.AbstractAmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.amazonaws.services.simpleemail.model.SendEmailResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ProcessUserRequestLambdaTest {

    private static QueryResult createGsi2QueryResult() {
        QueryResult result = new QueryResult();
        result.setCount(1);
        result.setItems(List.of(Map.of("PK", new AttributeValue("LIST#123"))));
        return result;
    }

    private static QueryResult createTaskQueryResult() {
        QueryResult result = new QueryResult();
        result.setItems(List.of(Map.of(
                "taskId", new AttributeValue("t1"),
                "name", new AttributeValue("Task 1"),
                "status", new AttributeValue("DONE"),
                "createdAt", new AttributeValue("2026-08-01T00:00:00Z")
        )));
        return result;
    }

    private static class FakeDynamoDB extends AbstractAmazonDynamoDB {
        private final QueryResult gsi2Result;
        private final QueryResult taskResult;

        FakeDynamoDB(QueryResult gsi2Result, QueryResult taskResult) {
            this.gsi2Result = gsi2Result;
            this.taskResult = taskResult;
        }

        @Override
        public QueryResult query(QueryRequest queryRequest) {
            if (queryRequest != null && "GSI2".equals(queryRequest.getIndexName())) {
                return gsi2Result;
            }
            return taskResult;
        }
    }

    private static class FakeS3 extends AbstractAmazonS3 {
        boolean putObjectCalled;

        @Override
        public PutObjectResult putObject(PutObjectRequest putObjectRequest) {
            putObjectCalled = true;
            return null;
        }

        @Override
        public PutObjectResult putObject(String bucketName, String key, InputStream input, ObjectMetadata metadata) {
            putObjectCalled = true;
            return null;
        }
    }

    private static class FakeSES extends AbstractAmazonSimpleEmailService {
        boolean sendEmailCalled;
        SendEmailResult sendEmailResult;
        RuntimeException failure;

        void setSendEmailResult(SendEmailResult sendEmailResult) {
            this.sendEmailResult = sendEmailResult;
        }

        void setFailure(RuntimeException failure) {
            this.failure = failure;
        }

        @Override
        public SendEmailResult sendEmail(SendEmailRequest sendEmailRequest) {
            sendEmailCalled = true;
            if (failure != null) {
                throw failure;
            }
            return sendEmailResult;
        }
    }

    @Test
    void shouldProcessSqsRecordAndSendEmail() {
        FakeDynamoDB dynamoDB = new FakeDynamoDB(createGsi2QueryResult(), createTaskQueryResult());
        FakeS3 s3 = new FakeS3();
        FakeSES ses = new FakeSES();
        SendEmailResult emailResult = new SendEmailResult().withMessageId("email-1");
        ses.setSendEmailResult(emailResult);

        ProcessUserRequestLambda lambda = new ProcessUserRequestLambda(
                dynamoDB,
                s3,
                ses,
                new ObjectMapper(),
                "todo-list",
                "reports-bucket",
                "noreply@example.com"
        );

        Map<String, Object> record = Map.of(
                "body", "{\"userSub\":\"user-123\",\"email\":\"user@example.com\",\"request\":{\"listId\":\"123\"}}"
        );

        Map<String, Object> response = lambda.handleRequest(Map.of("Records", List.of(record)), null);

        assertEquals(200, response.get("statusCode"));
        assertTrue(s3.putObjectCalled);
        assertTrue(ses.sendEmailCalled);
    }

    @Test
    void shouldThrowRuntimeExceptionWhenSesSendEmailReturnsNull() {
        FakeDynamoDB dynamoDB = new FakeDynamoDB(createGsi2QueryResult(), createTaskQueryResult());
        FakeS3 s3 = new FakeS3();
        FakeSES ses = new FakeSES();
        ses.setSendEmailResult(null);

        ProcessUserRequestLambda lambda = new ProcessUserRequestLambda(
                dynamoDB,
                s3,
                ses,
                new ObjectMapper(),
                "todo-list",
                "reports-bucket",
                "noreply@example.com"
        );

        Map<String, Object> record = Map.of(
                "body", "{\"userSub\":\"user-123\",\"email\":\"user@example.com\",\"request\":{\"listId\":\"123\"}}"
        );

        assertThrows(RuntimeException.class, () -> lambda.handleRequest(Map.of("Records", List.of(record)), null));
        assertTrue(ses.sendEmailCalled);
    }

    @Test
    void shouldThrowRuntimeExceptionWhenSesMessageIdIsNull() {
        FakeDynamoDB dynamoDB = new FakeDynamoDB(createGsi2QueryResult(), createTaskQueryResult());
        FakeS3 s3 = new FakeS3();
        FakeSES ses = new FakeSES();
        SendEmailResult emailResult = new SendEmailResult().withMessageId(null);
        ses.setSendEmailResult(emailResult);

        ProcessUserRequestLambda lambda = new ProcessUserRequestLambda(
                dynamoDB,
                s3,
                ses,
                new ObjectMapper(),
                "todo-list",
                "reports-bucket",
                "noreply@example.com"
        );

        Map<String, Object> record = Map.of(
                "body", "{\"userSub\":\"user-123\",\"email\":\"user@example.com\",\"request\":{\"listId\":\"123\"}}"
        );

        assertThrows(RuntimeException.class, () -> lambda.handleRequest(Map.of("Records", List.of(record)), null));
        assertTrue(ses.sendEmailCalled);
    }

    @Test
    void shouldThrowRuntimeExceptionWhenSesMessageIdIsBlank() {
        FakeDynamoDB dynamoDB = new FakeDynamoDB(createGsi2QueryResult(), createTaskQueryResult());
        FakeS3 s3 = new FakeS3();
        FakeSES ses = new FakeSES();
        SendEmailResult emailResult = new SendEmailResult().withMessageId("");
        ses.setSendEmailResult(emailResult);

        ProcessUserRequestLambda lambda = new ProcessUserRequestLambda(
                dynamoDB,
                s3,
                ses,
                new ObjectMapper(),
                "todo-list",
                "reports-bucket",
                "noreply@example.com"
        );

        Map<String, Object> record = Map.of(
                "body", "{\"userSub\":\"user-123\",\"email\":\"user@example.com\",\"request\":{\"listId\":\"123\"}}"
        );

        assertThrows(RuntimeException.class, () -> lambda.handleRequest(Map.of("Records", List.of(record)), null));
        assertTrue(ses.sendEmailCalled);
    }

    @Test
    void shouldThrowRuntimeExceptionWhenEmailIsInvalid() {
        FakeDynamoDB dynamoDB = new FakeDynamoDB(createGsi2QueryResult(), createTaskQueryResult());
        FakeS3 s3 = new FakeS3();
        FakeSES ses = new FakeSES();

        ProcessUserRequestLambda lambda = new ProcessUserRequestLambda(
                dynamoDB,
                s3,
                ses,
                new ObjectMapper(),
                "todo-list",
                "reports-bucket",
                "noreply@example.com"
        );

        Map<String, Object> record = Map.of(
                "body", "{\"userSub\":\"user-123\",\"email\":\"userexample.com\",\"request\":{\"listId\":\"123\"}}"
        );

        assertThrows(RuntimeException.class, () -> lambda.handleRequest(Map.of("Records", List.of(record)), null));
        assertFalse(ses.sendEmailCalled);
    }

    @Test
    void shouldThrowRuntimeExceptionWhenFromEmailIsBlank() {
        FakeDynamoDB dynamoDB = new FakeDynamoDB(createGsi2QueryResult(), createTaskQueryResult());
        FakeS3 s3 = new FakeS3();
        FakeSES ses = new FakeSES();

        ProcessUserRequestLambda lambda = new ProcessUserRequestLambda(
                dynamoDB,
                s3,
                ses,
                new ObjectMapper(),
                "todo-list",
                "reports-bucket",
                ""
        );

        Map<String, Object> record = Map.of(
                "body", "{\"userSub\":\"user-123\",\"email\":\"user@example.com\",\"request\":{\"listId\":\"123\"}}"
        );

        assertThrows(RuntimeException.class, () -> lambda.handleRequest(Map.of("Records", List.of(record)), null));
        assertFalse(ses.sendEmailCalled);
    }

    @Test
    void shouldThrowRuntimeExceptionWhenToEmailIsBlank() {
        FakeDynamoDB dynamoDB = new FakeDynamoDB(createGsi2QueryResult(), createTaskQueryResult());
        FakeS3 s3 = new FakeS3();
        FakeSES ses = new FakeSES();

        ProcessUserRequestLambda lambda = new ProcessUserRequestLambda(
                dynamoDB,
                s3,
                ses,
                new ObjectMapper(),
                "todo-list",
                "reports-bucket",
                "noreply@example.com"
        );

        Map<String, Object> record = Map.of(
                "body", "{\"userSub\":\"user-123\",\"email\":\"\",\"request\":{\"listId\":\"123\"}}"
        );

        assertThrows(RuntimeException.class, () -> lambda.handleRequest(Map.of("Records", List.of(record)), null));
        assertFalse(ses.sendEmailCalled);
    }

    @Test
    void shouldThrowRuntimeExceptionWhenSesSendEmailFails() {
        FakeDynamoDB dynamoDB = new FakeDynamoDB(createGsi2QueryResult(), createTaskQueryResult());
        FakeS3 s3 = new FakeS3();
        FakeSES ses = new FakeSES();
        ses.setFailure(new RuntimeException("SES failure"));

        ProcessUserRequestLambda lambda = new ProcessUserRequestLambda(
                dynamoDB,
                s3,
                ses,
                new ObjectMapper(),
                "todo-list",
                "reports-bucket",
                "noreply@example.com"
        );

        Map<String, Object> record = Map.of(
                "body", "{\"userSub\":\"user-123\",\"email\":\"user@example.com\",\"request\":{\"listId\":\"123\"}}"
        );

        assertThrows(RuntimeException.class, () -> lambda.handleRequest(Map.of("Records", List.of(record)), null));
        assertTrue(ses.sendEmailCalled);
    }

    @Test
    void shouldThrowRuntimeExceptionWhenEmailMissingInSqsMessage() {
        FakeDynamoDB dynamoDB = new FakeDynamoDB(createGsi2QueryResult(), createTaskQueryResult());
        FakeS3 s3 = new FakeS3();
        FakeSES ses = new FakeSES();

        ProcessUserRequestLambda lambda = new ProcessUserRequestLambda(
                dynamoDB,
                s3,
                ses,
                new ObjectMapper(),
                "todo-list",
                "reports-bucket",
                "noreply@example.com"
        );

        Map<String, Object> record = Map.of(
                "body", "{\"userSub\":\"user-123\",\"request\":{\"listId\":\"123\"}}"
        );

        assertThrows(RuntimeException.class, () -> lambda.handleRequest(Map.of("Records", List.of(record)), null));
        assertFalse(ses.sendEmailCalled);
    }

    @Test
    void shouldReturn400WhenNoRecordsPresent() {
        FakeDynamoDB dynamoDB = new FakeDynamoDB(createGsi2QueryResult(), createTaskQueryResult());
        FakeS3 s3 = new FakeS3();
        FakeSES ses = new FakeSES();

        ProcessUserRequestLambda lambda = new ProcessUserRequestLambda(
                dynamoDB,
                s3,
                ses,
                new ObjectMapper(),
                "todo-list",
                "reports-bucket",
                "noreply@example.com"
        );

        Map<String, Object> response = lambda.handleRequest(Map.of(), null);

        assertEquals(400, response.get("statusCode"));
    }
}
