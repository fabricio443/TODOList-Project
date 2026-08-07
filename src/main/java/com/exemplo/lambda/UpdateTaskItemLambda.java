package com.exemplo.lambda;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

public class UpdateTaskItemLambda implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final AmazonDynamoDB dynamoDB;
    private final ObjectMapper objectMapper;
    private final String tableName;

    public UpdateTaskItemLambda() {
        this(AmazonDynamoDBClientBuilder.defaultClient(), new ObjectMapper(), System.getenv().getOrDefault("TABLE_NAME", "todo-list"));
    }

    public UpdateTaskItemLambda(AmazonDynamoDB dynamoDB, ObjectMapper objectMapper, String tableName) {
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
            String taskId = pathParameters == null ? null : (pathParameters.get("taskId") instanceof String ? (String) pathParameters.get("taskId") : null);

            if (listId == null || listId.isBlank() || taskId == null || taskId.isBlank()) {
                return response(400, Map.of("message", "listId and taskId are required in pathParameters"));
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

            Map<String, AttributeValue> key = new HashMap<>();
            key.put("PK", new AttributeValue("LIST#" + listId));
            key.put("SK", new AttributeValue("TASK#" + taskId));

            GetItemRequest getItemRequest = new GetItemRequest()
                    .withTableName(tableName)
                    .withKey(key);

            GetItemResult getItemResult = dynamoDB.getItem(getItemRequest);
            Map<String, AttributeValue> existing = getItemResult == null ? null : getItemResult.getItem();
            if (existing == null) {
                return response(404, Map.of("message", "Task item not found"));
            }

            String status = existing.getOrDefault("status", new AttributeValue("PENDING")).getS();
            String createdAt = existing.getOrDefault("createdAt", new AttributeValue("")).getS();

            Map<String, AttributeValue> values = Map.of(":name", new AttributeValue(name));

            UpdateItemRequest updateRequest = new UpdateItemRequest()
                    .withTableName(tableName)
                    .withKey(key)
                    .withUpdateExpression("SET #name = :name")
                    .withExpressionAttributeNames(Map.of("#name", "name"))
                    .withExpressionAttributeValues(values);

            dynamoDB.updateItem(updateRequest);

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("listId", listId);
            responseBody.put("taskId", taskId);
            responseBody.put("name", name);
            responseBody.put("status", status);
            responseBody.put("createdAt", createdAt);

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
            return response(500, Map.of("message", "Error updating task item"));
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
