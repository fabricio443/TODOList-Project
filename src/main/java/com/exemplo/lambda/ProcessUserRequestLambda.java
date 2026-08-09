package com.exemplo.lambda;

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
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.amazonaws.services.simpleemail.model.SendEmailResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ProcessUserRequestLambda implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final AmazonDynamoDB dynamoDB;
    private final AmazonS3 s3;
    private final AmazonSimpleEmailService ses;
    private final ObjectMapper objectMapper;
    private final String tableName;
    private final String bucketName;
    private final String fromEmail;

    public ProcessUserRequestLambda() {
        this(
                AmazonDynamoDBClientBuilder.defaultClient(),
                AmazonS3ClientBuilder.defaultClient(),
                AmazonSimpleEmailServiceClientBuilder.defaultClient(),
                new ObjectMapper(),
                System.getenv().getOrDefault("TABLE_NAME", "todo-list"),
                System.getenv().getOrDefault("REPORTS_BUCKET", ""),
                System.getenv().getOrDefault("SES_FROM_EMAIL", "")
        );
    }

    public ProcessUserRequestLambda(AmazonDynamoDB dynamoDB,
                                    AmazonS3 s3,
                                    AmazonSimpleEmailService ses,
                                    ObjectMapper objectMapper,
                                    String tableName,
                                    String bucketName,
                                    String fromEmail) {
        this.dynamoDB = dynamoDB;
        this.s3 = s3;
        this.ses = ses;
        this.objectMapper = objectMapper;
        this.tableName = tableName;
        this.bucketName = bucketName;
        this.fromEmail = fromEmail;
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        try {
            List<Map<String, Object>> records = parseRecords(input);
            if (records == null || records.isEmpty()) {
                return response(400, Map.of("message", "No SQS records found"));
            }

            for (Map<String, Object> record : records) {
                Map<String, Object> body = parseBody(record.get("body"));
                if (body == null) {
                    throw new RuntimeException("Invalid SQS message body");
                }

                String userSub = body.get("userSub") instanceof String ? (String) body.get("userSub") : null;
                if (userSub == null || userSub.isBlank()) {
                    throw new RuntimeException("userSub is required");
                }

                Object requestValue = body.get("request");
                if (!(requestValue instanceof Map<?, ?> requestMap)) {
                    throw new RuntimeException("request object is required");
                }

                String listId = requestMap.get("listId") instanceof String ? (String) requestMap.get("listId") : null;
                if (listId == null || listId.isBlank()) {
                    throw new RuntimeException("listId is required in request");
                }

                String email = body.get("email") instanceof String ? (String) body.get("email") : null;
                if (email == null || email.isBlank()) {
                    throw new RuntimeException("email is required");
                }

                verifyListOwnership(userSub, listId);
                List<Map<String, Object>> tasks = queryTasks(listId);
                String key = uploadReport(listId, userSub, tasks);
                sendEmail(context, listId, key, email);
            }

            return response(200, Map.of("message", "Processed SQS records"));
        } catch (RuntimeException e) {
            logError(context, e);
            throw e;
        }
    }

    private List<Map<String, Object>> queryTasks(String listId) {
        String pk = "LIST#" + listId;

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":pk", new AttributeValue(pk));
        expressionValues.put(":skPrefix", new AttributeValue("TASK#"));

        QueryRequest queryRequest = new QueryRequest()
                .withTableName(tableName)
                .withKeyConditionExpression("PK = :pk and begins_with(SK, :skPrefix)")
                .withExpressionAttributeValues(expressionValues);

        QueryResult result = dynamoDB.query(queryRequest);

        return result.getItems().stream().map(item -> {
            Map<String, Object> task = new HashMap<>();
            task.put("taskId", item.getOrDefault("taskId", new AttributeValue("")).getS());
            task.put("name", item.getOrDefault("name", new AttributeValue("")).getS());
            task.put("status", item.getOrDefault("status", new AttributeValue("PENDING")).getS());
            task.put("createdAt", item.getOrDefault("createdAt", new AttributeValue("")).getS());
            return task;
        }).collect(Collectors.toList());
    }

    private void verifyListOwnership(String userSub, String listId) {
        String gsi2pk = "USER#" + userSub;
        String gsi2sk = "LIST#" + listId;

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":gsi2pk", new AttributeValue(gsi2pk));
        expressionValues.put(":gsi2sk", new AttributeValue(gsi2sk));

        QueryRequest queryRequest = new QueryRequest()
                .withTableName(tableName)
                .withIndexName("GSI2")
                .withKeyConditionExpression("GSI2PK = :gsi2pk and GSI2SK = :gsi2sk")
                .withExpressionAttributeValues(expressionValues)
                .withLimit(1);

        QueryResult result = dynamoDB.query(queryRequest);
        if (result.getCount() == null || result.getCount() == 0) {
            throw new RuntimeException("Task list not found for user");
        }
    }

    private String uploadReport(String listId, String userSub, List<Map<String, Object>> tasks) {
        if (bucketName == null || bucketName.isBlank()) {
            throw new RuntimeException("REPORTS_BUCKET not configured");
        }

        StringBuilder csv = new StringBuilder();
        csv.append("taskId,name,status,createdAt\n");
        for (Map<String, Object> task : tasks) {
            csv.append(escapeCsv(String.valueOf(task.getOrDefault("taskId", ""))));
            csv.append(',');
            csv.append(escapeCsv(String.valueOf(task.getOrDefault("name", ""))));
            csv.append(',');
            csv.append(escapeCsv(String.valueOf(task.getOrDefault("status", ""))));
            csv.append(',');
            csv.append(escapeCsv(String.valueOf(task.getOrDefault("createdAt", ""))));
            csv.append('\n');
        }

        String key = String.format("reports/user-%s/list-%s-%d.csv", userSub, listId, System.currentTimeMillis());
        byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("text/csv");
        metadata.setContentLength(bytes.length);

        PutObjectRequest putRequest = new PutObjectRequest(bucketName, key, inputStream, metadata);
        s3.putObject(putRequest);

        return key;
    }

    private void sendEmail(Context context, String listId, String key, String toEmail) {
        if (fromEmail == null || fromEmail.isBlank()) {
            throw new RuntimeException("SES fromEmail is not configured");
        }
        if (toEmail == null || toEmail.isBlank()) {
            throw new RuntimeException("SES toEmail is not configured");
        }
        if (!isValidEmailAddress(fromEmail)) {
            throw new RuntimeException("SES fromEmail is invalid");
        }
        if (!isValidEmailAddress(toEmail)) {
            throw new RuntimeException("SES toEmail is invalid");
        }

        logInfo(context, String.format("Sending SES email from=%s to=%s listId=%s bucket=%s key=%s",
                fromEmail, toEmail, listId, bucketName, key));

        String subjectText = String.format("Relatório de tarefas gerado para lista %s", listId);
        String bodyText = String.format("O relatório da lista %s foi gerado e enviado para o bucket %s com a chave %s.", listId, bucketName, key);

        SendEmailRequest emailRequest = new SendEmailRequest()
                .withSource(fromEmail)
                .withDestination(new Destination().withToAddresses(toEmail))
                .withMessage(new Message()
                        .withSubject(new Content().withData(subjectText))
                        .withBody(new Body().withText(new Content().withData(bodyText))));

        SendEmailResult result = ses.sendEmail(emailRequest);
        if (result == null) {
            String message = String.format("SES sendEmail returned null result for from=%s to=%s listId=%s key=%s",
                    fromEmail, toEmail, listId, key);
            logError(context, new RuntimeException(message));
            throw new RuntimeException(message);
        }

        String messageId = result.getMessageId();
        if (messageId == null || messageId.isBlank()) {
            String message = String.format("SES sendEmail returned invalid MessageId for from=%s to=%s listId=%s key=%s",
                    fromEmail, toEmail, listId, key);
            logError(context, new RuntimeException(message));
            throw new RuntimeException(message);
        }

        logInfo(context, String.format("SES email sent successfully from=%s to=%s listId=%s key=%s messageId=%s",
                fromEmail, toEmail, listId, key, messageId));
    }

    private boolean isValidEmailAddress(String email) {
        return email != null && email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    }

    private void logInfo(Context context, String message) {
        if (context != null) {
            try {
                context.getLogger().log(message);
            } catch (Exception ignore) {
                System.out.println(message);
            }
        } else {
            System.out.println(message);
        }
    }

    private List<Map<String, Object>> parseRecords(Map<String, Object> input) {
        if (input == null) {
            return null;
        }

        Object recordsValue = input.get("Records");
        if (!(recordsValue instanceof List<?> records)) {
            return null;
        }

        return records.stream()
                .filter(record -> record instanceof Map<?, ?>)
                .map(record -> objectMapper.convertValue(record, new TypeReference<Map<String, Object>>() {}))
                .collect(Collectors.toList());
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
