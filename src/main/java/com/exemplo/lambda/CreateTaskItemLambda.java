package com.exemplo.lambda;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CreateTaskItemLambda implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final AmazonDynamoDB dynamoDB;
    private final ObjectMapper objectMapper;
    private final String tableName;

    public CreateTaskItemLambda() {
        this(AmazonDynamoDBClientBuilder.defaultClient(), new ObjectMapper(), System.getenv().getOrDefault("TABLE_NAME", "todo-list"));
    }

    public CreateTaskItemLambda(AmazonDynamoDB dynamoDB, ObjectMapper objectMapper, String tableName) {
        this.dynamoDB = dynamoDB;
        this.objectMapper = objectMapper;
        this.tableName = tableName;
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        try {
            Object pathParametersValue = input == null ? null : input.get("pathParameters");
            Map<String, Object> pathParameters = null;
            if (pathParametersValue instanceof Map<?, ?> map) {
                pathParameters = objectMapper.convertValue(map, new TypeReference<Map<String, Object>>() {});
            }

            String listId = pathParameters == null ? null : (pathParameters.get("listId") instanceof String ? (String) pathParameters.get("listId") : null);
            if (listId == null || listId.isBlank()) {
                return response(400, Map.of("message", "listId is required in pathParameters"));
            }

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
                return response(400, Map.of("message", "name is required"));
            }

            Map<String, AttributeValue> listKey = new HashMap<>();
            listKey.put("PK", new AttributeValue("LIST#" + listId));
            listKey.put("SK", new AttributeValue("META"));

            GetItemRequest getItemRequest = new GetItemRequest()
                    .withTableName(tableName)
                    .withKey(listKey);

            if (dynamoDB.getItem(getItemRequest).getItem() == null) {
                return response(404, Map.of("message", "Task list not found"));
            }

            String taskId = UUID.randomUUID().toString();
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("PK", new AttributeValue("LIST#" + listId));
            item.put("SK", new AttributeValue("TASK#" + taskId));
            item.put("taskId", new AttributeValue(taskId));
            item.put("name", new AttributeValue(name));
            item.put("type", new AttributeValue("task"));
            item.put("createdAt", new AttributeValue(Instant.now().toString()));
            item.put("status", new AttributeValue("PENDING"));

            dynamoDB.putItem(new PutItemRequest(tableName, item));

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("id", taskId);
            responseBody.put("listId", listId);
            responseBody.put("taskId", taskId);
            responseBody.put("name", name);
            responseBody.put("createdAt", item.get("createdAt").getS());
            responseBody.put("status", "PENDING");
            return response(201, responseBody);
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
            return response(500, Map.of("message", "Error creating task item"));
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
