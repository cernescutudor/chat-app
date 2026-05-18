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
    private final FriendRequestService friendRequestService;

    public Message sendTextMessage(String senderId, String recipientId,
                                   String senderUsername, String content) {
        if (!friendRequestService.areFriends(senderId, recipientId)) {
            throw new RuntimeException("You can only message users who have accepted your friend request");
        }

        String conversationId = buildConversationId(senderId, recipientId);

        Message message = Message.builder()
                .conversationId(conversationId)
                .timestamp(Instant.now().toString())
                .senderId(senderId)
                .recipientId(recipientId)
                .senderUsername(senderUsername)
                .content(content)
                .messageType("TEXT")
                .build();

        messageRepository.save(message);
        return message;
    }

    public Message sendImageMessage(String senderId, String recipientId,
                                    String senderUsername, String mediaKey,
                                    String mediaUrl, String mediaContentType) {
        if (!friendRequestService.areFriends(senderId, recipientId)) {
            throw new RuntimeException("You can only message users who have accepted your friend request");
        }

        String conversationId = buildConversationId(senderId, recipientId);

        Message message = Message.builder()
                .conversationId(conversationId)
                .timestamp(Instant.now().toString())
                .senderId(senderId)
                .recipientId(recipientId)
                .senderUsername(senderUsername)
                .messageType("IMAGE")
                .mediaKey(mediaKey)
                .mediaUrl(mediaUrl)
                .mediaContentType(mediaContentType)
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