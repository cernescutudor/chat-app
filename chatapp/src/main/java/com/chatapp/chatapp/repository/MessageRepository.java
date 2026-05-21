package com.chatapp.chatapp.repository;

import com.chatapp.chatapp.model.Message;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

import java.util.List;

@Repository
public class MessageRepository {

    private final DynamoDbTable<Message> table;

    public MessageRepository(DynamoDbEnhancedClient enhancedClient) {
        this.table = enhancedClient.table("Messages", TableSchema.fromBean(Message.class));
    }

    public void save(Message message) {
        table.putItem(message);
    }


    
    // Fetch all messages for a conversation, sorted by timestamp (DynamoDB sort key)
    public List<Message> findByConversationId(String conversationId) {
        QueryConditional queryConditional = QueryConditional
                .keyEqualTo(Key.builder().partitionValue(conversationId).build());

        return table.query(QueryEnhancedRequest.builder()
                .queryConditional(queryConditional)
                .build())
                .items()
                .stream()
                .toList();
    }

    public void createTableIfNotExists() {
        try {
            table.createTable();
        } catch (Exception e) {
            // Table already exists, ignore
        }
    }
}