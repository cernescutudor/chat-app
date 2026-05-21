package com.chatapp.chatapp.controller;

import com.chatapp.chatapp.model.FriendRequest;
import com.chatapp.chatapp.model.User;
import com.chatapp.chatapp.repository.UserRepository;
import com.chatapp.chatapp.service.FriendRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/friend-requests")
@RequiredArgsConstructor
public class FriendRequestController {
    private final FriendRequestService friendRequestService;
    private final UserRepository userRepository;
    private final WebSocketController webSocketController;

    @PostMapping("/send/{recipientId}")
    public ResponseEntity<?> sendFriendRequest(
            @PathVariable String recipientId,
            @RequestBody(required = false) Map<String, String> payload,
            Authentication authentication) {
        
        String email = authentication.getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        String message = (payload != null && payload.containsKey("message")) ? payload.get("message") : null;

        try {
            FriendRequest request = friendRequestService.sendFriendRequest(
                    currentUser.getUserId(), recipientId, message);
            
            webSocketController.notifyFriendRequest(request, recipientId);
            
            return ResponseEntity.ok(request);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/pending")
    public ResponseEntity<List<FriendRequest>> getPendingRequests(Authentication authentication) {
        String email = authentication.getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<FriendRequest> pendingRequests = friendRequestService.getPendingRequests(
                currentUser.getUserId());
        return ResponseEntity.ok(pendingRequests);
    }

        @GetMapping("/sent-pending")
        public ResponseEntity<List<FriendRequest>> getSentPendingRequests(Authentication authentication) {
        String email = authentication.getName();
        User currentUser = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));

        List<FriendRequest> sentPendingRequests = friendRequestService.getOutgoingPendingRequests(
            currentUser.getUserId());
        return ResponseEntity.ok(sentPendingRequests);
        }

    @PostMapping("/{requestId}/accept")
    public ResponseEntity<?> acceptFriendRequest(
            @PathVariable String requestId,
            Authentication authentication) {
        
        String email = authentication.getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            FriendRequest request = friendRequestService.acceptFriendRequest(requestId, currentUser.getUserId());

            User sender = userRepository.findById(request.getSenderId())
                    .orElseThrow(() -> new RuntimeException("Sender not found"));
            webSocketController.notifyFriendRequest(request, sender.getUserId());

            return ResponseEntity.ok(request);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{requestId}/reject")
    public ResponseEntity<?> rejectFriendRequest(
            @PathVariable String requestId,
            Authentication authentication) {
        
        String email = authentication.getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            FriendRequest request = friendRequestService.rejectFriendRequest(requestId, currentUser.getUserId());

            User sender = userRepository.findById(request.getSenderId())
                    .orElseThrow(() -> new RuntimeException("Sender not found"));
            webSocketController.notifyFriendRequest(request, sender.getUserId());

            return ResponseEntity.ok(request);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/non-friends")
    public ResponseEntity<List<User>> getNonFriendsForRequest(Authentication authentication) {
        String email = authentication.getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<User> nonFriends = friendRequestService.getNonFriendsForRequest(
                currentUser.getUserId());
        return ResponseEntity.ok(nonFriends);
    }
}
