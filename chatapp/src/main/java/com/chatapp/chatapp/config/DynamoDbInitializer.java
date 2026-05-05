package com.chatapp.chatapp.config;

import com.chatapp.chatapp.repository.MessageRepository;
import com.chatapp.chatapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DynamoDbInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final MessageRepository messageRepository;

    @Override
    public void run(ApplicationArguments args) {
        userRepository.createTableIfNotExists();
        messageRepository.createTableIfNotExists();
        System.out.println("DynamoDB tables ready.");
    }
}