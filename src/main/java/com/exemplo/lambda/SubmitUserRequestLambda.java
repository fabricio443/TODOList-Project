package com.exemplo.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

public class SubmitUserRequestLambda implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final AmazonSQS sqs;
    private final ObjectMapper objectMapper;
    private final String queueUrl;

    public SubmitUserRequestLambda() {
        this(AmazonSQSClientBuilder.defaultClient(), new ObjectMapper(), System.getenv().getOrDefault("SQS_QUEUE_URL", ""));
    }

    public SubmitUserRequestLambda(AmazonSQS sqs, ObjectMapper objectMapper, String queueUrl) {
        this.sqs = sqs;
        this.objectMapper = objectMapper;
        this.queueUrl = queueUrl;
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        try {
            if (queueUrl == null || queueUrl.isBlank()) {
                return response(500, "Queue URL is not configured");
            }

            Object bodyValue = input == null ? null : input.get("body");
            Map<String, Object> body = parseBody(bodyValue);

            if (body == null || body.isEmpty()) {
                return response(400, "Payload inválido ou ausente");
            }

            String messageBody = objectMapper.writeValueAsString(body);
            SendMessageRequest sendMessageRequest = new SendMessageRequest()
                    .withQueueUrl(queueUrl)
                    .withMessageBody(messageBody);

            SendMessageResult sendMessageResult = sqs.sendMessage(sendMessageRequest);
            if (sendMessageResult == null || sendMessageResult.getMessageId() == null) {
                return response(500, "Erro enviando mensagem para a fila");
            }

            return response(202, "Sua solicitação foi recebida. Estamos processando-a.");
        } catch (JsonProcessingException e) {
            return response(400, "Payload inválido");
        } catch (RuntimeException e) {
            logError(context, e);
            return response(500, "Erro processando a solicitação");
        }
    }

    private Map<String, Object> parseBody(Object bodyValue) {
        if (bodyValue instanceof String stringBody) {
            try {
                return objectMapper.readValue(stringBody, new TypeReference<Map<String, Object>>() {});
            } catch (JsonProcessingException e) {
                return null;
            }
        }
        if (bodyValue instanceof Map<?, ?> mapBody) {
            return objectMapper.convertValue(mapBody, new TypeReference<Map<String, Object>>() {});
        }
        return null;
    }

    private Map<String, Object> response(int statusCode, String body) {
        Map<String, Object> response = new HashMap<>();
        response.put("statusCode", statusCode);
        response.put("headers", Map.of("Content-Type", "text/plain"));
        response.put("body", body);
        return response;
    }

    private void logError(Context context, RuntimeException e) {
        if (context != null) {
            try {
                context.getLogger().log(e.getMessage());
            } catch (Exception ignore) {
                System.err.println(e.getMessage());
            }
        } else {
            System.err.println(e.getMessage());
        }
    }
}
