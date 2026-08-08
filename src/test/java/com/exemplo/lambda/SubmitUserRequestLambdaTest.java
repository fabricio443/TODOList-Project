package com.exemplo.lambda;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
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

        Map<String, Object> response = lambda.handleRequest(Map.of("body", "{\"userId\":\"123\",\"action\":\"submit\"}"), null);

        assertEquals(202, response.get("statusCode"));
        assertEquals("Sua solicitação foi recebida. Estamos processando-a.", response.get("body"));
        verify(sqs).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    void shouldReturn400WhenBodyIsInvalidJson() {
        AmazonSQS sqs = Mockito.mock(AmazonSQS.class);
        SubmitUserRequestLambda lambda = new SubmitUserRequestLambda(sqs, new ObjectMapper(), "https://queue-url");

        Map<String, Object> response = lambda.handleRequest(Map.of("body", "{invalid}"), null);

        assertEquals(400, response.get("statusCode"));
    }

    @Test
    void shouldReturn400WhenBodyMissing() {
        AmazonSQS sqs = Mockito.mock(AmazonSQS.class);
        SubmitUserRequestLambda lambda = new SubmitUserRequestLambda(sqs, new ObjectMapper(), "https://queue-url");

        Map<String, Object> response = lambda.handleRequest(Map.of(), null);

        assertEquals(400, response.get("statusCode"));
    }

    @Test
    void shouldReturn500WhenSendMessageFails() {
        AmazonSQS sqs = Mockito.mock(AmazonSQS.class);
        SubmitUserRequestLambda lambda = new SubmitUserRequestLambda(sqs, new ObjectMapper(), "https://queue-url");
        when(sqs.sendMessage(any(SendMessageRequest.class))).thenThrow(new RuntimeException("boom"));

        Map<String, Object> response = lambda.handleRequest(Map.of("body", "{\"userId\":\"123\"}"), null);

        assertEquals(500, response.get("statusCode"));
    }

    @Test
    void shouldReturn500WhenQueueUrlMissing() {
        AmazonSQS sqs = Mockito.mock(AmazonSQS.class);
        SubmitUserRequestLambda lambda = new SubmitUserRequestLambda(sqs, new ObjectMapper(), "");

        Map<String, Object> response = lambda.handleRequest(Map.of("body", "{\"userId\":\"123\"}"), null);

        assertEquals(500, response.get("statusCode"));
    }
}
