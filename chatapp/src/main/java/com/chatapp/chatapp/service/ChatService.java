package com.chatapp.chatapp.service;

import com.chatapp.chatapp.model.Message;
import com.chatapp.chatapp.repository.MessageRepository;
import com.chatapp.chatapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    public Message sendMessage(String senderId, String recipientId,
                               String senderUsername, String content) {
        // Conversation ID is always sorted so "user1#user2" == "user2#user1"
        String conversationId = buildConversationId(senderId, recipientId);

        Message message = Message.builder()
                .conversationId(conversationId)
                .timestamp(Instant.now().toString())
                .senderId(senderId)
                .senderUsername(senderUsername)
                .content(content)
                .build();

        messageRepository.save(message);
        return message;
    }

    public List<Message> getConversation(String userId, String otherUserId) {
        String conversationId = buildConversationId(userId, otherUserId);
        return messageRepository.findByConversationId(conversationId);
    }

    // Ensures the same conversationId regardless of who initiates
    private String buildConversationId(String userId1, String userId2) {
        return userId1.compareTo(userId2) < 0
                ? userId1 + "#" + userId2
                : userId2 + "#" + userId1;
    }
}