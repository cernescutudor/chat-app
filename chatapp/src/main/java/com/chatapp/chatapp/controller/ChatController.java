package com.chatapp.chatapp.controller;

import com.chatapp.chatapp.model.Message;
import com.chatapp.chatapp.model.User;
import com.chatapp.chatapp.repository.UserRepository;
import com.chatapp.chatapp.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final UserRepository userRepository;

    @GetMapping("/chat")
    public String chatPage(Authentication authentication, Model model) {
        String email = authentication.getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Pass current user info to the template
        model.addAttribute("currentUser", currentUser);

        // Pass all users except current user (to start a conversation with)
        List<User> otherUsers = userRepository.findAll()
                .stream()
                .filter(u -> !u.getUserId().equals(currentUser.getUserId()))
                .toList();

        model.addAttribute("users", otherUsers);
        return "chat";
    }

    // REST endpoint to fetch conversation history
    @GetMapping("/api/messages/{recipientId}")
    public ResponseEntity<List<Message>> getMessages(
            @PathVariable String recipientId,
            Authentication authentication) {

        String email = authentication.getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Message> messages = chatService.getConversation(
                currentUser.getUserId(), recipientId);

        return ResponseEntity.ok(messages);
    }
}