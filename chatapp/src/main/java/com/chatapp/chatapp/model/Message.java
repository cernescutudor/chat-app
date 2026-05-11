package com.chatapp.chatapp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class Message {

    private String conversationId;   // Partition key (e.g. "user1#user2")
    private String timestamp;        // Sort key (ISO-8601 string)
    private String senderId;         // userId of sender
    private String recipientId;      // userId of recipient
    private String senderUsername;   // display name (denormalized for speed)
    private String content;          // message text
    private String messageType;      // TEXT or IMAGE
    private String mediaKey;         // S3 object key for media messages
    private String mediaUrl;         // App URL used to render media messages
    private String mediaContentType; // MIME type for media messages

    @DynamoDbPartitionKey
    public String getConversationId() {
        return conversationId;
    }

    @DynamoDbSortKey
    public String getTimestamp() {
        return timestamp;
    }
}