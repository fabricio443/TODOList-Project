package com.exemplo.lambda;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

public class GetTaskListLambda implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final AmazonDynamoDB dynamoDB;
    private final ObjectMapper objectMapper;
    private final String tableName;

    public GetTaskListLambda() {
        this(AmazonDynamoDBClientBuilder.defaultClient(), new ObjectMapper(), System.getenv().getOrDefault("TABLE_NAME", "todo-list"));
    }

    public GetTaskListLambda(AmazonDynamoDB dynamoDB, ObjectMapper objectMapper, String tableName) {
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
                pathParameters = objectMapper.convertValue(map, new TypeReference<Map<String, Object>>() {
                });
            }

            String listId = pathParameters == null ? null : (pathParameters.get("listId") instanceof String ? (String) pathParameters.get("listId") : null);
            if (listId == null || listId.isBlank()) {
                return response(400, Map.of("message", "listId is required in pathParameters"));
            }

            Map<String, AttributeValue> key = new HashMap<>();
            key.put("PK", new AttributeValue("LIST#" + listId));
            key.put("SK", new AttributeValue("META"));

            GetItemRequest request = new GetItemRequest()
                    .withTableName(tableName)
                    .withKey(key);

            GetItemResult result = dynamoDB.getItem(request);
            Map<String, AttributeValue> item = result == null ? null : result.getItem();
            if (item == null) {
                return response(404, Map.of("message", "Task list not found"));
            }

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("id", listId);
            responseBody.put("name", item.getOrDefault("name", new AttributeValue("")).getS());
            return response(200, responseBody);
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
            return response(500, Map.of("message", "Error retrieving task list"));
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
