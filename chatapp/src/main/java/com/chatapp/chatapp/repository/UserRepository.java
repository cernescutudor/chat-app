package com.chatapp.chatapp.repository;

import com.chatapp.chatapp.model.User;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;

import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {

    private final DynamoDbTable<User> table;

    public UserRepository(DynamoDbEnhancedClient enhancedClient) {
        this.table = enhancedClient.table("Users", TableSchema.fromBean(User.class));
    }

    public void save(User user) {
        table.putItem(user);
    }

    public Optional<User> findById(String userId) {
        User user = table.getItem(Key.builder().partitionValue(userId).build());
        return Optional.ofNullable(user);
    }

    // Scan the whole table to find a user by email (fine for small scale)
    public Optional<User> findByEmail(String email) {
        return table.scan(ScanEnhancedRequest.builder().build())
                .items()
                .stream()
                .filter(u -> email.equals(u.getEmail()))
                .findFirst();
    }

    // Scan to find by username
    public Optional<User> findByUsername(String username) {
        return table.scan(ScanEnhancedRequest.builder().build())
                .items()
                .stream()
                .filter(u -> username.equals(u.getUsername()))
                .findFirst();
    }

    public void createTableIfNotExists() {
        try {
            table.createTable();
        } catch (Exception e) {
            // Table already exists, ignore
        }
    }
    
    public List<User> findAll() {
        return table.scan(ScanEnhancedRequest.builder().build())
                .items()
                .stream()
                .toList();

    }
}