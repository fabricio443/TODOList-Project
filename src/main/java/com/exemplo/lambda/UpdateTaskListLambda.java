package com.exemplo.lambda;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

public class UpdateTaskListLambda implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final AmazonDynamoDB dynamoDB;
    private final ObjectMapper objectMapper;
    private final String tableName;

    public UpdateTaskListLambda() {
        this(AmazonDynamoDBClientBuilder.defaultClient(), new ObjectMapper(), System.getenv().getOrDefault("TABLE_NAME", "todo-list"));
    }

    public UpdateTaskListLambda(AmazonDynamoDB dynamoDB, ObjectMapper objectMapper, String tableName) {
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

            Object idValue = body == null ? null : body.get("id");
            Object nameValue = body == null ? null : body.get("name");
            if (!(idValue instanceof String id) || !(nameValue instanceof String name) || id.isBlank() || name.isBlank()) {
                return response(400, Map.of("message", "id and name are required"));
            }

            Map<String, AttributeValue> key = new HashMap<>();
            // Primary key format for list entity
            key.put("PK", new AttributeValue("LIST#" + id));
            key.put("SK", new AttributeValue("META"));

            Map<String, AttributeValue> attributes = new HashMap<>();
            attributes.put(":name", new AttributeValue(name));

            UpdateItemRequest request = new UpdateItemRequest()
                    .withTableName(tableName)
                    .withKey(key)
                    .withUpdateExpression("SET #name = :name")
                    .withExpressionAttributeNames(Map.of("#name", "name"))
                    .withExpressionAttributeValues(attributes);

            dynamoDB.updateItem(request);

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("id", id);
            responseBody.put("name", name);
            return response(200, responseBody);
        } catch (JsonProcessingException e) {
            return response(400, Map.of("message", "Invalid JSON"));
        } catch (RuntimeException e) {
            if (context != null) {
                try {
                    context.getLogger().log(e.getMessage());
                } catch (Exception ignore) {
                    System.err.println(e.getMessage());
                }
            } else {
                System.err.println(e.getMessage());
            }

            return response(500, Map.of("message", "Error updating task list"));
        }
    }

    private Map<String, Object> response(int statusCode, Map<String, Object> body) {
        Map<String, Object> response = new HashMap<>();
        try {
            response.put("statusCode", statusCode);
            response.put("headers", Map.of("Content-Type", "application/json"));
            response.put("body", objectMapper.writeValueAsString(body));

        } catch (JsonProcessingException e) {
            response.put("statusCode", 500);
            response.put("body", "{\"message\":\"Error converting response\"}");
        }

        return response;
    }
}
