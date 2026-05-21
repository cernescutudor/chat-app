package com.chatapp.chatapp.service;

import com.chatapp.chatapp.model.FriendRequest;
import com.chatapp.chatapp.model.Message;
import com.chatapp.chatapp.model.User;
import com.chatapp.chatapp.repository.FriendRequestRepository;
import com.chatapp.chatapp.repository.MessageRepository;
import com.chatapp.chatapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FriendRequestService {

    private final FriendRequestRepository friendRequestRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;

    public FriendRequest sendFriendRequest(String senderId, String recipientId, String message) {
        // Validate that both users exist
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Sender user not found"));
        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new RuntimeException("Recipient user not found"));

        // Check if already friends
        if (friendRequestRepository.areFriends(senderId, recipientId)) {
            throw new RuntimeException("You are already friends with this user");
        }

        // Check if a pending request already exists
        Optional<FriendRequest> existingRequest = friendRequestRepository.findPendingRequestBetween(senderId, recipientId);
        if (existingRequest.isPresent()) {
            throw new RuntimeException("A pending friend request already exists");
        }

        // Check for reverse pending request
        Optional<FriendRequest> reverseRequest = friendRequestRepository.findPendingRequestBetween(recipientId, senderId);
        if (reverseRequest.isPresent()) {
            // If recipient already sent a request, auto-accept it (bidirectional)
            return acceptFriendRequest(reverseRequest.get().getRequestId(), senderId);
        }

        // Create new friend request
        FriendRequest friendRequest = FriendRequest.builder()
                .requestId(UUID.randomUUID().toString())
                .senderId(senderId)
                .senderUsername(sender.getUsername())
                .recipientId(recipientId)
                .recipientUsername(recipient.getUsername())
                .status("PENDING")
                .message(message)
                .timestamp(Instant.now().toString())
                .build();

        friendRequestRepository.save(friendRequest);
        return friendRequest;
    }

    public FriendRequest acceptFriendRequest(String requestId, String currentUserId) {
        FriendRequest request = friendRequestRepository.findById(requestId)
                                .orElseThrow(() -> new RuntimeException("Friend request not found"));

        if (!currentUserId.equals(request.getRecipientId())) {
            throw new RuntimeException("You can only accept requests sent to you");
        }

        if (!"PENDING".equals(request.getStatus())) {
            throw new RuntimeException("This request is not pending");
        }

        request.setStatus("ACCEPTED");
        friendRequestRepository.save(request);
        
        // Save the request message as the first chat message if it exists
        if (request.getMessage() != null && !request.getMessage().trim().isEmpty()) {
            String conversationId = buildConversationId(request.getSenderId(), request.getRecipientId());
            Message chatMessage = Message.builder()
                    .conversationId(conversationId)
                    .timestamp(request.getTimestamp() != null ? request.getTimestamp() : Instant.now().toString())
                    .senderId(request.getSenderId())
                    .senderUsername(request.getSenderUsername())
                    .recipientId(request.getRecipientId())
                    .content(request.getMessage())
                    .messageType("TEXT")
                    .build();
            messageRepository.save(chatMessage);
        }
        
        return request;
    }

    private String buildConversationId(String userId1, String userId2) {
        return userId1.compareTo(userId2) < 0
                ? userId1 + "_" + userId2
                : userId2 + "_" + userId1;
    }

    public FriendRequest rejectFriendRequest(String requestId, String currentUserId) {
        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Friend request not found"));

        if (!currentUserId.equals(request.getRecipientId())) {
            throw new RuntimeException("You can only reject requests sent to you");
        }

        if (!"PENDING".equals(request.getStatus())) {
            throw new RuntimeException("This request is not pending");
        }

        request.setStatus("REJECTED");
        friendRequestRepository.save(request);
        return request;
    }

    public boolean areFriends(String userId1, String userId2) {
        return friendRequestRepository.areFriends(userId1, userId2);
    }

    public List<FriendRequest> getPendingRequests(String userId) {
        return friendRequestRepository.findByRecipientId(userId)
                .stream()
                .filter(r -> "PENDING".equals(r.getStatus()))
                .collect(Collectors.toList());
    }

    public List<FriendRequest> getOutgoingPendingRequests(String userId) {
        return friendRequestRepository.findBySenderId(userId)
                .stream()
                .filter(r -> "PENDING".equals(r.getStatus()))
                .collect(Collectors.toList());
    }

    public List<String> getFriendIds(String userId) {
        return friendRequestRepository.findAcceptedFriends(userId)
                .stream()
                .map(r -> userId.equals(r.getSenderId()) ? r.getRecipientId() : r.getSenderId())
                .distinct()
                .collect(Collectors.toList());
    }

    public List<User> getFriendsWithDetails(String userId) {
        return getFriendIds(userId)
                .stream()
                .flatMap(friendId -> userRepository.findById(friendId).stream())
                .collect(Collectors.toList());
    }

    public List<User> getNonFriendsForRequest(String userId) {
        List<String> friendIds = getFriendIds(userId);
        
        return userRepository.findAll()
                .stream()
                .filter(u -> !friendIds.contains(u.getUserId()))
                .collect(Collectors.toList());
    }
}
