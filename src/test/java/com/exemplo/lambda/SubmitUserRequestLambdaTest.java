package com.exemplo.lambda;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import org.mockito.Mockito;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import com.fasterxml.jackson.databind.ObjectMapper;

class SubmitUserRequestLambdaTest {

    @Test
    void shouldSendMessageWhenPayloadIsValid() {
        AmazonSQS sqs = Mockito.mock(AmazonSQS.class);
        SubmitUserRequestLambda lambda = new SubmitUserRequestLambda(sqs, new ObjectMapper(), "https://queue-url");

        SendMessageResult result = new SendMessageResult();
        result.setMessageId("msg-123");
        when(sqs.sendMessage(any(SendMessageRequest.class))).thenReturn(result);

        Map<String, Object> requestContext = Map.of(
                "authorizer", Map.of(
                        "claims", Map.of("sub", "user-123", "email", "user@example.com")
                )
        );

        Map<String, Object> response = lambda.handleRequest(Map.of(
                "body", "{\"listId\":\"abc\",\"action\":\"submit\"}",
                "requestContext", requestContext
        ), null);

        assertEquals(202, response.get("statusCode"));
        assertEquals("Sua solicitação foi recebida. Estamos processando-a.", response.get("body"));
        verify(sqs).sendMessage(argThat(request -> request != null && request.getMessageBody().contains("\"userSub\":\"user-123\"") && request.getMessageBody().contains("\"email\":\"user@example.com\"")));
    }

    @Test
    void shouldReturn400WhenBodyIsInvalidJson() {
        AmazonSQS sqs = Mockito.mock(AmazonSQS.class);
        SubmitUserRequestLambda lambda = new SubmitUserRequestLambda(sqs, new ObjectMapper(), "https://queue-url");

        Map<String, Object> requestContext = Map.of(
                "authorizer", Map.of(
                        "claims", Map.of("sub", "user-123", "email", "test@example.com")
                )
        );

        Map<String, Object> response = lambda.handleRequest(Map.of(
                "body", "{invalid}",
                "requestContext", requestContext
        ), null);

        assertEquals(400, response.get("statusCode"));
    }

    @Test
    void shouldReturn400WhenBodyMissing() {
        AmazonSQS sqs = Mockito.mock(AmazonSQS.class);
        SubmitUserRequestLambda lambda = new SubmitUserRequestLambda(sqs, new ObjectMapper(), "https://queue-url");

        Map<String, Object> requestContext = Map.of(
                "authorizer", Map.of(
                        "claims", Map.of("sub", "user-123")
                )
        );

        Map<String, Object> response = lambda.handleRequest(Map.of(
                "requestContext", requestContext
        ), null);

        assertEquals(400, response.get("statusCode"));
    }

    @Test
    void shouldReturn400WhenUserSubMissing() {
        AmazonSQS sqs = Mockito.mock(AmazonSQS.class);
        SubmitUserRequestLambda lambda = new SubmitUserRequestLambda(sqs, new ObjectMapper(), "https://queue-url");

        Map<String, Object> requestContext = Map.of(
                "authorizer", Map.of(
                        "claims", Map.of("email", "user@example.com")
                )
        );

        Map<String, Object> response = lambda.handleRequest(Map.of(
                "body", "{\"listId\":\"abc\"}",
                "requestContext", requestContext
        ), null);

        assertEquals(400, response.get("statusCode"));
    }

    @Test
    void shouldReturn400WhenUserEmailMissing() {
        AmazonSQS sqs = Mockito.mock(AmazonSQS.class);
        SubmitUserRequestLambda lambda = new SubmitUserRequestLambda(sqs, new ObjectMapper(), "https://queue-url");

        Map<String, Object> requestContext = Map.of(
                "authorizer", Map.of(
                        "claims", Map.of("sub", "user-123")
                )
        );

        Map<String, Object> response = lambda.handleRequest(Map.of(
                "body", "{\"listId\":\"abc\"}",
                "requestContext", requestContext
        ), null);

        assertEquals(400, response.get("statusCode"));
    }

    @Test
    void shouldReturn500WhenSendMessageFails() {
        AmazonSQS sqs = Mockito.mock(AmazonSQS.class);
        SubmitUserRequestLambda lambda = new SubmitUserRequestLambda(sqs, new ObjectMapper(), "https://queue-url");
        when(sqs.sendMessage(any(SendMessageRequest.class))).thenThrow(new RuntimeException("boom"));

        Map<String, Object> requestContext = Map.of(
                "authorizer", Map.of(
                        "claims", Map.of(
                                "sub", "user-123",
                                "email", "test@example.com"
                        )
                )
        );

        Map<String, Object> response = lambda.handleRequest(Map.of(
                "body", "{\"listId\":\"abc\"}",
                "requestContext", requestContext
        ), null);

        assertEquals(500, response.get("statusCode"));
    }

    @Test
    void shouldReturn500WhenQueueUrlMissing() {
        AmazonSQS sqs = Mockito.mock(AmazonSQS.class);
        SubmitUserRequestLambda lambda = new SubmitUserRequestLambda(sqs, new ObjectMapper(), "");

        Map<String, Object> requestContext = Map.of(
                "authorizer", Map.of(
                        "claims", Map.of("sub", "user-123")
                )
        );

        Map<String, Object> response = lambda.handleRequest(Map.of(
                "body", "{\"listId\":\"abc\"}",
                "requestContext", requestContext
        ), null);

        assertEquals(500, response.get("statusCode"));
    }
}
