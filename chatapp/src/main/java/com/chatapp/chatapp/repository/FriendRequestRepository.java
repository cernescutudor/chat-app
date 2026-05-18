package com.chatapp.chatapp.repository;

import com.chatapp.chatapp.model.FriendRequest;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;

import java.util.List;
import java.util.Optional;

@Repository
public class FriendRequestRepository {

    // Backing DynamoDB table for persisted friend-request records.
    private final DynamoDbTable<FriendRequest> table;

    public FriendRequestRepository(DynamoDbEnhancedClient enhancedClient) {
        this.table = enhancedClient.table("FriendRequests", TableSchema.fromBean(FriendRequest.class));
    }

    /**
     * Insert or overwrite a friend-request record.
     */
    public void save(FriendRequest friendRequest) {
        table.putItem(friendRequest);
    }

    /**
     * Find a request by its primary key.
     */
    public Optional<FriendRequest> findById(String requestId) {
        FriendRequest request = table.getItem(Key.builder().partitionValue(requestId).build());
        return Optional.ofNullable(request);
    }

    // Find all friend requests sent by a user
    public List<FriendRequest> findBySenderId(String senderId) {
        return table.scan(ScanEnhancedRequest.builder().build())
                .items()
                .stream()
                .filter(r -> senderId.equals(r.getSenderId()))
                .toList();
    }

    // Find all friend requests received by a user
    public List<FriendRequest> findByRecipientId(String recipientId) {
        return table.scan(ScanEnhancedRequest.builder().build())
                .items()
                .stream()
                .filter(r -> recipientId.equals(r.getRecipientId()))
                .toList();
    }

    // Find pending requests sent by a user to a specific recipient
    public Optional<FriendRequest> findPendingRequestBetween(String senderId, String recipientId) {
        return table.scan(ScanEnhancedRequest.builder().build())
                .items()
                .stream()
                .filter(r -> senderId.equals(r.getSenderId()) && 
                            recipientId.equals(r.getRecipientId()) &&
                            "PENDING".equals(r.getStatus()))
                .findFirst();
    }

    // Check if users are friends (accepted request exists in either direction)
    public boolean areFriends(String userId1, String userId2) {
        return table.scan(ScanEnhancedRequest.builder().build())
                .items()
                .stream()
                .anyMatch(r -> "ACCEPTED".equals(r.getStatus()) &&
                        ((userId1.equals(r.getSenderId()) && userId2.equals(r.getRecipientId())) ||
                         (userId2.equals(r.getSenderId()) && userId1.equals(r.getRecipientId()))));
    }

    // Get all accepted friend requests for a user (in both directions)
    public List<FriendRequest> findAcceptedFriends(String userId) {
        return table.scan(ScanEnhancedRequest.builder().build())
                .items()
                .stream()
                .filter(r -> "ACCEPTED".equals(r.getStatus()) &&
                        (userId.equals(r.getSenderId()) || userId.equals(r.getRecipientId())))
                .toList();
    }

    /**
     * Delete a request by primary key if it currently exists.
     */
    public void delete(String requestId) {
        findById(requestId).ifPresent(r -> table.deleteItem(Key.builder().partitionValue(requestId).build()));
    }

    public void createTableIfNotExists() {
        try {
            table.createTable();
        } catch (Exception e) {
            // Table already exists, ignore
        }
    }
}
