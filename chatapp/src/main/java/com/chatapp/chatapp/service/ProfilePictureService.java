package com.chatapp.chatapp.service;

import com.chatapp.chatapp.model.User;
import com.chatapp.chatapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class ProfilePictureService {

    private static final String PROFILE_PICTURE_PREFIX = "profile-pictures/";

    private final S3Client s3Client;
    private final UserRepository userRepository;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public void uploadProfilePicture(User user, MultipartFile file, String profilePictureUrl) {
        validateImage(file);

        String objectKey = objectKeyFor(user.getUserId());
        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";

        try (InputStream inputStream = file.getInputStream()) {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(objectKey)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromInputStream(inputStream, file.getSize())
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload profile picture.", e);
        }

        user.setProfilePictureUrl(profilePictureUrl);
        userRepository.save(user);
    }

    public byte[] downloadProfilePicture(String userId) {
        try {
            return s3Client.getObjectAsBytes(
                    GetObjectRequest.builder()
                            .bucket(bucketName)
                            .key(objectKeyFor(userId))
                            .build()
            ).asByteArray();
        } catch (NoSuchKeyException e) {
            throw new IllegalArgumentException("Profile picture not found.");
        }
    }

    public String getContentType(String userId) {
        return s3Client.headObject(HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKeyFor(userId))
                .build()).contentType();
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please choose an image file.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are allowed.");
        }
    }

    private String objectKeyFor(String userId) {
        return PROFILE_PICTURE_PREFIX + userId;
    }
}