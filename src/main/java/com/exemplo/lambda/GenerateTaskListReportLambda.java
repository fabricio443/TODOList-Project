package com.exemplo.lambda;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
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
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GenerateTaskListReportLambda implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final AmazonDynamoDB dynamoDB;
    private final AmazonS3 s3;
    private final ObjectMapper objectMapper;
    private final String tableName;
    private final String bucketName;

    public GenerateTaskListReportLambda() {
        this(AmazonDynamoDBClientBuilder.defaultClient(), AmazonS3ClientBuilder.defaultClient(), new ObjectMapper(), System.getenv().getOrDefault("TABLE_NAME", "todo-list"), System.getenv().getOrDefault("REPORTS_BUCKET", ""));
    }

    public GenerateTaskListReportLambda(AmazonDynamoDB dynamoDB, AmazonS3 s3, ObjectMapper objectMapper, String tableName, String bucketName) {
        this.dynamoDB = dynamoDB;
        this.s3 = s3;
        this.objectMapper = objectMapper;
        this.tableName = tableName;
        this.bucketName = bucketName;
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        try {
            Object pathParametersValue = input == null ? null : input.get("pathParameters");
            Map<String, Object> pathParameters = null;
            if (pathParametersValue instanceof Map<?, ?> map) {
                pathParameters = objectMapper.convertValue(map, Map.class);
            }

            String listId = pathParameters == null ? null : (pathParameters.get("listId") instanceof String ? (String) pathParameters.get("listId") : null);
            if (listId == null || listId.isBlank()) {
                return response(400, Map.of("message", "listId is required in pathParameters"));
            }

            if (bucketName == null || bucketName.isBlank()) {
                return response(500, Map.of("message", "REPORTS_BUCKET not configured"));
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
                task.put("taskId", item.getOrDefault("taskId", new AttributeValue("")).getS());
                task.put("name", item.getOrDefault("name", new AttributeValue("")).getS());
                task.put("status", item.getOrDefault("status", new AttributeValue("PENDING")).getS());
                task.put("createdAt", item.getOrDefault("createdAt", new AttributeValue("")).getS());
                return task;
            }).collect(Collectors.toList());

            // build CSV
            StringBuilder csv = new StringBuilder();
            csv.append("taskId,name,status,createdAt\n");
            for (Map<String, Object> t : tasks) {
                csv.append(escapeCsv(String.valueOf(t.getOrDefault("taskId", ""))));
                csv.append(',');
                csv.append(escapeCsv(String.valueOf(t.getOrDefault("name", ""))));
                csv.append(',');
                csv.append(escapeCsv(String.valueOf(t.getOrDefault("status", ""))));
                csv.append(',');
                csv.append(escapeCsv(String.valueOf(t.getOrDefault("createdAt", ""))));
                csv.append('\n');
            }

            String key = String.format("reports/list-%s-%d.csv", listId, System.currentTimeMillis());
            byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType("text/csv");
            metadata.setContentLength(bytes.length);

            PutObjectRequest putRequest = new PutObjectRequest(bucketName, key, inputStream, metadata);
            s3.putObject(putRequest);

            return response(200, Map.of("bucket", bucketName, "key", key));
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
            return response(500, Map.of("message", "Error generating report"));
        }
    }

    private String escapeCsv(String s) {
        if (s == null) return "";
        String escaped = s.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\"")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
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
