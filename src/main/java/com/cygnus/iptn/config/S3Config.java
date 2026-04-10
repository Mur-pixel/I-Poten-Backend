package com.cygnus.iptn.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
public class S3Config {

    @Value("${cloud.aws.credentials.profile.access-key}")
    private String accessKey;

    @Value("${cloud.aws.credentials.profile.secret-key}")
    private String secretKey;

    @Value("${cloud.aws.region.static}")
    private String region;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKey, secretKey)
                        )
                )
                .serviceConfiguration(
                        S3Configuration.builder()
                                .checksumValidationEnabled(false)
                                .build()
                )
                .endpointOverride(URI.create("https://s3." + region + ".amazonaws.com"))
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKey, secretKey)
                        )
                )
                .serviceConfiguration(
                        S3Configuration.builder()
                                .checksumValidationEnabled(false)
                                .build()
                )
                .endpointOverride(URI.create("https://s3." + region + ".amazonaws.com"))
                .build();
    }

    // --- 신고 파일용 S3 설정 ---
    @Value("${cloud.aws.credentials.report.access-key}")
    private String reportAccessKey;

    @Value("${cloud.aws.credentials.report.secret-key}")
    private String reportSecretKey;

    @Bean
    @Qualifier("reportS3Client")
    public S3Client reportS3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(reportAccessKey, reportSecretKey)
                        )
                )
                .build();
    }

    @Bean
    @Qualifier("reportS3Presigner")
    public S3Presigner reportS3Presigner() {
        return S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(reportAccessKey, reportSecretKey)
                        )
                )
                .build();
    }
}