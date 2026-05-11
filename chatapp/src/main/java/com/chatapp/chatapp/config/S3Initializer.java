package com.chatapp.chatapp.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;

@Component
@RequiredArgsConstructor
public class S3Initializer implements ApplicationRunner {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Override
    public void run(ApplicationArguments args) {
        if (bucketName == null || bucketName.isBlank()) {
            return;
        }

        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
        } catch (Exception e) {
            System.out.println("Skipping S3 bucket initialization: " + e.getMessage());
            try {
                s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
            } catch (Exception ignored) {
                System.out.println("S3 bucket initialization could not complete.");
            }
        }
    }
}