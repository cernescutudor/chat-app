package com.chatapp.chatapp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageMediaService {

    private static final String CHAT_MESSAGE_PREFIX = "chat-messages/";

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public MediaUploadResult uploadImage(String conversationId, MultipartFile file) {
        validateImage(file);

        String mediaKey = buildMediaKey(conversationId, file.getOriginalFilename());
        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";

        try (InputStream inputStream = file.getInputStream()) {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(mediaKey)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromInputStream(inputStream, file.getSize())
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload image message.", e);
        }

        return new MediaUploadResult(mediaKey, buildMediaUrl(mediaKey), contentType);
    }

    public byte[] downloadMedia(String mediaKey) {
        try {
            return s3Client.getObjectAsBytes(
                    GetObjectRequest.builder()
                            .bucket(bucketName)
                            .key(mediaKey)
                            .build()
            ).asByteArray();
        } catch (NoSuchKeyException e) {
            throw new IllegalArgumentException("Media not found.");
        }
    }

    public String getContentType(String mediaKey) {
        return s3Client.headObject(HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(mediaKey)
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

    private String buildMediaKey(String conversationId, String originalFilename) {
        String safeName = sanitizeFilename(originalFilename);
        return CHAT_MESSAGE_PREFIX + conversationId + "/" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID() + "-" + safeName;
    }

    private String buildMediaUrl(String mediaKey) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/messages/media")
                .queryParam("key", mediaKey)
                .toUriString();
    }

    private String sanitizeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "image";
        }

        return originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    public record MediaUploadResult(String mediaKey, String mediaUrl, String contentType) {
    }
}
