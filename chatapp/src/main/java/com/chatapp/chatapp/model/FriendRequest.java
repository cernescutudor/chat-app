package com.chatapp.chatapp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class FriendRequest {
    private String requestId;
    private String senderId;
    private String senderUsername;
    private String recipientId;
    private String recipientUsername;
    private String status;
    private String timestamp;
    private String message;

    @DynamoDbPartitionKey
    public String getRequestId() {
        return requestId;
    }
}
