package com.chatapp.chatapp.config;

import com.chatapp.chatapp.repository.MessageRepository;
import com.chatapp.chatapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import com.chatapp.chatapp.repository.FriendRequestRepository;

@Component
@RequiredArgsConstructor
public class DynamoDbInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final FriendRequestRepository friendRequestRepository;

    @Override
    public void run(ApplicationArguments args) {
        userRepository.createTableIfNotExists();
        messageRepository.createTableIfNotExists();
        friendRequestRepository.createTableIfNotExists();
        System.out.println("DynamoDB tables ready.");
    }
}