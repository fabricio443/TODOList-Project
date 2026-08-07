package com.exemplo.lambda;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ListTaskItemsLambda implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final AmazonDynamoDB dynamoDB;
    private final ObjectMapper objectMapper;
    private final String tableName;

    public ListTaskItemsLambda() {
        this(AmazonDynamoDBClientBuilder.defaultClient(), new ObjectMapper(), System.getenv().getOrDefault("TABLE_NAME", "todo-list"));
    }

    public ListTaskItemsLambda(AmazonDynamoDB dynamoDB, ObjectMapper objectMapper, String tableName) {
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

            String pk = "LIST#" + listId;
            Map<String, AttributeValue> expressionValues = new HashMap<>();
            expressionValues.put(":pk", new AttributeValue(pk));
            expressionValues.put(":skPrefix", new AttributeValue("TASK#"));

            QueryRequest queryRequest = new QueryRequest()
                    .withTableName(tableName)
                    .withKeyConditionExpression("PK = :pk and begins_with(SK, :skPrefix)")
                    .withExpressionAttributeValues(expressionValues);

            QueryResult result = dynamoDB.query(queryRequest);

            List<Map<String, Object>> tasks = result.getItems().stream().map(item -> {
                Map<String, Object> task = new HashMap<>();
                task.put("taskId", item.getOrDefault("taskId", new AttributeValue("")) .getS());
                task.put("name", item.getOrDefault("name", new AttributeValue("")) .getS());
                task.put("status", item.getOrDefault("status", new AttributeValue("PENDING")).getS());
                task.put("createdAt", item.getOrDefault("createdAt", new AttributeValue("")) .getS());
                return task;
            }).collect(Collectors.toList());

            return response(200, tasks);
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
            return response(500, Map.of("message", "Error listing task items"));
        }
    }

    private Map<String, Object> response(int statusCode, Object body) {
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
