package com.chatapp.chatapp.controller;

import com.chatapp.chatapp.model.Message;
import com.chatapp.chatapp.model.User;
import com.chatapp.chatapp.repository.UserRepository;
import com.chatapp.chatapp.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final ChatService chatService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // Client sends to /app/chat.send
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload Map<String, String> payload,
                            Authentication authentication) {

        String senderEmail = authentication.getName();
        User sender = userRepository.findByEmail(senderEmail)
                .orElseThrow(() -> new RuntimeException("Sender not found"));

        String recipientId = payload.get("recipientId");
        String content = payload.get("content");

        Message message = chatService.sendTextMessage(
                sender.getUserId(),
                recipientId,
                sender.getUsername(),
                content
        );

        // Build conversation ID the same way ChatService does
        String conversationId = sender.getUserId().compareTo(recipientId) < 0
                ? sender.getUserId() + "#" + recipientId
                : recipientId + "#" + sender.getUserId();

        // Broadcast to everyone subscribed to this conversation topic
        messagingTemplate.convertAndSend("/topic/conversation." + conversationId, message);
    }
}