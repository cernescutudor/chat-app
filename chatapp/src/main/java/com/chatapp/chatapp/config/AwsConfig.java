package com.chatapp.chatapp.config;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials; // only for local dev with static creds, not used in deployment
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider; // only for local dev with static creds, not used in deployment

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class AwsConfig {

    @Value("${aws.region}")
    private String region;

    @Value("${aws.dynamodb.endpoint:}")
    private String dynamoDbEndpoint;

    @Value("${aws.s3.endpoint:}")
    private String s3Endpoint;

    // vvvv comment out when deploying
    @Value("${aws.access-key-id}")
    private String accessKeyId;

    @Value("${aws.secret-access-key}")
    private String secretAccessKey;
    // ^^^^ comment out when deploying


    private StaticCredentialsProvider credentialsProvider() {
        return StaticCredentialsProvider.create(
            AwsBasicCredentials.create(accessKeyId, secretAccessKey)
        );
    }


    // IAM Role (EC2 instance profile) uncomment for deployment vvvv

    // private DefaultCredentialsProvider credentialsProvider() {
    //     return DefaultCredentialsProvider.create();
    // }

    @Bean
    public DynamoDbClient dynamoDbClient() {

        var builder = DynamoDbClient.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider());

        // optional local dev (DynamoDB Local)
        if (dynamoDbEndpoint != null && !dynamoDbEndpoint.isBlank()) {
            builder.endpointOverride(URI.create(dynamoDbEndpoint));
        }

        return builder.build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }

    @Bean
    public S3Client s3Client() {

        var builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider());

        // optional local dev (MinIO / localstack)
        if (s3Endpoint != null && !s3Endpoint.isBlank()) {
            builder.endpointOverride(URI.create(s3Endpoint))
                   .forcePathStyle(true);
        }

        return builder.build();
    }
}