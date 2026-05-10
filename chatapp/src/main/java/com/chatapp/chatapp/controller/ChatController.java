package com.chatapp.chatapp.controller;

import com.chatapp.chatapp.model.Message;
import com.chatapp.chatapp.model.User;
import com.chatapp.chatapp.repository.UserRepository;
import com.chatapp.chatapp.service.ChatService;
import com.chatapp.chatapp.service.MessageMediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final UserRepository userRepository;
    private final MessageMediaService messageMediaService;
    private final SimpMessagingTemplate messagingTemplate;

    @GetMapping("/chat")
    public String chatPage(Authentication authentication, Model model) {
        String email = authentication.getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        model.addAttribute("currentUser", currentUser);

        List<User> otherUsers = userRepository.findAll()
                .stream()
                .filter(u -> !u.getUserId().equals(currentUser.getUserId()))
                .toList();

        model.addAttribute("users", otherUsers);
        return "chat";
    }

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

    @PostMapping("/api/messages/image")
    public ResponseEntity<Message> sendImageMessage(Authentication authentication,
                                                    @RequestParam String recipientId,
                                                    @RequestParam MultipartFile file) {
        String email = authentication.getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new RuntimeException("Recipient not found"));

        String conversationId = buildConversationId(currentUser.getUserId(), recipient.getUserId());
        MessageMediaService.MediaUploadResult uploadResult = messageMediaService.uploadImage(conversationId, file);

        Message message = chatService.sendImageMessage(
                currentUser.getUserId(),
                recipient.getUserId(),
                currentUser.getUsername(),
                uploadResult.mediaKey(),
                uploadResult.mediaUrl(),
                uploadResult.contentType()
        );

        messagingTemplate.convertAndSend("/topic/conversation." + conversationId, message);
        return ResponseEntity.ok(message);
    }

    @GetMapping("/api/messages/media")
    public ResponseEntity<ByteArrayResource> messageMedia(@RequestParam String key) {
        try {
            byte[] mediaBytes = messageMediaService.downloadMedia(key);
            String contentType = messageMediaService.getContentType(key);

            MediaType mediaType = contentType != null
                    ? MediaType.parseMediaType(contentType)
                    : MediaType.APPLICATION_OCTET_STREAM;

            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                    .contentType(mediaType)
                    .body(new ByteArrayResource(mediaBytes));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    private String buildConversationId(String userId1, String userId2) {
        return userId1.compareTo(userId2) < 0
                ? userId1 + "#" + userId2
                : userId2 + "#" + userId1;
    }
}
