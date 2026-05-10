package com.chatapp.chatapp.controller;

import com.chatapp.chatapp.model.User;
import com.chatapp.chatapp.repository.UserRepository;
import com.chatapp.chatapp.service.ProfilePictureService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Controller
@RequestMapping
@RequiredArgsConstructor
public class ProfileController {

    private final UserRepository userRepository;
    private final ProfilePictureService profilePictureService;

    @GetMapping("/profile")
    public String profilePage(Authentication authentication, Model model) {
        User currentUser = currentUser(authentication);
        model.addAttribute("currentUser", currentUser);
        return "profile";
    }

    @PostMapping("/profile/picture")
    public String uploadProfilePicture(Authentication authentication,
                                       @RequestParam("file") MultipartFile file,
                                       Model model) {
        User currentUser = currentUser(authentication);
        String profilePictureUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/users/{userId}/profile-picture")
            // to be able to see updated profile pic immediately after upload
            .queryParam("v", Instant.now().toEpochMilli())
                .buildAndExpand(currentUser.getUserId())
                .toUriString();

        try {
            profilePictureService.uploadProfilePicture(currentUser, file, profilePictureUrl);
            return "redirect:/profile?uploaded";
        } catch (IllegalArgumentException e) {
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("error", e.getMessage());
            return "profile";
        }
    }

    @GetMapping("/api/users/{userId}/profile-picture")
    public ResponseEntity<?> profilePicture(@PathVariable String userId) {
        try {
            byte[] pictureBytes = profilePictureService.downloadProfilePicture(userId);
            String contentType = profilePictureService.getContentType(userId);

            MediaType mediaType = contentType != null ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM;

            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                    .contentType(mediaType)
                    .body(new ByteArrayResource(pictureBytes));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    private User currentUser(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}