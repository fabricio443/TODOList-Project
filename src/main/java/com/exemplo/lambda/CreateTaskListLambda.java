package com.exemplo.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CreateTaskListLambda implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final AmazonDynamoDB dynamoDB;
    private final ObjectMapper objectMapper;
    private final String tableName;

    public CreateTaskListLambda() {
        this(AmazonDynamoDBClientBuilder.defaultClient(), new ObjectMapper(), System.getenv().getOrDefault("TABLE_NAME", "todo-list"));
    }

    public CreateTaskListLambda(AmazonDynamoDB dynamoDB, ObjectMapper objectMapper, String tableName) {
        this.dynamoDB = dynamoDB;
        this.objectMapper = objectMapper;
        this.tableName = tableName;
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        try {
            Object bodyValue = input == null ? null : input.get("body");
            Map<String, Object> body;

            if (bodyValue instanceof String stringBody) {
                body = objectMapper.readValue(stringBody, new TypeReference<Map<String, Object>>() {});
            } else if (bodyValue instanceof Map<?, ?> mapBody) {
                body = objectMapper.convertValue(mapBody, new TypeReference<Map<String, Object>>() {});
            } else {
                body = null;
            }

            Object nameValue = body == null ? null : body.get("name");
            if (!(nameValue instanceof String name) || name.isBlank()) {
                return response(400, Map.of("message", "Name is required"));
            }

            String id = UUID.randomUUID().toString();
            String userSub = extractUserSub(input);

            Map<String, AttributeValue> item = new HashMap<>();
            // Primary key for list entity
            item.put("PK", new AttributeValue("LIST#" + id));
            item.put("SK", new AttributeValue("META"));
            item.put("GSI1PK", new AttributeValue("LIST"));
            item.put("GSI1SK", new AttributeValue("LIST#" + id));
            item.put("name", new AttributeValue(name));
            item.put("type", new AttributeValue("task-list"));
            if (userSub != null && !userSub.isBlank()) {
                item.put("userSub", new AttributeValue(userSub));
                item.put("GSI2PK", new AttributeValue("USER#" + userSub));
                item.put("GSI2SK", new AttributeValue("LIST#" + id));
            }

            dynamoDB.putItem(new PutItemRequest(tableName, item));

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("id", id);
            responseBody.put("name", name);
            return response(201, responseBody);
        } catch (JsonProcessingException e) {
            return response(400, Map.of("message", "Invalid JSON"));
        } catch (RuntimeException e) {
            return response(500, Map.of("message", "Error creating task list"));
        }
    }

    private String extractUserSub(Map<String, Object> input) {
        if (input == null) {
            return null;
        }

        Object requestContextValue = input.get("requestContext");
        if (!(requestContextValue instanceof Map<?, ?> requestContext)) {
            return null;
        }

        Object authorizerValue = requestContext.get("authorizer");
        if (!(authorizerValue instanceof Map<?, ?> authorizer)) {
            return null;
        }

        Object claimsValue = authorizer.get("claims");
        if (!(claimsValue instanceof Map<?, ?> claims)) {
            return null;
        }

        Object subValue = claims.get("sub");
        return subValue instanceof String ? (String) subValue : null;
    }

    private Map<String, Object> response(int statusCode, Map<String, Object> body) {
    Map<String, Object> response = new HashMap<>();

    try {
        response.put("statusCode", statusCode);
        response.put("headers", Map.of(
                "Content-Type",
                "application/json"
        ));
        response.put(
                "body",
                objectMapper.writeValueAsString(body)
        );

    } catch (JsonProcessingException e) {
        response.put("statusCode", 500);
        response.put("body", "{\"message\":\"Error converting response\"}");
    }

    return response;
}
}
