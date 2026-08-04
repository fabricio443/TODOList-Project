package com.exemplo.lambda;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ListTaskListsLambda implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final AmazonDynamoDB dynamoDB;
    private final ObjectMapper objectMapper;
    private final String tableName;

    public ListTaskListsLambda() {
        this(AmazonDynamoDBClientBuilder.defaultClient(), new ObjectMapper(), System.getenv().getOrDefault("TABLE_NAME", "todo-list"));
    }

    public ListTaskListsLambda(AmazonDynamoDB dynamoDB, String tableName) {
        this(dynamoDB, new ObjectMapper(), tableName);
    }

    public ListTaskListsLambda(AmazonDynamoDB dynamoDB, ObjectMapper objectMapper, String tableName) {
        this.dynamoDB = dynamoDB;
        this.objectMapper = objectMapper;
        this.tableName = tableName;
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        try {
                Map<String, AttributeValue> expressionValues = new HashMap<>();
            expressionValues.put(":gsi1pk", new AttributeValue("LIST"));

            QueryRequest queryRequest = new QueryRequest()
                    .withTableName(tableName)
                    .withIndexName("GSI1")
                    .withKeyConditionExpression("GSI1PK = :gsi1pk")
                    .withExpressionAttributeValues(expressionValues);

            QueryResult result = dynamoDB.query(queryRequest);

                List<Map<String, Object>> lists = result.getItems().stream()
                    .map(item -> {
                        Map<String, Object> listItem = new HashMap<>();
                    // id is encoded in the GSI1 sort key for list entities
                    AttributeValue skAttr = item.get("GSI1SK");
                    if (skAttr == null || skAttr.getS() == null) {
                        skAttr = item.get("SK");
                    }
                    String rawSk = skAttr == null || skAttr.getS() == null ? "" : skAttr.getS();
                    listItem.put("id", rawSk.replace("LIST#", ""));

                    AttributeValue nameAttr = item.get("name");
                    String name = nameAttr == null || nameAttr.getS() == null ? "" : nameAttr.getS();
                    listItem.put("name", name);
                        return listItem;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("statusCode", 200);
            response.put("headers", Map.of("Content-Type", "application/json"));
            try {
                response.put("body", objectMapper.writeValueAsString(lists));
            } catch (JsonProcessingException e) {
                response.put("statusCode", 500);
                response.put("body", "{\"message\":\"Error converting response\"}");
            }

            return response;
        } catch (Exception e) {
            if (context != null) {
                try {
                    context.getLogger().log(e.getMessage());
                } catch (Exception ignore) {
                    System.err.println(e.getMessage());
                }
            } else {
                System.err.println(e.getMessage());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("statusCode", 500);
            response.put("headers", Map.of("Content-Type", "application/json"));
            try {
                response.put("body", objectMapper.writeValueAsString(Map.of("message", "Error listing task lists")));
            } catch (JsonProcessingException ex) {
                response.put("body", "{\"message\":\"Error listing task lists\"}");
            }
            return response;
        }
    }
}
