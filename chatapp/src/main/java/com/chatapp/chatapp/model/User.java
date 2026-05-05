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
public class User {

    private String userId;         // Partition key (UUID)
    private String username;       // Unique display name
    private String email;          // Used for login
    private String passwordHash;   // BCrypt hashed password
    private String profilePictureUrl; // S3/MinIO URL

    @DynamoDbPartitionKey
    public String getUserId() {
        return userId;
    }
}